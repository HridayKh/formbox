# Proof: External Failure Paths

Audit of DB calls, Redis operations, S3 requests, HTTP client calls (Supabase, ZeptoMail, Polar, Discord) for explicit timeout, retry, fallback, and exception propagation controls.

---

## PROVEN
Cases verified with explicit degradation or exception catching:

1. [RedisCache.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/shared/RedisCache.java#L57)
   - Every Redis operation (`get`, `set`, `increment`, `decrement`) is wrapped in `try/catch(Exception e)`. If Redis connection fails or times out, methods return `Optional.empty()` or fallback values, preventing cache failure from downing core services.
2. [AuthController.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/auth/internal/AuthController.java#L56)
   - Supabase Auth API calls catch `AuthRestException` and `TurnstileAuthException` to return error alert views instead of unmapped HTTP 500 pages.

---

## UNPROVEN / GAPS
Cases with uncaught external calls, missing retry policies, or missing fallbacks:

1. [UploadService.java:33](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/notifs/UploadService.java#L33)
   - **Risk**: `s3Client.putObject(...)` is called synchronously without a `try/catch`. If S3 drops connection, times out, or returns a 5xx error, an uncaught `SdkClientException` / `S3Exception` is thrown, aborting user form submissions.
2. [FormSubmissionService.java:147](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/submission/internal/FormSubmissionService.java#L147)
   - **Risk**: `asyncSendNotifs` invokes `discordNotif.sendDiscordNotif(...)` and `submissionEmailsService.sendEmailAutoresponse(...)` sequentially without local `try/catch` wrappers. If Discord or ZeptoMail API is unreachable or returns HTTP 500, execution halts mid-way, leaving email notifications unsent and database records unupdated.
3. [VerifiedEmailsService.java:70](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/dashboard/VerifiedEmailsService.java#L70)
   - **Risk**: `emailApi.sendGenericEmail(...)` is called synchronously during verification flow without `try/catch`. Email provider failure causes verification request to crash with an unhandled exception.
4. [index.js:98](file:///home/hridaykh/Code/hriday_tech/formbox/formboxWorker/src/index.js#L98)
   - **Risk**: `polarCacheService.getCachedSubmissionBalance` and `entitlementsCacheService.getEntitlements` in Cloudflare Worker edge script lack `try/catch` fallbacks. Edge worker crashes with 500 if KV/cache service fails.

---

## RECOMMENDATION

1. **UploadService.java**: Wrap `s3Client.putObject` in a `try/catch` and throw a domain-specific exception or return a fallback result:
   ```java
   try {
       s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(is, size));
   } catch (SdkException e) {
       log.error("Failed to upload file to S3", e);
       throw new StorageException("File storage failed", e);
   }
   ```
2. **FormSubmissionService.java**: Wrap individual notification channels (Discord, Email) in separate `try/catch` blocks so one channel failure does not block others.
3. Add Spring `@Retryable` or Resilience4j circuit breakers on external HTTP and S3 operations.
