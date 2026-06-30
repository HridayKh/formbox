## 1. Public Submission Endpoint (The Core Engine)

This is your high-throughput, mission-critical endpoint (e.g.,
`POST /f/{form_id}`). It must be lightning-fast and bulletproof.

* **[ ] Submission Ingestion & Validation**
* Accepts `application/x-www-form-urlencoded` and `application/json`.
* Determines redirect behavior: Looks for a custom `_next` field or falls back to a default, clean "Thank You" page.


* **[ ] Rate Limiting & Abuse Prevention**
* **Free Tier:** Standard global rate limiting (e.g., max 60 requests/minute per IP) to prevent DDoS.
* **Paid Tier:** Generous or dedicated rate-limiting bucket.


* **[ ] Tier-Based Submission Cap**
* **Free Tier:** Hard cap at **100 submissions/month
  **. System rejects new submissions with an error or queues them and sends an "Upgrade Required" email to the owner.
* **Paid Tier:** Hard cap at **2,000 submissions/month**.


* **[ ] Spam Filtering & Protection**
* **Free Tier:** Basic honeypot field detection (hidden inputs that bots fill out).
* **Paid Tier:** Cloudflare Turnstile integration (verify `cf-turnstile-response` token before processing).


* **[ ] Async Processing Queue**
* Saves the raw submission immediately to the database.
* Pushes a job to a background queue (e.g., Redis/BullMQ, Celery) to handle notifications asynchronously so the client request isn't blocked.

---

## 2. Forms & Version Management (Dashboard Engine)

Where users manage their endpoints and look at data.

* **[ ] Form CRUD Operations**
* Create, Read, Update, and Delete forms.
* **Soft Delete:** Mark as `deleted_at` so users can recover accidentally deleted forms.
* **Hard Delete (Cascade):** Clean up all associated submissions, versions, and webhooks when permanently deleting.


* **[ ] Form Versioning**
* Snapshots form schemas or configurations over time.
* Allows updating the "active" version on the main form so old endpoints don't break when fields change.

---

## 3. Data Export & Portability

Giving users access to their data when they need it.

* **[ ] Basic Export**
* **Free Tier:** Standard CSV export of the current form submissions.


* **[ ] Advanced Export**
* **Paid Tier:** Export to JSON, Excel (.xlsx), and time-range filtered CSVs.
* **Paid Tier:
  ** S3 Auto-Backup (optional bonus feature: automatically dump monthly submissions to a user's own storage).

---

## 4. Notifications & Integrations

The queue workers that fire off after a successful submission.

* **[ ] Instant Email Notifications**
* **Free Tier:** Standard, unbranded email notification to the form owner for every submission.
* **Paid Tier:** Custom-branded email notifications, or routing to multiple email addresses.


* **[ ] Daily Email Digest**
* **Free Tier:** *Not available.*
* **Paid Tier:** A cron-job powered summary email sent every 24 hours containing a snapshot of the day's submissions.


* **[ ] Notification Channels (Webhooks)**
* **Free Tier:** *Not available.*
* **Paid Tier:** * [ ] **Discord Webhook:** Format submission data into clean Discord embeds.
* [ ] **Slack Webhook:** Format data into Slack Blocks.
* [ ] **Custom Webhook:** Raw JSON POST to any user-defined URL.

---

## 5. Billing & Account Management

The infrastructure that separates the free tier from the paid tier.

* **[-] Stripe Integration**
* Handle the $2.99/month subscription lifecycle (Trial → Active → Past Due → Canceled).


* **[-] Usage Tracking (Metered Billing or Caps)**
* A cron job or event listener that resets submission counters on the 1st of the month or on the user's billing cycle date.


* **[ ] Feature Flags / RBAC**
* Middleware that checks
  `user.plan === 'paid'` before allowing access to Advanced Export, Turnstile settings, Daily Digest toggles, and Notification channels.

---

## Feature Matrix Summary

| Feature                   | Free Tier ($0)         | Paid Tier ($2.99/mo)            |
|---------------------------|------------------------|---------------------------------|
| **Monthly Submissions**   | 100                    | 2,000                           |
| **Spam Protection**       | Honeypot               | Honeypot + Cloudflare Turnstile |
| **Instant Email Alerts**  | Yes (Single recipient) | Yes (Multiple recipients)       |
| **Daily Email Digest**    | No                     | Yes                             |
| **Export Options**        | Basic CSV              | Advanced CSV, JSON, Excel       |
| **Notification Channels** | None                   | Discord, Slack, Custom Webhooks |
| **Form Management**       | CRUD & Versioning      | CRUD & Versioning               |

How are you planning to build the backend? If you're deciding between a traditional relational database (like PostgreSQL) or a NoSQL setup to handle dynamic form fields, we can map out the DB schema next.