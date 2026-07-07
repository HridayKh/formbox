# notes

- focus first for submission pipeline
- file uploads is pro only feature using cf r2
- Submissions{free: 100, starter: 2k, pro: 30k}

## Todo

### Performance, Security & Core Infra

- sentry integration (Crucial to catch errors while optimizing/fixing auth)
- SENTRY OTEL AGENT
- auth fixes (link to home page on auth pages, magic links)
- spam protection pipeline (rate limits, then turnstile)
- put submissions older than last 100 for every form in cf r2

### Form Management & Data Lifecycle

- expanded form settings (custom rate limits, spam toggles (force _gotcha, require turnstile), etc.)
- turnstile
- custom filters and rules on pro tier for submissions
- limit num of forms (submissions are already limited)
- soft deleting old data
- revamp how forms are displayed and add paging

### Integrations, Exports & Notifications

- discord notifs and custom webhooks
- email digests
- JSON output
- csv exports
- 3rd party libs (react, etc.)
- put cache keys and names in properties file

### Admin, Growth & Public Facing

- fix waitlist, use my own form, with ability to return htmx
- start on admin panel
- finish admin panel
- index page
- knowledge base
- legal docs
- auto delete free tier from polar.sh of higher tiers
