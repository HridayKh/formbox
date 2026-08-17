# Proof: Resource Handling

Audit of opened streams, HTTP response streams, file handles, database connections, and memory allocations across non-ignored source files.

---

## PROVEN
Cases verified to properly release resources:

1. [AuthServiceKt.kt](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/kotlin/formbox/auth/internal/AuthServiceKt.kt#L44)
   - `client.close()` is explicitly invoked in `closeIsolatedClient` inside a try/finally block to guarantee Supabase HTTP client closure.
2. Spring DB & Redis Connection Pools:
   - DataSource and Redis connection management rely on HikariCP and Spring Data Lettuce connection pooling, returning connections safely to pools on request completion.

---

## UNPROVEN / GAPS
Resource handling gaps, stream leaks, or memory bloat risks:

1. [UploadService.java:23](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/notifs/UploadService.java#L23)
   - **Risk**: `uploadFile(InputStream is, ...)` receives an `InputStream` and passes it to AWS SDK `RequestBody.fromInputStream(is, size)`, but **never closes `is`**.
2. [FormSubmissionService.java:89](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/submission/internal/FormSubmissionService.java#L89)
   - **Risk**: `part.getInputStream()` is opened for every file part in a multipart request and passed directly to `uploadService.uploadFile`. There is no `try-with-resources` or `finally` block to close the input streams. Unclosed input streams leak underlying web server file descriptors and temporary disk buffers.
3. [CsvExportServiceImpl.java:69](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/submission/internal/CsvExportServiceImpl.java#L69)
   - **Risk**: The entire CSV export payload is constructed in heap memory as a single `byte[]` via `StringBuilder`. For forms with large submission counts, this creates severe memory pressure and can trigger `OutOfMemoryError` (OOM crash).

---

## RECOMMENDATION

1. **FormSubmissionService.java & UploadService.java**: Use `try-with-resources` when acquiring file part input streams:
   ```java
   try (InputStream is = part.getInputStream()) {
       uploadService.uploadFile(is, part.getSubmittedFileName(), part.getSize(), part.getContentType());
   }
   ```
2. **CsvExportServiceImpl.java**: Refactor export generation to stream rows directly to a temporary file on disk using buffered output streams, then upload the file stream to S3.
