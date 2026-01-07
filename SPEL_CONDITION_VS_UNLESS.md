# SpEL Cache Condition vs Unless - The Critical Difference

## The Problem
Cache conditions using `#result` were not working, even though the syntax looked correct.

## Root Cause (From Spring Documentation)

**`condition` attribute:**
- Evaluated **BEFORE** method execution
- **Cannot access `#result`** (method hasn't returned yet)
- Can only check method parameters

**`unless` attribute:**
- Evaluated **AFTER** method execution
- **CAN access `#result`** (method has returned)
- Perfect for checking return value properties

## The Solution

### ❌ Wrong (Doesn't Work):
```java
@Cacheable(
    value = CacheKeys.ADMINISTRATIVE_AREAS,
    condition = "#result != null && #result.status == true"  // ❌ #result not available!
)
```

### ✅ Correct (Works):
```java
@Cacheable(
    value = CacheKeys.ADMINISTRATIVE_AREAS,
    unless = "#result == null || #result.status != true"  // ✅ #result is available!
)
```

## Logic Translation

**Original condition (what we want):**
- Cache IF: `result != null AND status == true`

**Converted to `unless` (inverted logic):**
- DON'T cache IF: `result == null OR status != true`
- Which means: Cache IF: `result != null AND status == true` ✅

## Updated Code

All cacheable endpoints now use `unless` instead of `condition`:

```java
@Cacheable(
    value = CacheKeys.ADMINISTRATIVE_AREAS,
    keyGenerator = "queryMapKeyGenerator",
    cacheManager = "hourCacheManager",
    unless = "#result == null || #result.status != true"  // ✅ Works!
)
public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(...) {
    return administrativeAreaService.searchList(queryMap);
}
```

## For Maps (CacheTestController)

```java
@Cacheable(
    value = CacheKeys.ADMINISTRATIVE_AREAS,
    key = "'test:condition'",
    unless = "#result == null || #result['status'] != true"  // ✅ Works!
)
public Map<String, Object> conditionTest() {
    // ...
}
```

## Key Takeaways

1. **Use `condition`** when checking method parameters (before execution)
2. **Use `unless`** when checking return value (after execution)
3. **Invert the logic** when converting from `condition` to `unless`
4. **`#result` is only available in `unless`**, not in `condition`

## Testing

After restarting the application:

```bash
# Test the endpoint with condition
curl "http://localhost:8084/cache-test/condition-test"

# Test the main endpoint
curl "http://localhost:8084/AdministrativeAreas2/searchList?type=REGION"

# Check cache keys
curl "http://localhost:8084/cache-diagnostics/keys"
```

Expected: Keys should now appear in Redis! 🎉

## Reference

- [Spring Framework Documentation - Cache Annotations](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)
- `condition`: Evaluated before method execution
- `unless`: Evaluated after method execution (can access `#result`)
