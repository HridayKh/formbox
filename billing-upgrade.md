# Billing Upgrade Guide

This document outlines what to do when upgrading from the bare minimum billing implementation to a fully-featured system. The current MVP handles subscription lifecycle via `subscription.updated` with entitlements derived from Polar product metadata.

---

## Current State (MVP)

### What's Implemented
- **Single webhook**: `subscription.updated` — handles all subscription state transitions
- **Entitlements source**: Polar product metadata (feature flags, numeric limits, tier identity)
- **Subscription state**: Stored on the `Entitlements` JSONB column (`subscriptionStatus`, `cancelAtPeriodEnd`, etc.)
- **Meters**: Submissions meter exists and is wired up; storage meter is placeholder
- **Usage tracking**: Submissions counted via Polar event ingestion + `PolarMeterService`
- **Refresh timing**: Auto-calculated from subscription billing period

### What's NOT Implemented
- Checkout flow tracking (no `checkout.*` events)
- Order/payment tracking (no `order.*` events)
- Welcome/onboarding flows (no `subscription.created` handling)
- Refund handling
- Event audit log / billing event history
- UI-specific lifecycle banners (past_due payment method warning, etc.)

---

## Upgrade Path

### Phase 1: Add Checkout & Order Events

**Goal**: Track the full purchase funnel and payment status.

**Events to subscribe to** (in Polar webhook settings):
- `checkout.created` — track when a checkout session starts
- `checkout.updated` — track `status` field: `confirmed`, `succeeded`, `failed`
- `checkout.expired` — abandoned checkout
- `order.created` — new invoice/charge generated
- `order.paid` — payment confirmed
- `order.refunded` — refund processed

**Implementation**:
1. Add event routing in `PolarWebhookService.processHook()` via a `switch` on event type
2. Create new models: `CheckoutEvent.java`, `OrderEvent.java`
3. Store checkout/order events in a `billing_events` audit log table for future analytics
4. Use `order.paid` to confirm successful renewals (currently assumed from `subscription.cycled`)

### Phase 2: Lifecycle UI Banners

**Goal**: Show contextual banners to users based on subscription state.

| Subscription State | Banner |
|---|---|
| `active` + `cancel_at_period_end: true` | "Your plan will be canceled on {currentPeriodEnd}. [Undo]" |
| `past_due` | "Payment failed. [Update payment method] to keep your subscription." |
| `paused` | "Your subscription is paused. [Resume] to continue." |
| `trialing` | "Your free trial ends on {currentPeriodEnd}. [Add payment method]" |

**Implementation**:
- The `Entitlements` record already carries `subscriptionStatus`, `cancelAtPeriodEnd`, and `currentPeriodEnd` — just wire these into the dashboard templates
- Use `subscription.uncanceled` (arrives via `subscription.updated` with `cancel_at_period_end: false`) to clear cancellation banners

### Phase 3: Subscription Event History Table

**Goal**: Log every subscription state change for analytics (churn tracking, upgrade/downgrade stats, MRR).

**Schema** (suggested):
```sql
CREATE TABLE billing_events (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id     UUID NOT NULL REFERENCES tenants(id),
  event_type    TEXT NOT NULL,           -- 'subscription.updated', 'order.paid', etc.
  subscription_id TEXT,
  product_id    TEXT,
  status        TEXT,                     -- subscription status at time of event
  amount        BIGINT,                   -- cents
  currency      TEXT,
  billing_reason TEXT,                    -- 'subscription_create', 'subscription_cycle', etc.
  raw_payload   JSONB,                    -- full webhook payload for debugging
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_billing_events_tenant ON billing_events(tenant_id, created_at);
```

**Ordering rule** (from billing-notes.md): Only update state if the incoming event's `created_at` is newer than the last stored event's. If timestamps tie, fall back to logical ordering.

### Phase 4: Storage Meter

**Goal**: Track file upload storage usage as a Polar meter.

**Steps**:
1. Create the `storage_bytes` meter in Polar dashboard
2. Configure `polar-ids.storage-meter-id` in `application.properties`
3. Wire up `PolarMeterService` with a `reportStorageUsageEvent()` method
4. Emit storage events on file upload/deletion
5. Add balance checking in the upload controller (similar to submission balance checks)

### Phase 5: Separate Entitlements from Subscription State

**Goal**: If the `Entitlements` record gets too bloated, split into two concerns.

**Option A** — Keep consolidated (recommended for now):
- The `Entitlements` JSONB already carries both subscription state and feature flags
- Simple, single source of truth, cached together

**Option B** — Split if needed later:
- `Entitlements` → pure feature flags and limits
- `SubscriptionState` → subscription lifecycle info (status, period, cancel flags)
- Both stored as separate JSONB columns or in a `subscriptions` table

### Phase 6: Benefit Grants (if needed)

**Goal**: If you add non-subscription benefits (manual grants, one-time purchases, etc.).

**Events**: `benefit_grant.created`, `benefit_grant.updated`, `benefit_grant.revoked`

Currently not needed because all access is subscription-driven and entitlements come from product metadata. If you later need per-customer overrides (e.g., manually granting a user extra forms), benefit grants would be the way.

---

## Polar Webhook Configuration Checklist

When upgrading, add events to your Polar webhook configuration incrementally:

| Phase | Events |
|---|---|
| **MVP (current)** | `subscription.updated` |
| **Phase 1** | + `checkout.created`, `checkout.updated`, `checkout.expired`, `order.created`, `order.paid`, `order.refunded` |
| **Phase 6** | + `benefit_grant.created`, `benefit_grant.updated`, `benefit_grant.revoked` |
