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
4. annual and lifetime plans

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

* **[Medium Priority]**: Offload submissions older than the last 100 per form to Cloudflare R2.
* **[Medium Priority]**: Convert POST requests for redirects (like managing subscriptions) into GET requests.
* **[Medium Priority]**: Implement OpenTelemetry (Otel) across all methods and capture deeper metadata (IP, User ID, Session data).
* **[Low Priority]**: Audit and re-decide log levels; implement structured logging usage everywhere.
* **[Low Priority]**: Retry setting up the Sentry agent.

### Dashboard & Frontend UX

* **[High Priority]**: Revamp the **Manage Form** page: Separate settings into a tab and display submission stats directly on the main form page.
* **[High Priority]**: Show the user's current submission balance.
* **[Medium Priority]**: Revamp the overall form display layout and implement pagination.

### Core Form Features & Integrations

* **[High Priority]**: Settings page with all the form settings
* **[Medium Priority]**: Add asynchronous Discord notifications via webhook URL (with an errors table to report dashboard failures).
* **[Medium Priority]**: Build CSV export functionality for submissions.
* **[Low Priority]**: Magic `mailto:` links.
* **[Low Priority]**: Email Digests
* **[Low Priority]**: Webhook routing

### Billing, Monetization & Launch Strategy

* **[High Priority]**: Fix billing.
* **[Medium Priority]**: Add "Annual Plans (Coming Soon)" to the index page.
* **[Near Launch]**: Build a dashboard page to manage/buy plans if the Polar-hosted checkout doesn't cover it.
* **[Near Launch]**: Add a "Contact Us" custom enterprise plan to the frontend alongside a scheduling call link.

### Admin & Documentation (Pre-Launch)

* **[Medium Priority]**: Auth improvements: Password reset, forgot password, and OAuth for quick signup.
* **[Near Launch]**: Build SQL queries as an admin panel
* **[Near Launch]**: Write the Knowledge Base / Help documentation.
* **[Near Launch]**: Draft and publish Legal Docs (Privacy Policy, ToS).

### Roadmap

1. Change `isSpam` boolean to a `spamReason` enum (e.g., `notSpam`, `turnstileFailed`)
2. Build the internal Admin Panel web based
3. Build an `/onboarding` page post-signup that auto-generates a mock form with fields to get users started instantly
4. Build 3rd-party library wrappers (React components, etc.)
5. Slack webhooks
6. Telegram Webhooks
7. Form Error Handling: Let users decide error behavior per form (e.g., return `202 Accepted` even if validation fails for sub-20ms responses, or throw proper errors).
