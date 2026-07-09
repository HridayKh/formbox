# notes

1. focus first for submission pipeline
2. file uploads is pro only feature using cf r2
3. Submissions{free: 100, starter: 2k, pro: 30k}

## Form Submission Pipeline

1. get form (404 if db request says form doesn't exist)
2. per form rate limit (error 429)
3. check submissions quota
4. check if content type allowed
5. check honeypot
6. check turnstile
7. abort request if files not allowed (error 400)
8. abort request if invalid mime type on file (error 400)
9. check custom filters and validations (error 400)
10. save form payload and metadata
11. update leftover submission balance
12. update form submissions cache
13. return 200 ok
14. async start upload files/attachments
15. async 3rd party webhooks and notifs

## Todo

1. spam protection pipeline (rate limits, then turnstile)
2. custom filters and rules on pro tier for submissions
3. auth improvements (password reset, password forget)
4. revamp how forms are displayed and add paging
5. put submissions older than last 100 for every form in cf r2
6. soft deleting forms
7. discord notifs and custom webhooks
8. email digests
9. 3rd party libs (react, etc.)

### Quick

1. JSON output
2. htmx output
3. csv exports
4. limit num of forms

### Admin, Growth & Public Facing

1. fix waitlist, use my own form, with ability to return htmx
2. index page
3. start on admin panel
4. finish admin panel
5. auto delete free tier from polar.sh of higher tiers
6. knowledge base
7. legal docs
