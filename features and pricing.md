# Headless Form Backend - Race to MVP

## MVP

### Misc

- [x] LIMIT THE CC/BCC EMAILS PER TIER
- [x] LIMIT THE FILE SIZES UPLOADED
- [x] Fix Polar.sh Webhooks (reliable but inefficient)
- [x] Record storage usage in tenant meter

### Submission handling

- [x] Unique POST endpoint per form
- [x] See submissions in a dashboard
- [x] Email notification to site owner/agency on new submission
- [x] Email autoresponder
- [x] Retention window per plan tier

### Spam protection

- [x] Honeypot fields
- [x] Rate limiting per endpoint
- [x] Cloudflare Turnstile, BYOK
- [x] Verify the added To email for email notifications

### Reliability (core differentiator)

- [x] Delivery monitoring — track sent/delivered/bounced per autoresponse
- [x] Delivery monitoring — track sent/delivered/bounced per notification email
- [x] Email delivery status shown in dashboard — surfaced prominently; answers "is my form broken" before anyone asks

### Client/agency structure

- [x] Account → folder/bucket → form hierarchy
- [x] Client-facing view: read-only submission list, scoped to their form(s)/folder

### Developer ergonomics

- [x] Plain HTML form support
- [x] JSON reply support
- [x] Redirect-after-submit / custom success handling
- [x] Multiple recipient emails (CC/BCC) per form
- [x] File upload support
- [x] Passwordless login
- [x] CSV export (unsorted, baseline)

### Notifications

- [x] Discord Webhooks
- [x] Email Autoresponder
- [x] Independent delivery tracking per email/recipient (Autoresponder)
- [x] Email Notifications
- [x] Independent delivery tracking per email/recipient (Notifications)

## v1 — Simpler additive features

- Full non-password auth flow instead of mixed
- detailed billing state tracking
- Submission False Positives
	- mark spam as false positives
	- `spamReason != none` but `isSpam == false`, ie, false positive marked
	- store submissions dropped due to out of submissions too but with a flag
- Spam/Fail Reason (False Positives)
- Submission deletion capability
- Custom JSON reply
- JSON Form input
- Htmx-Support
- Allowed-origin/domain locking
- Multiple client credentials
- client credentials per folder
- custom client credentials
- permissions per client credential
- Custom meta fields — per-form, user-defined (notes, status, tags, etc.), with global templates a user can apply to new forms
- Time since submission — visible, sortable field per submission
- Overdue/stale submission indicator — visual flag when a submission sits untouched past a configurable threshold
- Custom routing — route submissions to different recipients based on form config/field values
- Simple stats dashboard — total submissions, submissions per week, spam ratio
- Altcha (privacy-first, no-cookie CAPTCHA)

## v2 — The rest of the core CRM-leaning set

- Multi-variable sort in dashboard
- sort/filter submissions by multiple fields at once (status, date, meta fields, etc.)
- Pre-sorted CSV export — export respects current dashboard sort/filter state
- Outbound integrations:
- Slack webhook
- Telegram webhook
- Custom webhooks (generic — covers pushing to external CRMs like HubSpot/Pipedrive without bespoke per-platform integrations)
- move old submissions to object storage
- store 3rd party webhooks in a log and show them as a read only in the dash and let them replay them

## v3 — Polish

- Status page / uptime visibility for the service itself
- Meta field templates: refine/expand (copy-on-apply starting point)
- Dashboard UX polish around overdue indicators (e.g. digest email of overdue submissions, not just visual flag)
- Expanded stats (response-time-to-first-contact, per-client breakdowns for agencies managing multiple folders)
- Additional webhook targets / pre-built formatting for common CRMs beyond generic webhook

## Pricing

> both starter and pro have annual variants with 2 months free

### Free

- 1 form
- 100 submissions/mo
- Retention: 2 weeks
- Core dashboard, spam protection (honeypot, timing, origin lock, rate limit, Altcha, Turnstile BYOK)
- Client credentials (per form/folder)
- Meta fields + templates
- No email notifications bundled
- No file uploads, or token 50MB allotment

### Starter — $14/mo

- Unlimited forms
- 2,500 submissions/mo
- Retention: 2 months
- Everything in Free
- Webhooks (Slack/Telegram/custom)
- 1GB bundled file storage (cycle-scoped — see Files below)
- Email: notification

### Pro — $28/mo

- Unlimited forms
- 25,000 submissions/mo
- Retention: 6 months
- Everything in Starter
- Multi-variable sort, pre-sorted export, stats dashboard, overdue indicators
- 10GB bundled file storage
- Email: notifications & Autoresponder
