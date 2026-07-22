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

### customer state webhook sent on every ingest event

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

* **[High Priority]**: Auth improvements: Password reset, forgot password, and OAuth for quick signup.
* **[Medium Priority]**: Make the new ui look good
* **[Medium Launch]**: Write the Knowledge Base / Help documentation.
* **[Low Priority]**: Draft and publish Legal Docs (Privacy Policy, ToS).
* **[Near Launch]**: Build SQL queries as an admin panel

### Roadmap

1. Retry setting up the Sentry agent.
2. should also handle benifit_grant.updated
3. Change `isSpam` boolean to a `spamReason` enum (e.g., `notSpam`, `turnstileFailed`)
4. Build the internal Admin Panel web based
5. Build an `/onboarding` page post-signup that auto-generates a mock form with fields to get users started instantly
6. Build 3rd-party library wrappers (React components, etc.)
7. Slack webhooks
8. Telegram Webhooks
9. Form Error Handling: Let users decide error behavior per form (e.g., return `202 Accepted` even if validation fails for sub-20ms responses, or throw proper errors).
10. deleted forms page
