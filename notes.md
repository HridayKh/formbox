# Notes

## Ui Inspiration

1. formlink.io
2. staticforms.dev
3. splitforms.com
4. usebasin.com
5. slapform.com

## Billing Upgrade Notes

1. Map Java Record to all feature flags and limits (current balance per user + max balance per user)
2. user.getEntitlements().whateverFeatureFlagOrLimitHere()
3. ts can be cached ez

## Experiments

1. how long does the pipeline take without turnstile and the 2 redis lookups (on prod, near redis)
2. experiment with cf workers and cf cache to get sub 20ms 
3. try to get the same with just workers 
4. try to mini.ise latency without cf 
5. move the turnstile check and schema validation after the response if the user says so (force the user to choose on form creation, no defaults)
6. Turnstile verification on my own /f/ or /verify/ domain????
7. `ALTCHA` as an alternative option for spam protection

## Todo

### Backend, Infrastructure & Operations

* **[High Priority]** Set up the Scheduler Service base for automated tasks; sync Polar with Redis more frequently.
* **[Medium Priority]** Offload submissions older than the last 100 per form to Cloudflare R2.
* **[Medium Priority]** Convert POST requests for redirects (like managing subscriptions) into GET requests.
* **[Medium Priority]** Implement OpenTelemetry (Otel) across all methods and capture deeper metadata (IP, User ID, Session data).
* **[Low Priority]** Audit and re-decide log levels; implement structured logging usage everywhere.
* **[Low Priority]** Retry setting up the Sentry agent.

### Core Form Features & Integrations

* **[High Priority]** Implement soft-deleting for forms.
* **[Medium Priority]** Add asynchronous Discord notifications via webhook URL (with an errors table to report dashboard failures).
* **[Medium Priority]** Form Error Handling: Let users decide error behavior per form (e.g., return `202 Accepted` even if validation fails for sub-20ms responses, or throw proper errors).
* **[Medium Priority]** Build CSV export functionality for submissions.
* **[Low Priority]** Magic `mailto:` links.
* **[Future]** Advanced Integrations & Routing: Slack, Telegram, Custom Webhooks, Custom Pro filters/rules, Routing rules for webhooks/Discord, and Email digests.
* **[Future]** Build 3rd-party library wrappers (React components, etc.).

### Dashboard & Frontend UX

* **[High Priority]** Revamp the **Manage Form** page: Separate settings into a tab and display submission stats directly on the main form page.
* **[High Priority]** Show the user's current submission balance.
* **[Medium Priority]** Revamp the overall form display layout and implement pagination.
* **[Near Launch]** Build an `/onboarding` page post-signup that auto-generates a mock form with fields to get users started instantly.

### Billing, Monetization & Launch Strategy

* **[High Priority]** Fix billing.
* **[Medium Priority]** Add "Annual Plans (Coming Soon)" to the index page.
* **[Near Launch]** Build a dashboard page to manage/buy plans if the Polar-hosted checkout doesn't cover it.
* **[Near Launch]** Handle automated tier cleanup: Auto-delete free tier configurations from Polar.sh if a user upgrades to a higher tier.
* **[Near Launch]** Fully implement the Annual plans.
* **[Near Launch]** Add a "Contact Us" custom enterprise plan to the frontend alongside a scheduling call link.
* **[Future]** Launch a Lifetime Plan ($89 for lifetime Pro or 3-5 year access).

### Admin & Documentation (Pre-Launch)

* **[Medium Priority]** Auth improvements: Password reset, forgot password, and OAuth for quick signup.
* **[Near Launch]** Build the internal Admin Panel.
* **[Near Launch]** Write the Knowledge Base / Help documentation.
* **[Near Launch]** Draft and publish Legal Docs (Privacy Policy, ToS).

### Roadmap

1. Change `isSpam` boolean to a `spamReason` enum (e.g., `notSpam`, `turnstileFailed`)
