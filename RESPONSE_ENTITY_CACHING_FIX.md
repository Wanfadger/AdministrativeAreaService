# ResponseEntity Caching Fix

## Problem
When caching `ResponseEntity` objects, Redis deserialization fails with:
```
Could not read JSON: Cannot construct instance of `org.springframework.http.ResponseEntity` 
(no Creators, like default constructor, exist)
```

## Root Cause
`ResponseEntity` is a complex Spring object that:
- Doesn't have a default constructor
- Contains internal state (headers, status codes, etc.)
- Cannot be properly deserialized from JSON

## Solution
**Cache the response body, not the ResponseEntity wrapper.**

### ❌ Wrong (causes deserialization error):
```java
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, key = "'test:simple'")
public ResponseEntity<Map<String, Object>> simpleCache() {
    return ResponseEntity.ok(Map.of("message", "test"));
}
```

### ✅ Correct (caches the body):
```java
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, key = "'test:simple'")
public Map<String, Object> simpleCache() {
    Map<String, Object> response = new HashMap<>();
    response.put("message", "test");
    return response;
}
```

Spring will automatically wrap the returned object in a `ResponseEntity` when returning to the client.

## Good News: Main Controllers Are Already Correct

Your main controllers (`AdministrativeAreaController` and `AdministrativeAreaControllerImproved`) already return DTOs directly:

```java
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, ...)
public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(...) {
    return administrativeAreaService.searchList(queryMap);
}
```

This is correct! They return `AdministrativeAreaResponseDto`, not `ResponseEntity<AdministrativeAreaResponseDto>`.

## Best Practices

1. **Cache DTOs, not ResponseEntity**: Always cache the data object, not the HTTP wrapper
2. **Use DTOs for complex responses**: DTOs are designed to be serializable
3. **Let Spring handle ResponseEntity**: Spring will wrap your cached DTO in ResponseEntity automatically

## Example Pattern

```java
// ✅ Good - Cache the DTO
@Cacheable(value = "myCache", key = "#id")
public MyDto getData(String id) {
    return service.getData(id);
}

// ❌ Bad - Don't cache ResponseEntity
@Cacheable(value = "myCache", key = "#id")
public ResponseEntity<MyDto> getData(String id) {
    return ResponseEntity.ok(service.getData(id));
}
```

## Testing

After the fix, test the endpoint:
```bash
# First call - creates cache
curl "http://localhost:8084/cache-test/simple"

# Second call - should return from cache (no error)
curl "http://localhost:8084/cache-test/simple"
```

The timestamp should remain the same on the second call, proving it's cached.
