# Forms & Submissions (Core Architecture)

This document details the reverse-engineered Form Engine modeling, submission pipeline, validation phases, and the HTMX interaction matrix.

## 1. The Form Engine

Forms are defined as database entities that control validation, security headers, limits, and feature flags.

### Database Schema Mappings

#### `Form` Entity (`forms` table)
- **`id`** (`UUID`, Primary Key): Uniquely identifies the endpoint target.
- **`tenant_id`** (`UUID`, Foreign Key): Belongs to a specific tenant workspace.
- **`name`** (`String`): Descriptive name for the dashboard view.
- **`redirect_url`** (`String`, Nullable): Custom URL redirect target for standard HTML form submissions.
- **`honeypot_name`** (`String`, Nullable): Invisible input name to decoy spam bots.
- **`turnstile_secret_key`** (`String`, Nullable): Private key used to verify Cloudflare Turnstile tokens.
- **`rate_limit_rpm`** (`Integer`): Maximum requests-per-minute allowed for the form endpoint.
- **`allow_files`** (`Boolean`): Allows multipart file attachments.
- **`allow_json`** (`Boolean`): Accepts JSON request payloads.
- **`allow_htmx`** (`Boolean`): Enables HTMX-friendly responses.
- **`is_active`** (`Boolean`): Toggle to enable or disable the endpoint.
- **`field_validations`** (`JSONB` / Array of Strings): Custom criteria expressions for field validation checks.

#### `Submission` Entity (`submissions` table)
- **`id`** (`UUID`, Primary Key): Submission identifier.
- **`form_id`** (`UUID`, Foreign Key): Associated Form.
- **`payload`** (`JSONB` Map): Key-value pairs containing user submitted inputs.
- **`remote_addr`** (`String`): Originating IP address of the sender.
- **`is_spam`** (`Boolean`): Flag indicating if the submission failed spam checks.
- **`created_at`** (`OffsetDateTime`): Capture time of submission.

---

## 2. Submission Pipeline Lifecycle

When a public client posts to `/f/{formId}`, the request undergoes a structured validation, logging, and dispatch pipeline:

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Controller as IndexController
    participant Service as FormSubmissionService
    participant Redis as Redis Cache
    participant DB as Postgres DB
    participant Hook as FormFileService

    Client->>Controller: POST /f/{formId} (Form Data or JSON)
    
    Controller->>Service: Retrieve form config (Cache/DB)
    alt Form does not exist
        Controller-->>Client: HTTP 404 (submit/form-not-found)
    end

    Controller->>Service: Check rate limits (Redis increment)
    alt Limit exceeded
        Controller-->>Client: HTTP 429 (submit/rate-limit)
    end

    Controller->>Service: Check tenant submission balance
    alt Quota empty
        Controller-->>Client: HTTP 402 (submit/out-of-submissions)
    end

    Controller->>Service: Validate payload type (JSON or Standard)
    alt JSON not allowed but sent
        Controller-->>Client: HTTP 200 (submit/json-not-allowed)
    end

    alt Honeypot field filled
        Controller->>DB: Save silently as Spam
        Controller-->>Client: HTTP 200 (submit/thanks)
    end

    alt Turnstile verification fails
        Controller->>DB: Save silently as Spam
        Controller-->>Client: HTTP 200 (submit/thanks)
    end

    alt Files attached but disallowed
        Controller-->>Client: HTTP 400 (submit/files-not-allowed)
    end

    alt Files contain invalid mime types
        Controller-->>Client: HTTP 400 (submit/files-not-allowed)
    end

    Controller->>Service: Evaluate field validation expressions
    alt Validation fails
        Controller-->>Client: HTTP 400 (submit/invalid-fields)
    end

    Controller->>DB: Persist Submission (isSpam = false)
    Controller->>Redis: Decrement cached quota balance
    Controller->>Hook: Trigger Async Files & Hooks (Webhooks / Discord / Slack)
    
    alt Request is JSON
        Controller-->>Client: HTTP 200 (submit/json-response)
    else Has Redirect URL
        Controller-->>Client: HTTP 302 Redirect to redirectUrl
    else Default
        Controller-->>Client: HTTP 200 (submit/thanks)
    end
```

---

## 3. HTMX Interaction Matrix (Dashboard)

The forms dashboard relies on HTMX for responsive updates, page navigation, and options setting swaps.

| User Action | Triggering Element | HTMX Attribute | Target Element | Swap Method | Spring Controller Endpoint | Returned Thymeleaf Fragment |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **List Forms** | Endpoints Container | `hx-get="/forms"` `hx-trigger="load"` | `#endpoints-tbody` | `innerHTML` | `GET /forms` in `FormController` | `fragments/form-list :: form-rows` |
| **Create Form** | New Form Modal Submit | `hx-post="/forms"` | `#endpoints-tbody` | `beforeend` | `POST /forms` in `FormController` | `auth/fragments :: empty-frag` (Sets `HX-Redirect` header) |
| **Delete Form** | Delete Button click | `hx-delete` (Client Script trigger or direct mapping) | Modal Dismiss / List refresh | OOB / Direct | `DELETE /forms/{id}` in `FormController` | `void` (Sends HTTP 200) |
| **Update Settings** | Form Settings Submit | `hx-put` | `#settings-panel-container` | `innerHTML` | `PUT /forms/{id}` in `FormController` | `dashboard/manage-form :: settings-panel` (OOB heading swap: `#form-heading-settings`) |
| **Logout** | Logout header button | `hx-post="/auth/logout"` | Window Redirect | header `HX-Redirect` | `POST /auth/logout` in `AuthController` | `auth/fragments :: empty-frag` (triggers redirect to `/auth/login`) |
