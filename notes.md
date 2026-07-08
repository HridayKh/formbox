# notes

- focus first for submission pipeline
- file uploads is pro only feature using cf r2
- Submissions{free: 100, starter: 2k, pro: 30k}

## Form Submission Pipeline

1. ip rate limiting
2. check _gotcha field
3. start async request to get form info
4. check turnstile
5. 404 if db request says form doesn't exist
6. per form rate limit (error 429)
7. get form tenant submissions (error 402)
8. abort request if files not allowed (error 400)
9. abort request if invalid mime type on file (error 400)
10. check custom filters and validations (error 400)
11. save form payload and metadata
12. update leftover submission balance
13. return 200 ok
14. update form submissions cache
15. async start upload files/attachments
16. async (wait for file uploads) 3rd party webhooks and notifs

## Todo

### Performance, Security & Core Infra

- Single externally hosted, in app cached JSON file that controls allowed features
- auth fixes (link to home page on auth pages, magic links)
- spam protection pipeline (rate limits, then turnstile)
- put submissions older than last 100 for every form in cf r2

### Form Management & Data Lifecycle

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
