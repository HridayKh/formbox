# Proof: Exception Handling Coverage

Audit of exception handling across all layers, including `@ControllerAdvice`, global handlers, middleware, and catch-all safe responses.

---

## PROVEN
Cases verified to catch exceptions and return controlled, non-leaking responses:

1. [GlobalExceptionHandler.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/shared/internal/GlobalExceptionHandler.java#L20)
   - Contains `@ExceptionHandler(Exception.class)` as a last-resort catch-all. It logs the unhandled exception and renders a generic error view (`error.jte`) with `HttpStatus.INTERNAL_SERVER_ERROR`.
2. [GlobalExceptionHandler.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/shared/internal/GlobalExceptionHandler.java#L75)
   - Specifically handles `TokenExpiredException`, returning an HTTP 401 response with friendly UI message ("Session Expired").
3. [GlobalExceptionHandler.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/shared/internal/GlobalExceptionHandler.java#L45)
   - Handles `MultipartException` (e.g. client disconnect mid-upload) and converts it to a 400 Bad Request instead of a 500 error.

---

## UNPROVEN / GAPS
Cases where exceptions can bypass handlers, leak state, or be swallowed silently:

1. [CsvExportServiceImpl.java:64](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/submission/internal/CsvExportServiceImpl.java#L64)
   - **Risk**: Asynchronous CSV export method catches broad `Exception e`, logs it, and swallows it without notifying the user or updating export job status. The user UI hangs waiting for an export that silently failed.
2. [PolarWebhookService.java:36](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/billing/service/PolarWebhookService.java#L36)
   - **Risk**: `objectMapper.readTree(rawBody)` throws checked `JsonProcessingException` on malformed payload. Without explicit controller/service level catch or advice mapping, Webhook endpoint responds with default Spring Whitelabel 500 HTML instead of expected JSON error response for webhooks.
3. [FolderController.java:43](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/folder/internal/FolderController.java#L43)
   - **Risk**: Uncaught `DataIntegrityViolationException` (e.g. database foreign key or non-null constraint violation) falls back to generic 500 handler rather than informing user of constraint failure.

---

## RECOMMENDATION

1. **CsvExportServiceImpl.java**: Implement status tracking or failure event publishing when async export fails:
   ```java
   } catch (Exception e) {
       log.error("Failed to export CSV for form {}", formId, e);
       notificationService.notifyExportFailed(formId, e.getMessage());
   }
   ```
2. **PolarWebhookController.java**: Handle JSON parsing exceptions explicitly and return 400 Bad Request JSON response expected by webhook providers.
