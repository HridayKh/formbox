# GDPR — Code & Infra Changes Needed

> Legal docs (Privacy Policy, ToS, DPA, Impressum, Cookie Policy) are excluded — known todo for after code is finalized.

---

## Bugs to Fix

### 1. 🐛 Orphaned S3 Files on Form Deletion
[FormSubmissionCleanupListener.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/submission/internal/FormSubmissionCleanupListener.java) batch-deletes submissions from DB but doesn't delete their S3 file attachments. Individual `deleteSubmission()` does it correctly — the bulk path doesn't.

### 2. 🐛 CSV Exports Not Covered by Retention Scheduler
[SubmissionCleanupScheduler.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/submission/internal/SubmissionCleanupScheduler.java) purges submissions after 7/30/90 days but CSV exports at `exports/{formId}/` on S3 persist indefinitely. These should be auto-deleted (e.g., after 7 days, or when their parent submissions are purged).

### 3. 🐛 IP Addresses Not Hashed (Existing Salt Unused)
[IpRateLimitFilterService.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/shared/internal/IpRateLimitFilterService.java) logs and caches raw IPs. `.env` defines `IP_RATE_LIMIT_SECRET_SALT` but it's never used. Hash IPs with this salt before logging and using as cache keys.

---

## Features / Changes

### 4. Self-Host Google Fonts
Download Inter (and JetBrains Mono if used) `.woff2` files, serve from `/assets/fonts/`. Update:
- [styles.css](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/resources/static/assets/styles.css) — remove `@import url('https://fonts.googleapis.com/...')`
- All 46 HTML pages in `pages/` — remove `<link href="https://fonts.googleapis.com/...">`
- [index.jte](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/jte/index.jte) — if it has a Google Fonts link

### 5. Self-Host HTMX
Download `htmx.min.js` to `/assets/`, update [auth/layout.jte](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/jte/auth/layout.jte) and [auth/callback.jte](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/jte/auth/callback.jte) to load from local path instead of `cdn.jsdelivr.net`.

### 6. Pre-Signed S3 URLs for File Uploads
Currently file uploads are publicly accessible at `https://web-s3.hridaykh.in/uploads/{UUID}/{fileName}`. Generate temporary pre-signed URLs (e.g., 15-min expiry) on demand when an authenticated user views a submission, instead of storing/exposing the raw public URL.

### 7. Add Consent Checkbox on Signup
Add a checkbox to [register.jte](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/jte/auth/register.jte): *"I agree to the [Privacy Policy] and [Terms of Service]"*. Validate server-side in [AuthController.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/auth/internal/AuthController.java).

### 8. Migrate Supabase to EU Region
Currently on `aws-1-ap-northeast-1` (Tokyo). Switch to an EU region. *(Noted as already planned.)*

---

## Not Needed (Per Your Feedback)

| Item | Why Not Needed |
|------|---------------|
| Self-service account deletion | Contact-support flow is legally sufficient (respond within 30 days) |
| Privacy notice on submission thank-you page | Form owner's responsibility as data controller |
| Discord/webhook data sharing warnings | Form owner consciously configures these as data controller |
| Sentry user ID stripping | Low risk — just list Sentry as sub-processor in DPA |
| Auto-purge inactive accounts | Not required — just state retention policy in Privacy Policy |
