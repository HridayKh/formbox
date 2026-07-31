# cache

- negative caching for formMetadata
- generic redis caching system!!!!!
- change redis ttl to 7 days from last access instead of 7 days from creation
- proxy all repo calls over a cache
- add low ttl for in mem cache

# dashboard

- push manage subs, email, logout, under a profile button
- only show submissions and upgrade button
- show messages, banners, etc. in place of upgrade button and open a modal when clicked

# modularity

- move to modulith
- change root package from `in.hridaykh.formbox` to `formbox` and put modules under `formbox.*`
- Relaxed Module Boundaries while exploring and building, strict once stable and non changing
- small modules just being fully flat `formbox.module.internal.*` is fine, even `formbox.module.*` is okay for internal stuff

# submissions

- mark spam as false positives
- spam reason!=none and is spam ==true, ie, false positive marked
- analyze this data maybe to improve spam?
- origin locks
- show banner if submissions dropped and drop reasons
- store submissions dropped due to out of submissions too but with a flag
- store 3rd party webhooks in a log and show them as a read only in the dash and let them replay them

# jte

- move to jte
- teach vaidik jte
- treat controllers as react apps and services as the backend called by react apps and DTOs as the JSON contract
- don’t worry about making 2 line services and 20 line controllers as long as the service is the backend and controller is the frontend only

# workers and ui/ux

- change rate limits to a single global value of 20rpm
- use cf worker's inbuilt rate limit systen
- pages/roadmap
- highlight upcoming features properly on index landing page
- make sure the main differentiators are visible first, such as in-built dash for your clients, etc.
- make token refreshing async
- magic links
