# Billing System Architecture (Polar Integration)

Document the complete monetization layer here. Focus on data accuracy, webhook safety, and frontend access control.

### 1. Data Models & Database State
- **Tenant Entity / Table**:
  - The [Tenant](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/model/entity/Tenant.java) class maps to the `tenants` table.
  - It contains a JSONB column `entitlements` storing the serialized [Entitlements](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/model/Entitlements.java) snapshot.
- **Entitlements Tracking State**:
  - The [Entitlements](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/model/Entitlements.java) object tracks key billing state metrics:
    - `tier_name` (String): e.g., `"free"` (fallback: `FreeTierDefaults.TIER_NAME`).
    - `tier_priority` (int): Prioritizes tier features.
    - `refresh_at` (Instant): Next boundary for submission counter reset.
    - `recurring_interval` (String): Interval duration (e.g. `"free"`, `"month"`, `"year"`, `"one_time"`).
    - `submissions_limit` (long): Maximum submissions allowed per monthly cycle.
    - `forms_limit` (long): Maximum endpoints allowed.
    - `storage_limit_bytes` (long): Max allowed storage.
    - Feature Flags: `discord_notifs_allowed`, `turnstile_allowed`, `redirect_urls_allowed`, `json_forms_allowed`, `file_uploads_allowed`, `field_validations_allowed`, `slack_notifs_allowed`, `telegram_notifs_allowed`, `custom_webhooks_allowed`, `csv_exports_allowed`, `email_digests_allowed`, `altcha_allowed`.
    - Numeric Caps: `max_rate_limit_rpm`, `max_file_size_bytes`.
- **Mapping to Polar Objects**:
  - Mirroring occurs dynamically inside [WebhookService.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/service/WebhookService.java) from the `CustomerStateChanged` payload:
    - **Tenant Mapping**: Local `Tenant` primary key `id` (UUID) maps to Polar Customer's `external_id`.
    - **Tier Identity & Features**: Extracted from Polar Feature Flag benefits metadata (looks for keys `tier_priority`, `tier_name`, `feature_key`, `limit_key` + `limit_value`).
    - **Submissions Meter**: Derived from native Polar meter data (`ActiveMeters`). The code matches the `meterId` using `polar-ids.submission-meter-id` from [PolarIdProperties](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/PolarIdProperties.java).

### 2. Webhook Engine
- **Endpoint**: `/polar` inside [PolarWebhookController](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/controller/PolarWebhookController.java) (handling HTTP POST).
- **Execution Path**:
  1. **Signature Verification**: Verified against the configured secret key using `PolarWebhookVerifier.verify(body, webhookId, timestamp, signature)` in [PolarWebhookController.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/controller/PolarWebhookController.java).
  2. **Event Routing**: Binds request body and validates that the event type is `"customer.state_changed"`. It then delegates processing to `webhooksService.processHook(body)`.
  3. **Database & Cache Synchronization**: In [WebhookService.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/service/WebhookService.java#L34-L60):
     - Resolves the matching `Tenant` by parsing the external ID to UUID.
     - Builds a consolidated `Entitlements` snapshot using `createEntitlements(...)`.
     - Persists the new JSONB payload using `tenantRepository.save(tenant)`.
     - Updates L1 (Caffeine) and L2 (Redis) entitlements cache via `entitlementsCacheService.updateEntitlementsCache(...)`.
- **Architectural Watchpoint (Synchronous Processing)**:
  - The webhook payload processing runs **synchronously** within the webhook controller request thread before sending the HTTP 200 response back to Polar. If there are database locks or Redis failures, the thread will hang, potentially causing Polar's delivery worker to time out.

### 3. Checkout & Portal Lifecycle
- **Upgrade Flow**:
  - The user navigates to `/billing/upgrade` mapped inside [BillingController](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/controller/BillingController.java#L36-L53).
  - If the user is on the free tier, it invokes [TenantService.ensurePolarCustomerExists](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/service/TenantService.java#L40-L67) to lazily provision/verify the customer mirror profile on Polar via the Polar API.
  - The controller requests a new Customer Portal session link from Polar's API using `POST /customer-sessions/` (passing the `external_customer_id`).
  - HTMX / Redirect handling:
    - If the request contains the `HX-Request` header, it sets the response header `HX-Redirect` to the Polar-provided portal URL to break out of the HTMX frame.
    - For regular HTTP navigation, it responds with a standard Spring Redirect `redirect:{customerPortalUrl}`.

### 4. Code Smells & Overcomplications to Flag
Review of common antipatterns and architectural flags in the current codebase:
- **Redundant State Checking & Blocking Network I/O on Cache Miss**:
  - Yes, we are falling into this. In [PolarCacheService](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/service/PolarCacheService.java#L45-L62), when a Redis cache miss occurs (`formbox:meterBalance:{tenantId}` is null), the app falls back to [PolarMeterService.getRemainingSubmissionsBalance](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/service/PolarMeterService.java#L29-L51), executing a **synchronous REST call** to Polar's Customer Meters API. This block resides on the hot path for `/dashboard` generation. If Polar's API is slow, rate-limited, or down, the user's dashboard page will hang.
- **Synchronous Database Writes on GET Requests**:
  - In [PolarCacheService.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/billing/service/PolarCacheService.java#L63-L110), the helper `ensureEntitlementsRefresh` checks if `Instant.now()` has passed `refreshAt`. If so, it synchronously runs database read/write queries (`tenantRepository.saveAndFlush(...)`) to increment the cycle dates. This occurs during the dashboard GET request routing, which degrades page response times.
- **Local Integration Testing Broken by Auth Filter Exclusion**:
  - The security interceptor [SupabaseSessionFilter](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/filter/SupabaseSessionFilter.java#L34) excludes `/polar/**`, allowing incoming production webhooks to pass. However, the local testing endpoint is mapped to `/webhooks/polar/test` which begins with `/webhooks`. Because it is not in the filter's exclusion list, local webhook test triggers get blocked and redirected unless a valid auth session cookie is manually injected.