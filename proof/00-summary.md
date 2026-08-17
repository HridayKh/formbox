# Codebase Correctness & Exhaustive Error Handling Audit: Summary

**Audit Target**: `formbox` codebase (Java/Kotlin in `src/main/`, JS/Java in `formboxWorker/`).  
**Excluded Scope**: `/landingPage/` and `/research/`.  
**Mode**: Read-only static analysis audit. No application source code was modified.

---

## Category Scorecard

| Category | Status | Proven Safe Cases | Identified Gaps | Scorecard |
| :--- | :---: | :---: | :---: | :---: |
| **01. Null Safety** | ⚠️ GAPS | 5 | 5 | FAIL |
| **02. Exhaustive Branching** | ✅ PASS | 3 | 2 | PASS |
| **03. External Failure Paths** | ⚠️ GAPS | 2 | 4 | FAIL |
| **04. Exception Handling Coverage** | ⚠️ GAPS | 3 | 3 | FAIL |
| **05. Input Validation Boundary** | ⚠️ GAPS | 2 | 4 | FAIL |
| **06. Resource Handling** | ⚠️ GAPS | 2 | 3 | FAIL |
| **07. Concurrency / Shared State** | ✅ PASS | 2 | 2 | PASS |

**Overall Audit Result**: **5/7 Categories Have Actionable Gaps (Pass Rate: 2/7)**

---

## Top 5 Highest-Risk Gaps

### 1. Unclosed File `InputStream` Handles Leaking Resources
- **Location**: [FormSubmissionService.java:89](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/submission/internal/FormSubmissionService.java#L89) & [UploadService.java:23](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/notifs/UploadService.java#L23)
- **Concern**: Resource Handling
- **Risk**: `part.getInputStream()` is opened for multipart file uploads and passed to `uploadService.uploadFile` without a `try-with-resources` block or explicit `.close()`. Under sustained submission load, unclosed input streams leak underlying file descriptors and temp buffers until garbage collected.
- **Fix**: Wrap stream acquisition in `try (InputStream is = part.getInputStream()) { ... }`.

### 2. Uncaught S3 & Notification API External Failures
- **Location**: [UploadService.java:33](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/notifs/UploadService.java#L33) & [FormSubmissionService.java:147](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/submission/internal/FormSubmissionService.java#L147)
- **Concern**: External Failure Paths
- **Risk**: Synchronous S3 uploads (`putObject`) and multi-channel notifications (Discord, ZeptoMail) lack local `try-catch` handlers. S3 network timeouts or notification provider outages cause the entire submission transaction to fail, discarding user submissions.
- **Fix**: Wrap S3 operations and each notification dispatch channel in individual `try-catch` blocks; add `@Retryable` policies.

### 3. In-Memory Large Byte Array Allocation on CSV Exports
- **Location**: [CsvExportServiceImpl.java:69](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/submission/internal/CsvExportServiceImpl.java#L69)
- **Concern**: Resource Handling / Memory Safety
- **Risk**: CSV export data for a form is buffered entirely in heap memory as a `byte[]`. Exporting forms with large numbers of submissions causes high JVM memory spikes and can crash the application process with an `OutOfMemoryError` (OOM).
- **Fix**: Stream database submission rows directly to a temporary disk file buffer using `BufferedWriter` before uploading the stream to S3.

### 4. Unguarded `.getFirst()` on Filtered Lists Causing 500 Errors
- **Location**: [DashboardController.java:108](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/dashboard/DashboardController.java#L108) & [DashboardController.java:140](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/dashboard/DashboardController.java#L140)
- **Concern**: Null Safety
- **Risk**: Invoking `.getFirst()` on stream filtering results assumes a matching folder always exists. If a form references a deleted or non-existent folder ID, it throws `NoSuchElementException`, returning an unhandled HTTP 500 error page.
- **Fix**: Use `.findFirst().orElse(null)` and handle missing parent folders gracefully in UI rendering.

### 5. Missing `@Valid` & Boundary Input Restrictions on Form Controllers
- **Location**: [FormController.java:68](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/form/internal/FormController.java#L68) & [FormController.java:31](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/form/internal/FormController.java#L31)
- **Concern**: Input Validation Boundary
- **Risk**: `@ModelAttribute FormSettingsRequest` lacks `@Valid` annotations. Controller endpoints accept unvalidated string lengths and unformatted `redirectUrl` strings without validation, exposing the system to database length errors and unsafe redirects.
- **Fix**: Add Jakarta `@Valid` annotations to controller model attributes and apply `@Size` and `@URL` constraints on DTO properties.

---

## Detailed Reports Directory

Each specific concern area has been analyzed in detail with `file:line` references, proven safe cases, unproven gaps, and recommended minimal fixes:

- [01-null-safety.md](file:///home/hridaykh/Code/hriday_tech/formbox/proof/01-null-safety.md)
- [02-branching.md](file:///home/hridaykh/Code/hriday_tech/formbox/proof/02-branching.md)
- [03-external-failures.md](file:///home/hridaykh/Code/hriday_tech/formbox/proof/03-external-failures.md)
- [04-exception-coverage.md](file:///home/hridaykh/Code/hriday_tech/formbox/proof/04-exception-coverage.md)
- [05-input-validation.md](file:///home/hridaykh/Code/hriday_tech/formbox/proof/05-input-validation.md)
- [06-resource-handling.md](file:///home/hridaykh/Code/hriday_tech/formbox/proof/06-resource-handling.md)
- [07-concurrency.md](file:///home/hridaykh/Code/hriday_tech/formbox/proof/07-concurrency.md)
