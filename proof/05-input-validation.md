# Proof: Input Validation Boundary

Audit of public entry points (Controllers, Webhook handlers, Worker routes) for validation of parameters, payloads, headers, and path variables before reaching business logic.

---

## PROVEN
Cases verified with explicit input validation at the boundary:

1. [FolderController.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/folder/internal/FolderController.java#L32)
   - `folderName` is explicitly checked using `.isBlank()` before processing folder creation requests.
2. Strongly-typed UUID Path Variables across Controllers (`FormController.java`, `SubmissionController.java`, `FolderController.java`):
   - Spring MVC automatically validates string format for `@PathVariable UUID formId` / `@PathVariable UUID folderId`, rejecting non-UUID format strings with HTTP 400 before reaching service logic.

---

## UNPROVEN / GAPS
Entry points missing validation annotations, string length caps, format checks, or sanitization:

1. [FormController.java:68](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/form/internal/FormController.java#L68)
   - **Risk**: `@ModelAttribute FormSettingsRequest request` is missing `@Valid` annotation. Input attributes (`name`, `honeypotName`, `turnstileSecretKey`, `fieldValidationsRaw`) can be arbitrary strings of unbounded length or malformed data.
2. [FormController.java:31](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/form/internal/FormController.java#L31)
   - **Risk**: `redirectUrl` is accepted as a raw String without URL scheme/format validation. Malformed strings or `javascript:` URLs can be stored, leading to open redirect vulnerabilities or broken submission redirects.
3. [FolderController.java:39](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/folder/internal/FolderController.java#L39)
   - **Risk**: `folderName` length is not capped (e.g. max 255 chars). Large string payloads (100k+ chars) will cause database column length errors or memory bloat.
4. [NewControllers.java:68](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/dashboard/NewControllers.java#L68) & [AuthController.java:49](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/auth/internal/AuthController.java#L49)
   - **Risk**: `email` inputs rely on simple `.strip()` without structural email regex or `@Email` constraint validation at controller boundary.

---

## RECOMMENDATION

1. Add `@Valid` to form update controller methods and add Jakarta Bean Validation constraints to DTOs:
   ```java
   public record FormSettingsRequest(
       @NotBlank @Size(max = 255) String name,
       @URL String redirectUrl,
       @Size(max = 100) String honeypotName
   ) {}
   ```
2. Annotate controller input endpoints with `@Validated` and add length bounds to all user-controlled text inputs.
