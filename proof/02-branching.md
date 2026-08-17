# Proof: Exhaustive Branching

Audit of all pattern matching, switch statements, enum handle chains, and `if-else` cascades across non-ignored source files.

---

## PROVEN
Cases verified to handle all cases or include explicit catch-all fallback branches:

1. [ZeptoWebhookController.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/notifs/internal/ZeptoWebhookController.java#L68)
   - `switch(eventName.toLowerCase().strip())` includes a default branch (`default -> null;`), ensuring unexpected email event notifications from ZeptoMail return `null` safely without unhandled exception.
2. [GlobalExceptionHandler.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/shared/internal/GlobalExceptionHandler.java#L64)
   - Status code conversion from Supabase Auth exceptions is wrapped in a `try-catch(IllegalArgumentException)` block to catch unmapped HTTP status integers and default safely to `HttpStatus.BAD_REQUEST`.
3. [VerifiedEmailsService.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/dashboard/VerifiedEmailsService.java#L75)
   - Verification logic cascades from token verification to secondary verification parameters, explicitly returning `false` at the end of the decision tree.

---

## UNPROVEN / GAPS
Cases with incomplete branching or silent fall-through:

1. [PolarWebhookService.java:45](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/billing/service/PolarWebhookService.java#L45)
   - **Risk**: Webhook event processing branches on expected event type strings (e.g., `customer.state_changed`). Unrecognized event types silently pass through without logging or auditing. If Polar introduces new billing event types, they are ignored without trace.
2. [FormController.java:54](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/form/internal/FormController.java#L54)
   - **Risk**: Branching on form setting updates evaluates boolean flags without a default assertion check for missing parameters in model attributes, allowing uninitialized parameters to remain in ambiguous state.

---

## RECOMMENDATION

1. **PolarWebhookService.java**: Add explicit logging for unknown webhook event types in an `else` / `default` branch:
   ```java
   log.info("Unhandled Polar webhook event type: {}", eventType);
   ```
2. Enforce compiler checks for sealed classes/interfaces and enums across all new domain events to mandate exhaustive switch statements at compile time.
