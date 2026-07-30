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


## Frontend Pages

### 1. Navbar

- Breadcrumbs
- Submissions left
- Mange Subscription/Upgrade
- Logged-in Email
- Logout

### 2. Account Home

- List of folders + any unfoldered forms, each with a delivery-health badge (green/yellow/red)
- No global search in v0 — just navigation into folders/forms
- "Create folder" / "Create form"

**3. Folder View**
- List of forms in the folder
- Folder-level client credential issuance (scoped to whole folder, auto-covers new forms added later)
- No folder-wide submission rollup — search/export stays per-form, so this view is really just a navigation + access-management layer, not a data view

**4. Form View** (tabbed)
- **Submissions**: searchable table, CSV export, click-through to submission detail
- **Delivery/Health**: per-recipient delivery status (owner notification + each CC/BCC tracked independently) — this is the differentiator, keep it visually prominent, not buried
- **Settings**: endpoint/snippet, allowed origins, recipients, redirect config, spam protection toggles, retention window
- **Access**: client credentials issued specifically to this form (plus a read-only note showing folder-level creds that also apply)

**5. Submission Detail** — own view/modal, handles file attachments and per-email delivery status without cramming into a table row.

**6. Account Settings** — plan tier, retention window, billing stub.

**7. Client Dashboard**
- Since clients get full delivery status now, this view is closer to a scoped mirror of the owner's Form View rather than a stripped-down version: submissions tab + delivery/health tab, both read-only, scoped to their form(s)/folder
- No settings, no access management, no ability to change recipients/spam config — just visibility
- Worth deciding whether they see this as one screen per form or a folder-scoped list they click into — given per-form-only search, probably: client lands on a list of their form(s) → clicks in → same tabbed submissions/health view as owner, just read-only
