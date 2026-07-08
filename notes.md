# notes

1. focus first for submission pipeline
2. file uploads is pro only feature using cf r2
3. Submissions{free: 100, starter: 2k, pro: 30k}

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

1. Single externally hosted, in app cached JSON file that controls allowed features
2. auth improvements (link to home page on auth pages, magic links)
3. spam protection pipeline (rate limits, then turnstile)
4. put submissions older than last 100 for every form in cf r2
5. custom filters and rules on pro tier for submissions
6. limit num of forms
7. soft deleting old data
8. revamp how forms are displayed and add paging
9. discord notifs and custom webhooks
10. email digests
11. JSON output
12. htmx output
13. csv exports
14. 3rd party libs (react, etc.)

### Admin, Growth & Public Facing

1. fix waitlist, use my own form, with ability to return htmx
2. index page
3. start on admin panel
4. finish admin panel
5. auto delete free tier from polar.sh of higher tiers
6. knowledge base
7. legal docs
