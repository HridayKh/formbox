# Proof: Null Safety

Audit of all source code (excluding `/landingPage/` and `/research/`) for null checks, unguarded `.get()`, `.orElseThrow()`, unchecked casts, and raw `null` returns.

---

## PROVEN
Cases verified to be exhaustively and safely handled:

1. [DashboardController.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/dashboard/DashboardController.java#L52)
   - `userMetadata` and `userMetadata.getSub()` are explicitly checked for `null` before parsing UUID, preventing `NullPointerException` on unauthenticated or malformed session context.
2. [VerifiedEmailsService.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/dashboard/VerifiedEmailsService.java#L81)
   - `.get()` calls on `Optional` objects are strictly guarded by prior `.isPresent()` checks.
3. [FolderController.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/folder/internal/FolderController.java#L65)
   - `Optional.get()` usage is guarded by prior checks returning appropriate response status when empty.
4. [PolarWebhookService.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/billing/service/PolarWebhookService.java#L67)
   - Implements a helper method `nullSafe(List<T> list)` returning `List.of()` when `list` is `null`, preventing null iteration in webhook payload mapping.
5. [FormSubmissionService.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/submission/internal/FormSubmissionService.java#L55)
   - `request.getContentType()` and `request.getHeader("Accept")` are explicitly checked against `null` before evaluating string matchers.

---

## UNPROVEN / GAPS
Cases with missing or unsafe null handling:

1. [DashboardController.java:108](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/dashboard/DashboardController.java#L108)
   - **Risk**: `thisFolder.getFirst()` is called after filtering `folders` by `folderId`. If a form points to a deleted or non-existent folder ID, filtering yields an empty list, causing `NoSuchElementException` (500 Internal Server Error).
2. [DashboardController.java:140](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/dashboard/DashboardController.java#L140)
   - **Risk**: `folder.toList().getFirst()` assumes the filtered folder list contains at least one match. If the requested folder ID is invalid or inaccessible, it throws `NoSuchElementException`.
3. [IndexController.java:51](file:///home/hridaykh/Code/hriday_tech/formbox/formboxWorker/IndexController.java#L51)
   - **Risk**: `payload.getOrDefault(form.honeypotName(), "")` will throw `NullPointerException` if `form.honeypotName()` returns `null` (Map key lookup with null key or method call on null string).
4. [IndexController.java:62](file:///home/hridaykh/Code/hriday_tech/formbox/formboxWorker/IndexController.java#L62)
   - **Risk**: `form.turnstileSecretKey()` is passed directly to `TurnstileVerifier.turnstileFailed`. If null, downstream HTTP request formatting or verification can throw `NullPointerException`.
5. [index.js:114](file:///home/hridaykh/Code/hriday_tech/formbox/formboxWorker/src/index.js#L114)
   - **Risk**: `payload[form.honeypotName]` returns `undefined` if `form.honeypotName` is unconfigured or not present in `payload`. Calling `.toString()` directly without optional chaining will throw `TypeError: Cannot read properties of undefined` if `payload[form.honeypotName]` is `null`.

---

## RECOMMENDATION

1. **DashboardController.java**: Replace `getFirst()` calls with stream `.findFirst()` handling or safe fallback:
   ```java
   Folder folder = folders.stream().filter(f -> f.getId().equals(form.getFolderId())).findFirst().orElse(null);
   ```
2. **IndexController.java & index.js**: Default nullable entity fields to non-null fallback values or guard them before processing:
   ```java
   String honeypot = form.honeypotName() != null ? form.honeypotName() : "_gotcha";
   ```
