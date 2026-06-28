# FormBox Core Architecture & Anti-Abuse Spec

## 1. Plan Feature Matrix

Our core offering provides high-volume ingestion by utilizing raw edge-compute infrastructure, removing expensive middle-tier operational markup.

- Submission Volume: 5,000 processed submissions per billing cycle.
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
