# FormBox Core Architecture & Anti-Abuse Spec

## 1. Plan Feature Matrix

- Submission Volume: 2,500 processed submissions per billing cycle.
- Form Management: Unlimited active endpoints and independent project environments.
- File Upload Support: Up to 1 GB shared file object storage (Max 50 MB per file).
- Redirect Engine: Full support for to send users to a custom thank-you route.
- AJAX Fetch Mode: Native `application/json` string parsing supporting smooth `fetch()` / `axios` forms.
- Billing Engine: Handled natively by Polar.sh for global sales tax compliance and automated digital product receipt generation.

## 2. Dynamic Spam Prevention Pipeline

Because we route alerts directly through Amazon SES, protecting IP sending reputation requires an aggressive,
multi-layered defensive boundary.

```
[ Incoming Request ]
│
├──► 1. Rate Limiter (Token Bucket Redis Check) ──► Fail: 429 Too Many Requests
│
├──► 2. Honeypot Validation ──────────────────────► Fail: Silent 200 (Drop Payload)
│
├──► 3. Cloudflare Turnstile Server-Side Check ───► Fail: 400 Bad Request
│
└──► [ Pass: Queue Ingest & Send Transactional Email via SES ]
```

### Layer A: Hardware / IP Token Bucket Rate Limiting

To prevent sudden script execution loops, requests are rate-limited before reaching database memory stores.

- Threshold: Max 10 submissions per rolling 10-second window per originating IP address.
- Overages: Drops connections using an immediate `429 Too Many Requests` header status.

### Layer B: Passive Honeypot Validation

- Every endpoint output template injects an invisible, hidden field using inline CSS styling (`display: none;`).
- Action: If a field contains data, the payload is immediately dropped. The system triggers a fake `200 OK` response
  to the bot script to waste its network time while preserving database and SES quotas.

### Layer C: Cloudflare Turnstile Verification

A privacy-first, zero-friction verification layer running telemetry challenges without the use of invasive visual cookie
puzzles.

```js
// Backend validation block inside edge handler
async function verifyTurnstile(token, secretKey, remoteIp) {
    const formData = new FormData();
    formData.append('secret', secretKey);
    formData.append('response', token);
    formData.append('remoteip', remoteIp);
    const result = await fetch('https://challenges.cloudflare.com/turnstile/v0/siteverify', {
        body: formData,
        method: 'POST',
    });
    const outcome = await result.json();
    return outcome.success; // Returns true/false based on token validity
}
```

- Cost Factor: $0.00 base cost with unlimited verifications on standard free site keys.
- Compliance: Fully GDPR/CCPA compliant; runs without injecting tracking cookies.

## 3. Webhook Data Sync Integrations

Instead of locking integrations behind higher pricing tiers, native structural webhooks route payload transformations
directly on success events:

1. Discord: Bundles and strings the payload arrays directly into a single formatted Embed card.
2. Slack: Formats fields natively using Slack's layout Block Kit components.
3. Google Sheets: Direct appending of row indexes using verified client authorization keys.

Here is the completed section detailing the **Form Payload Database Object
** and how it integrates natively with Polar.sh webhooks to manage usage limits.

---

## 4. Form Payload & Subscriptions Schema (Database Spec)

To enforce the 5,000 submission limit on your serverless architecture without introducing heavy query bottlenecks, your database must cleanly track both the active submission structure and the real-time billing state synced via Polar.sh.

### Postgres / Supabase Database Schema

```sql
CREATE OR REPLACE FUNCTION generate_form_version_number()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Force a row lock on the parent form to serialize version creation for this form
    PERFORM 1 FROM forms WHERE id = NEW.form_id FOR UPDATE;

    -- Safe to calculate now without race conditions
    SELECT COALESCE(MAX(version_number), 0) + 1
    INTO NEW.version_number
    FROM form_versions
    WHERE form_id = NEW.form_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auto_increment_version_number
    BEFORE INSERT
    ON form_versions
    FOR EACH ROW
EXECUTE FUNCTION generate_form_version_number();

CREATE TYPE subscription_state AS ENUM (
    'free',
    'incomplete',
    'incomplete_expired',
    'trialing',
    'active',
    'past_due',
    'canceled',
    'unpaid'
    );

CREATE TABLE tenants
(
    id                  UUID PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
    subscription_status subscription_state DEFAULT 'free', -- active, past_due, canceled, inactive
    current_period_end  TIMESTAMPTZ,
    submission_quota    INT                DEFAULT 100,    -- Injected from Polar metadata on checkout
    submissions_used    INT                DEFAULT 0,      -- Reset every billing cycle
    updated_at          TIMESTAMPTZ        DEFAULT NOW()
);

CREATE TABLE forms
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID REFERENCES tenants (id) ON DELETE CASCADE,
    name                 TEXT NOT NULL,
    redirect_url         TEXT,
    turnstile_secret_key TEXT,
    is_active            BOOLEAN          DEFAULT TRUE,
    current_version_id   UUID, -- Circular reference handled after versions table
    created_at           TIMESTAMPTZ      DEFAULT NOW()
);

CREATE TABLE form_versions
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id        UUID REFERENCES forms (id) ON DELETE CASCADE,
    version_number INT NOT NULL, -- Sequential order increment (1, 2, 3...)
    allowed_fields JSONB,        -- Array of expected inputs with types and other info
    created_at     TIMESTAMPTZ      DEFAULT NOW(),
    UNIQUE (form_id, version_number)
);

-- Add the missing foreign key constraint for our safe circular lookup
ALTER TABLE forms
    ADD CONSTRAINT fk_current_version
        FOREIGN KEY (current_version_id) REFERENCES form_versions (id) ON DELETE SET NULL;

-- ==========================================
-- 3. THE LIGHTWEIGHT SUBMISSIONS TABLE
-- ==========================================
CREATE TABLE submissions
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id    UUID REFERENCES forms (id) ON DELETE CASCADE,
    version_id UUID  REFERENCES form_versions (id) ON DELETE SET NULL, -- Identifies exact structural era
    payload    JSONB NOT NULL,                                         -- Pure text form key-value hashes
    sender_ip  TEXT,                                                   -- For rate limiting / internal audits
    is_spam    BOOLEAN          DEFAULT FALSE,                         -- True if failed Turnstile or Honeypot
    created_at TIMESTAMPTZ      DEFAULT NOW()
);

-- Highly performant indexing for fast table scans
CREATE INDEX idx_submissions_form_id ON submissions (form_id);
CREATE INDEX idx_submissions_payload_gin ON submissions USING gin (payload);
```

### The Ingestion Flow & Quota Check

When a standard request hits your API endpoint, the system runs a lean, resource-efficient check against the
`profiles` table before evaluating any code logic:

```js
// Edge Handler Pre-flight Pseudo-logic
async function handleSubmission(req, formId) {
    // 1. Fetch form profile and usage metrics simultaneously
    const {data: form} = await supabase
        .from('forms')
        .select('is_active, profiles(subscription_status, submissions_used, submission_quota)')
        .eq('id', formId)
        .single();

    const profile = form.profiles;

    // 2. Gate access immediately if limits are breached
    if (!form.is_active || profile.subscription_status !== 'active') {
        return new Response('Form Inactive or Subscription Paused', {status: 403});
    }

    if (profile.submissions_used >= profile.submission_quota) {
        return new Response('Monthly Submission Limit Reached', {status: 429});
    }

    // 3. (Proceed to Honeypot, Turnstile Check, DB Write, and Amazon SES delivery...)
}

```

### Polar.sh Webhook Sync Handler

Polar.sh broadcasts clean events whenever a customer signs up or alters their membership status. Your webhooks must listen specifically for
`subscription.created` and `subscription.revoked` to instantly manipulate database access flags.

```js
// POST /api/webhooks/polar
export async function POST(req) {
    const payload = await req.text();
    const signature = req.headers.get('polar-signature');

    // Verify webhook event authenticity using your Polar webhook secret
    const event = verifyPolarWebhook(payload, signature, process.env.POLAR_WEBHOOK_SECRET);

    if (event.type === 'subscription.created') {
        await supabase
            .from('profiles')
            .update({
                subscription_status: 'active',
                current_period_end: event.data.current_period_end,
                submissions_used: 0 // Reset usage counter for the billing cycle
            })
            .eq('polar_customer_id', event.data.customer_id);
    }

    if (event.type === 'subscription.revoked') {
        await supabase
            .from('profiles')
            .update({subscription_status: 'inactive'})
            .eq('polar_customer_id', event.data.customer_id);
    }

    return new Response(JSON.stringify({received: true}), {status: 200});
}

```




PolarApiException
PolarAuthenticationException
PolarNotFoundException
PolarRateLimitException
PolarValidationException
