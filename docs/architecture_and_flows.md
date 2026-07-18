# Global Architecture & System Flows

This document synthesizes FormBox's macro design, class layouts, system-wide data flows, and global exception mapping.

## 1. Codebase Component Directory Structure

The project follows a standard Spring Boot layout mixed with Kotlin bridges:

```
src/main/
├── java/in/hridaykh/formbox/
│   ├── billing/                  # Monetization & Quotas (Polar Integration)
│   │   ├── controller/           # Webhook ingestion & Billing portal redirect endpoints
│   │   ├── model/                # Records representing ActiveSubscriptions, Entitlements, Meters
│   │   └── service/              # Entitlements caching, Meter counters, Webhook parsing
│   ├── config/                   # Configuration beans (Redis scripts, Polar client, properties)
│   ├── constant/                 # Views, Paths, and Cache keys registry constants
│   ├── controller/               # Spring MVC Page & Fragment Controllers (Auth, Dashboard, Forms)
│   ├── exception/                # Application exceptions and GlobalExceptionHandler
│   ├── filter/                   # CORS, IP Rate-limiting, and SupabaseSessionFilter
│   ├── model/
│   │   ├── dto/                  # DTO payloads for caching, requests, and tier validations
│   │   └── entity/               # Database Entities: Tenant, Form, Submission (JPA)
│   ├── repository/               # Database Query interfaces (TenantRepository, FormRepository, etc.)
│   └── service/                  # Core Business Services (Auth, Tenant onboarding, Caching)
└── kotlin/in/hridaykh/formbox/
    └── AuthServiceKt.kt          # Coroutines bridge to the Supabase Kotlin SDK
```

---

## 2. Complete User Journey Data Flow

Below is a lifecycle map depicting a tenant starting from registration, upgrading their plan via Polar, creating a form, and receiving submissions.

```mermaid
sequenceDiagram
    autonumber
    actor Tenant
    actor Submitter
    participant App as FormBox App
    participant Supabase as Supabase Auth
    participant Polar as Polar Platform
    participant Redis as Redis Cache
    participant DB as PostgreSQL DB

    %% 1. AUTHENTICATION
    Tenant->>App: Register / Login Form Submission (HTMX hx-post)
    App->>Supabase: Process credentials & return JWTs
    App->>DB: Provision Tenant workspace in DB with Free Entitlements
    App->>Tenant: Establish session (sb_token & sb_refresh cookies)

    %% 2. SUBSCRIPTION
    Tenant->>App: Click "Upgrade Plan" (Redirect)
    App->>Polar: Generate customer session (/customer-sessions/)
    Polar-->>App: Return Portal URL
    App->>Tenant: Redirect to Polar Customer Portal
    Tenant->>Polar: Complete checkout payment
    Polar->>App: Dispatch webhook (customer.state_changed)
    App->>DB: Parse features & save new Entitlements JSONB
    App->>Redis: Invalidate L2 Cache & backfill limits

    %% 3. CREATION
    Tenant->>App: Create form: "Feedback Form" (HTMX hx-post)
    App->>App: Audit forms count against Entitlements limit
    App->>DB: Persist Form details & custom rules
    App->>Redis: Invalidate cache
    App-->>Tenant: Redirect to manage form view

    %% 4. SUBMISSION
    Submitter->>App: Post submit to public endpoint (/f/{formId})
    App->>Redis: Audit IP / Form rate limit
    App->>Redis: Verify remaining submission quota
    App->>App: Parse honeypot / Turnstile captcha / file attachments
    App->>DB: Persist Submission payload in DB
    App->>Redis: Decrement remaining quota counter
    App->>App: Trigger async hooks (Webhooks / Discord integrations)
    App-->>Submitter: Return thanks confirmation
```

---

## 3. Global Exception Handling & HTMX Friendly Responses

FormBox handles application errors centrally to prevent server failures from interrupting client interfaces:

- **Component:** [GlobalExceptionHandler](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/in/hridaykh/formbox/exception/GlobalExceptionHandler.java)
- **Spring Mapping:** Annotated with `@ControllerAdvice` to intercept exceptions system-wide.

### Intercepted Errors
- **`NoResourceFoundException` / `FormNotFoundException`:** Responds with a `404 Not Found` payload status.
- **`MultipartException` / Malformed files uploads:** Returns a `400 Bad Request` explaining download interruptions.
- **`AuthRestException` / `TokenExpiredException`:** Translates Supabase authentication API codes into user-friendly descriptions (e.g. Session Expired).
- **Generic `Exception.class`:** Fallback for internal server breakdowns.

### HTMX Alert Strategy
To keep the dashboard interface smooth when loading pages via HTMX:
1. The backend builds a `ModelAndView` target mapping to [error.html](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/resources/templates/error.html) containing the fields `${errorTitle}`, `${errorMessage}`, and `${errorStatus}`.
2. The servlet response status code is explicitly set to **`HttpStatus.OK`** (`200`) using:
   ```java
   mav.setStatus(HttpStatus.OK);
   ```
3. Setting the status code to `200` ensures that HTMX processes the response body and swaps the error markup into the DOM container instead of triggering fallback generic network failure popups or ignoring the content.
