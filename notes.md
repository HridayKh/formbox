# Notes

## Ui Inspiration

1. formlink.io
2. staticforms.dev
3. splitforms.com
4. usebasin.com
5. slapform.com

## Experiments

1. how long does the pipeline take without turnstile and the 2 redis lookups (on prod, near redis)
2. experiment with cf workers and cf cache to get sub 20ms 
3. try to get the same with just workers 
4. try to mini.ise latency without cf 
5. move the turnstile check and schema validation after the response if the user says so (force the user to choose on form creation, no defaults)
6. Turnstile verification on my own /f/ or /verify/ domain????
7. `ALTCHA` as an alternative option for spam protection

## Todo

### Dashboard & Frontend UX

* **[Medium Priority]**: Revamp the overall form display layout and implement pagination.
* **[LOw Priority]**: Reimagine the **Manage Form** page
* **[Near Launch]**: Tally all the tier features and sync them all with the frontend! (maybe even dynamically generate them?)

### Core Form Features & Integrations

* **[Medium Priority]**: Add asynchronous Discord notifications via webhook URL (with an errors table to report dashboard failures).
* **[Low Priority]**: Magic `mailto:` links.
* **[Low Priority]**: Email Digests
* **[Low Priority]**: Webhook routing

### Backend, Infrastructure & Operations

* **[High Priority]**: Implement OpenTelemetry (Otel) across all methods and capture deeper metadata (IP, User ID, Session data).
* **[Medium Priority]**: Audit and re-decide log levels; implement structured logging usage everywhere.
* **[Low Priority]**: Offload submissions older than the last 100 per form to Cloudflare R2.
* **[Low Priority]**: Build CSV export functionality for submissions.

### Admin & Documentation (Pre-Launch)

* **[Medium Priority]**: Auth improvements: Password reset, forgot password, and OAuth for quick signup.
* **[Near Launch]**: Build SQL queries as an admin panel
* **[Near Launch]**: Write the Knowledge Base / Help documentation.
* **[Near Launch]**: Draft and publish Legal Docs (Privacy Policy, ToS).

### Roadmap

1. Retry setting up the Sentry agent.
2. should also handle benifit_grant.updated
2. Change `isSpam` boolean to a `spamReason` enum (e.g., `notSpam`, `turnstileFailed`)
3. Build the internal Admin Panel web based
4. Build an `/onboarding` page post-signup that auto-generates a mock form with fields to get users started instantly
5. Build 3rd-party library wrappers (React components, etc.)
6. Slack webhooks
7. Telegram Webhooks
8. Form Error Handling: Let users decide error behavior per form (e.g., return `202 Accepted` even if validation fails for sub-20ms responses, or throw proper errors).
9. deleted forms page
