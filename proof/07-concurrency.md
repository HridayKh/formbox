# Proof: Concurrency / Shared State

Audit of shared mutable state, thread synchronization, Spring singleton statefulness, and data safety under multi-threaded request execution.

---

## PROVEN
Cases verified for safe concurrency and thread isolation:

1. [RedisCache.java](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/shared/RedisCache.java#L37)
   - Uses `ConcurrentHashMap<String, Object> keyLocks` and `synchronized(lock)` inside `computeIfAbsent` to prevent cache stampedes under high concurrent load, removing locks safely in `finally` blocks.
2. Spring Controllers & Services (`FormService.java`, `SubmissionController.java`, `AuthController.java`):
   - All Spring `@Service`, `@Controller`, and `@Repository` components use constructor injection for `private final` dependencies and maintain zero mutable instance fields, guaranteeing request-thread safety.

---

## UNPROVEN / GAPS
Shared state risks:

1. [HmacKey.java:9](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/shared/internal/HmacKey.java#L9)
   - **Risk**: `private String key` field is non-final and mutable. If instantiated or mutated across bean lifecycles without volatile/synchronization guards, concurrent readers could observe stale or partial key state.
2. [RedisCache.java:112](file:///home/hridaykh/Code/hriday_tech/formbox/src/main/java/formbox/shared/RedisCache.java#L112)
   - **Risk**: `keyLocks.remove(fullKey, lock)` is safe, but dynamic per-key lock creation under massive burst concurrency (e.g. 50,000 req/sec) creates high short-lived object allocation pressure.

---

## RECOMMENDATION

1. **HmacKey.java**: Mark the field `private final String key;` to ensure thread-safe publication across all memory model boundaries.
2. Maintain strict enforcement of immutability for all Spring managed singleton component fields.
