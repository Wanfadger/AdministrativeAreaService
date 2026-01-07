# JSON Cache Serialization - Language-Agnostic Caching

## Problem
Cached data contained Java class metadata (`@class` fields), making it difficult for other languages to consume:
```json
{
  "@class": "com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto",
  "data": ["java.util.ImmutableCollections$ListN", [...]],
  ...
}
```

## Solution
Switched from `GenericJackson2JsonRedisSerializer` (includes type info) to `Jackson2JsonRedisSerializer` with type information disabled.

## Changes Made

### Before (Java-specific):
```java
GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
// Includes @class fields for type information
```

### After (Language-agnostic):
```java
private Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.deactivateDefaultTyping(); // No type information
    
    return new Jackson2JsonRedisSerializer<>(mapper, Object.class);
}
```

## Result

### Before:
```json
{
  "@class": "com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto",
  "data": ["java.util.ImmutableCollections$ListN", [
    {"@class": "com.wanfadger.AdministrativeareaApi.dto.CodeNameDto", "code": "REG001", "name": "Central Region"}
  ]],
  "message": "success",
  "status": true
}
```

### After (Pure JSON):
```json
{
  "data": [
    {"code": "REG001", "name": "Central Region"},
    {"code": "REG002", "name": "Northern Region"},
    {"code": "REG003", "name": "Eastern Region"}
  ],
  "message": "success",
  "status": true
}
```

## Benefits

1. **Language-agnostic**: Any language with JSON support can read the cached data
2. **Smaller cache size**: No Java class metadata reduces storage
3. **Human-readable**: Easier to debug and inspect in Redis Insight
4. **Standard JSON**: Works with any JSON parser (Python, Node.js, Go, etc.)

## Updated Components

All cache managers now use the new serializer:
- `cacheManager` (primary, 30 minutes TTL)
- `hourCacheManager` (1 hour TTL)
- `_24HourCacheManager` (24 hours TTL)
- `weekCacheManager` (7 days TTL)
- `monthCacheManager` (30 days TTL)
- `RedisTemplate` (for manual Redis operations)

## Testing

After restarting the application:

```bash
# Clear old cache (with Java metadata)
redis-cli -h localhost -p 8101 FLUSHDB

# Call endpoint to create new cache entry
curl "http://localhost:8084/AdministrativeAreas/filterList?type=REGION"

# Check cached data (should be pure JSON now)
redis-cli -h localhost -p 8101 GET "AdministrativeAreaFilters::filterList:type=REGION"
```

Expected: Pure JSON without `@class` fields, readable by any language.

## Compatibility

⚠️ **Important**: Existing cache entries with `@class` metadata will need to be cleared or will fail to deserialize. Clear the cache after deploying this change:

```bash
# Clear all cache
redis-cli -h localhost -p 8101 FLUSHDB

# Or clear specific cache keys
redis-cli -h localhost -p 8101 KEYS "AdministrativeArea*" | xargs redis-cli -h localhost -p 8101 DEL
```

## Notes

- The serializer still works with Spring's cache abstraction
- Deserialization works because Spring knows the return types from method signatures
- For manual Redis operations, you may need to specify types explicitly
