# Notes

## Ui Inspiration

1. formlink.io
2. staticforms.dev
3. splitforms.com
4. usebasin.com
5. slapform.com

## Billing Upgrade Notes

1. Map Java Record to all feature flags and limits (current balance per user + max balance per user)
2. user.getEntitlements().whateverFeatureFlagOrLimitHere()
3. ts can be cached ez

## Experiments

1. how long does the pipeline take without turnstile and the 2 redis lookups (on prod, near redis)
2. experiment with cf workers and cf cache to get sub 20ms 
3. try to get the same with just workers 
4. try to mini.ise latency without cf 
5. move the turnstile check and schema validation after the response if the user says so (force the user to choose on form creation, no defualts)

## Todo

### Unorganized

1. let user decide per form for errors (202 even if fail (hopefully for sub-20ms response times maybe) or throw proper errors)
2. magic mailto links

### Next Up

1. add turnstile to my own forms
2. show submissions balance to user
3. redirects on pro only, but turnstile on starter tier
4. turn post requests for redirects (like manage subscription, etc.) to get respects

### Tasks

1. increase strict rate limits and decrease lenient rate limits
2. instead of checking tier in thymeleaf, check it in controller per thing ex: form controller, send: `showManageSubscrition`, `showBuyStarter`, `showBuyPro`, etc.
3. redo the manage form page: separate tab for settings, show submission stats on form page
4. scheduler service: setup base for methods to run automatically on scheduled time, sync polar with redis more often
5. add otel to all methods and add more metadata: ip, user id, user sessions, etc.
6. change isSpam to spamReason: notSpam, turnstileFailed, etc.
7. optional (for now) auth improvements (password reset, password forget, oauth for quick signup)
8. revamp how forms are displayed and add paging
9. fix billing
10. put submissions older than last 100 for every form in cf r2
11. soft deleting forms
12. discord notifs (url field for discord webhook url and send it async, report errors to dashboard if failed through an errors table)
13. csv exports
14. redecide log levels and log usage everywhere
15. add annual plans to the index page as coming soon

### Later

1. Lifetime Plan ($89 maybe for lifetime pro tier or 3-5yr)
2. custom filters and rules on pro tier for submissions
3. email digests
4. telegram notifs
5. slack notifs
6. webhooks
7. 3rd party libs (react, etc.)
8. try sentry agent again
9. routing rules for webhooks and discord
10. implement the annual plans
11. proper page to manage which plan to buy in dashboard if polar hosted doesn't do that already

### Near Launch

1. start on admin panel
2. finish admin panel
3. auto delete free tier from polar.sh of higher tiers
4. knowledge base
5. legal docs
6. add a contact us plan to the frontend with call link
7. add `ALTCHA` as an option for spam protection
8. /onboarding page after sign up create a new form with generated fields, constantly
