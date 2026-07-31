# 1. Core Philosophy

- Don't overengineer billing — no need for Polar's meters as a source of truth for plan structure; hard-coding is OK.
- Store product info directly in the DB.
- Only 2 plan types: **monthly** and **yearly**. No addons.
- No separate entitlements table — since there are no addons, there's no need for one.
- Write simple tests for billing.

# 2. State Tracking Design

- Track user state via **webhooks**, not customer state polling/events.
- **Ordering rule:** only update state if the incoming event's `created_at` is newer than the last stored event's.
	- If `created_at` ties, fall back to logical ordering (i.e. which state transition makes sense to happen later).
- Log every purchase-state change in the DB for future stats (churn, upgrades/downgrades, etc.), but keep the states themselves simple.
- Reset specific states automatically after a set time period.
- Use a **simple 2-field system** for state tracking (state + timestamp, presumably).
- Use Polar's **product metadata** for feature flags and plan limits.
- Use Polar's **meters** for usage tracking.
- Use **hidden monthly subscriptions** to drive meter refresh timing for non-monthly plans, keeping usage cycles in sync with billing.
- Split state into two tracks:
	- **Entitlements** → driven by `benefit_grant.*` events.
	- **Other UI/lifecycle state** (e.g. banners, CTAs) → driven by the remaining event types.

# 3. State Definitions

## Checkout session states (`status` field)

| State       | Meaning                                                     |
|-------------|-------------------------------------------------------------|
| `open`      | Session created, customer hasn't paid yet ("init checkout") |
| `confirmed` | Customer clicked Pay; payment processing, outcome unknown   |
| `succeeded` | Payment succeeded; subscription/order created               |
| `failed`    | Payment failed for technical reasons, non-retryable (rare)  |
| `expired`   | Session timed out unused                                    |

## Subscription states (`status` field, post-checkout)

| State                                   | Meaning                                                                                                                       |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `incomplete`                            | Subscription created, first payment not yet confirmed (e.g. pending 3DS) — no access yet                                      |
| `incomplete_expired`                    | First payment never completed in time — subscription dies without activating                                                  |
| `trialing`                              | Inside free trial; customer has access, not yet charged                                                                       |
| `active`                                | Normal paid state; access continues while active or trialing                                                                  |
| `active` + `cancel_at_period_end: true` | "Canceled but still has benefits" — technically still active under the hood, access continues until period end                |
| `past_due`                              | A renewal charge failed; Polar handles retries. In  grace period                                                              |
| `unpaid`                                | All retries exhausted or the decline is unrecoverable — final state before revocation                                         |
| `canceled`                              | Fully ended (immediate revocation, grace period end, or period-end reached); benefits gone                                    |
| `paused`                                | billing stops and benefits are revoked until resumed (preceded by a `pause_at_period_end` flag, same pattern as cancellation) |

# 4. State → Webhook Event Mapping

## Checkout states (5)
| State     | Webhook                                   |
|-----------|-------------------------------------------|
| open      | `checkout.created`                        |
| confirmed | `checkout.updated` (`status="confirmed"`) |
| succeeded | `checkout.updated` (`status="succeeded"`) |
| failed    | `checkout.updated` (`status="failed"`)    |
| expired   | `checkout.expired` (dedicated event)      |

> `checkout.updated` is a generic catch-all fired on every status change — read `data.status` to tell confirmed/succeeded/failed apart.

## Subscription states (9)
| State                     | Webhook                                                                    |
|---------------------------|----------------------------------------------------------------------------|
| incomplete                | `subscription.created`                                                     |
| incomplete_expired        | ⚠️ *no dedicated event* (see gap below)                                    |
| trialing                  | `subscription.created` (`status="trialing"`)                               |
| active (new/recovered)    | `subscription.active`                                                      |
| active (renewal)          | `subscription.cycled`                                                      |
| active + pending cancel   | `subscription.canceled` (status still active, `cancel_at_period_end=true`) |
| past_due                  | `subscription.past_due`                                                    |
| unpaid → canceled (final) | `subscription.revoked`                                                     |
| paused (takes effect)     | `subscription.paused`                                                      |
| resumed → back to active  | `subscription.resumed`                                                     |

> `subscription.updated` fires alongside all of these as a companion catch-all — useful if you don't need to distinguish the reason. Per the docs it's a catch-all for `subscription.active`, `subscription.canceled`, `subscription.uncanceled`, `subscription.past_due`, and `subscription.revoked`.

# 5. Known Gaps / Open Questions

1. **`incomplete_expired` has no dedicated webhook**, and it isn't in the `subscription.updated` catch-all list either.
	- Workaround: poll `GET /subscriptions/{id}`, or infer it from a `checkout.expired` with no matching `subscription.active` following.

2. **No webhook for meter refresh.** Meters are read via the `customer_meters` API (get/list), not pushed via webhook.
	- Workaround: poll that endpoint, or recompute from your own event ingestion. Polar doesn't fire a `meter.updated` event.

3. **Entitlement event naming correction:** it's `benefit_grant.created`, not `benefit.granted`.
	- Full entitlement event set: `benefit_grant.created`, `benefit_grant.updated`, `benefit_grant.cycled`, `benefit_grant.revoked`.
	- This is the most reliable way to drive access control — it fires regardless of *why* access changed (new subscription, renewal, or one-time benefit).

# 6. Entitlements vs. UI/Lifecycle State

## Entitlements (does the user get access?) → `benefit_grant.*`
| Event                   | Effect                                                                     |
|-------------------------|----------------------------------------------------------------------------|
| `benefit_grant.created` | Turn a feature **on**                                                      |
| `benefit_grant.updated` | Grant details changed (e.g. plan swap changes what's included)             |
| `benefit_grant.cycled`  | Renewed for another period (relevant for consumable benefits like credits) |
| `benefit_grant.revoked` | Turn a feature **off**                                                     |

This is the source of truth for access because it fires for the reason that actually matters, regardless of the upstream cause (new sub, plan change, grace-period expiry, manual revoke). You never have to re-derive "does this person have access" from subscription-status math.

## UI/lifecycle state (what do we tell the user?) → everything else
| Event(s)                                                  | Use                                                                                                                                      |
|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `checkout.*`                                              | Checkout funnel tracking, abandoned-cart nudges                                                                                          |
| `subscription.created` / `.active`                        | Welcome flows, onboarding                                                                                                                |
| `subscription.canceled` (active + `cancel_at_period_end`) | "You're canceling, still have access until X" banner, win-back offer                                                                     |
| `subscription.past_due`                                   | "Update your payment method" banner — the important one, since access hasn't necessarily changed yet but a warning is due before it does |
| `subscription.paused` / `.resumed`                        | Pause-state messaging                                                                                                                    |
| `order.paid` / `.refunded`                                | Receipts, refund confirmations                                                                                                           |
| `subscription.uncanceled`                                 | "Welcome back" / clear the cancellation banner                                                                                           |