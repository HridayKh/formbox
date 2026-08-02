# dashboard

- push manage subs, email, logout, under a profile button
- only show submissions and upgrade button
- show messages, banners, etc. in place of upgrade button and open a modal when clicked
- options to contact support, give feedback, submit bugs, and request features (all powered by formbox!) 
- link to docs

# submissions

- mark spam as false positives
- spam reason!=none and is spam ==true, ie, false positive marked
- analyze this data maybe to improve spam?
- origin locks
- show banner if submissions dropped and drop reasons
- store submissions dropped due to out of submissions too but with a flag
- store 3rd party webhooks in a log and show them as a read only in the dash and let them replay them

# workers and ui/ux

- change rate limits to a single global value of 20rpm
- use cf worker's inbuilt rate limit system
- pages/roadmap
- highlight upcoming features properly on index landing page
- make sure the main differentiators are visible first, such as in-built dash for your clients, etc.
- make token refreshing async
- magic links
