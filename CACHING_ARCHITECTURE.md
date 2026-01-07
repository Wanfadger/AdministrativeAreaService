# Caching Architecture Documentation

## Overview

This application uses **service-level caching** with clean JSON storage for cross-language compatibility. All caching logic is centralized in the service layer, making it easy to understand, maintain, and debug.

## Architecture Principles

### 1. **Service-Level Caching**
- All caching logic resides in the **service layer** (`AdministrativeAreaServiceImpl`)
- Controllers are **caching-agnostic** - they only handle HTTP concerns
- This reduces learning curve and ensures consistency

### 2. **Clean JSON Storage**
- Cached data is stored as **pure JSON** without Java class metadata (`@class` fields)
- Makes cached data **language-agnostic** - any service can read it
- Other services (Python, Node.js, etc.) can consume cached data seamlessly

### 3. **Explicit Type Conversion**
- Uses `ParameterizedTypeReference` for type-safe deserialization
- Converts `LinkedHashMap` (from clean JSON) to target types automatically
- Prevents casting errors while maintaining clean JSON

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP Request                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Controller Layer                              │
│  - Handles HTTP concerns only                                    │
│  - No caching annotations                                        │
│  - Delegates to service layer                                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Service Layer                                 │
│  AdministrativeAreaServiceImpl                                   │
│                                                                   │
│  1. Check Cache (CacheHelperService.get())                      │
│     ├─ Cache Hit → Return cached data                            │
│     └─ Cache Miss → Continue to step 2                          │
│                                                                   │
│  2. Fetch from Database                                          │
│                                                                   │
│  3. Cache Result (CacheHelperService.put())                      │
│     └─ Only cache successful responses (status == true)          │
│                                                                   │
│  4. Return Result                                                │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              CacheHelperService                                  │
│  - get(): Retrieve with type conversion                          │
│  - put(): Store with TTL                                         │
│  - evict(): Remove specific entry                                │
│  - evictAll(): Clear cache namespace                             │
│  - generateKey(): Create deterministic keys                     │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Redis (via RedisTemplate)                     │
│  - Stores clean JSON (no @class fields)                          │
│  - Keys: "CacheName::methodName:param1=value1&param2=value2"    │
│  - TTL: Varies by operation (1 hour, 7 days)                    │
└─────────────────────────────────────────────────────────────────┘
```

## Components

### CacheHelperService

**Location:** `com.wanfadger.AdministrativeareaApi.shared.util.CacheHelperService`

**Purpose:** Central service for all cache operations

**Key Methods:**

1. **`get(String cacheName, String key, ParameterizedTypeReference<T> typeRef)`**
   - Retrieves cached value
   - Converts `LinkedHashMap` to target type using `ParameterizedTypeReference`
   - Returns `null` if not found

2. **`put(String cacheName, String key, T value, long ttl, TimeUnit timeUnit)`**
   - Stores value in cache with TTL
   - Serializes to clean JSON (no `@class` fields)
   - Key format: `cacheName::key`

3. **`evict(String cacheName, String key)`**
   - Removes specific cache entry

4. **`evictAll(String cacheName)`**
   - Removes all entries in a cache namespace
   - Uses pattern matching: `cacheName::*`

5. **`generateKey(String methodName, Map<String, String> queryMap)`**
   - Creates deterministic cache keys
   - Sorts query parameters alphabetically
   - Format: `methodName:param1=value1&param2=value2`

### Cache Namespaces

**Location:** `com.wanfadger.AdministrativeareaApi.shared.util.CacheKeys`

- `ADMINISTRATIVE_AREAS`: Full details (code, name, coordinates)
- `ADMINISTRATIVE_AREAS_FILTER`: Code and name only

## Implementation Pattern

### Read Operations (Cache-Enabled)

```java
@Override
public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(
        Map<String, String> queryMap) {
    
    // 1. Generate cache key
    String cacheKey = CacheHelperService.generateKey("searchList", queryMap);
    
    // 2. Check cache
    ParameterizedTypeReference<AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>>> typeRef = 
        new ParameterizedTypeReference<AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>>>() {};
    
    AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> cached = 
        cacheHelper.get(CacheKeys.ADMINISTRATIVE_AREAS, cacheKey, typeRef);
    
    if (cached != null) {
        return cached; // Cache hit
    }
    
    // 3. Cache miss - fetch from database
    AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> result = 
        // ... database query logic ...
    
    // 4. Cache successful responses only
    if (result != null && result.isStatus()) {
        cacheHelper.put(CacheKeys.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
    }
    
    return result;
}
```

### Write Operations (Cache Eviction)

```java
@Override
public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(
        Map<String, String> queryMap, NewAdministrativeAreaDto dto) {
    
    // 1. Perform database operation
    // ... create logic ...
    
    // 2. Evict cache after write
    cacheHelper.evictAll(CacheKeys.ADMINISTRATIVE_AREAS);
    cacheHelper.evictAll(CacheKeys.ADMINISTRATIVE_AREAS_FILTER);
    
    return ResponseEntity.ok(...);
}
```

## Cache TTL Strategy

| Operation | TTL | Reason |
|-----------|-----|--------|
| `searchList()` | 1 hour | Frequently changing data with coordinates |
| `searchOne()` | 1 hour | Frequently changing data with coordinates |
| `filterList()` | 7 days | Relatively stable code/name data |
| `filterOne()` | 7 days | Relatively stable code/name data |
| `getParishByPartOf()` | 7 days | Relatively stable hierarchical data |

## Cache Key Format

**Pattern:** `CacheName::methodName:param1=value1&param2=value2`

**Examples:**
- `AdministrativeAreas::searchList:type=REGION`
- `AdministrativeAreas::searchList:type=SUB REGION&partOf=001`
- `AdministrativeAreaFilters::filterList:type=REGION`

**Key Generation:**
- Parameters are sorted alphabetically for consistency
- Ensures same query always generates same key
- Prevents cache misses due to parameter order

## Cache Eviction Strategy

### When Cache is Evicted

1. **After Create Operations** (`newOne`, `newList`)
   - Evicts all entries in both cache namespaces
   - Ensures fresh data after writes

2. **After Update Operations** (`updateOne`)
   - Evicts all entries in both cache namespaces
   - Ensures consistency

3. **After Bulk Upload** (`upload`)
   - Evicts all entries in both cache namespaces
   - Bulk operations affect multiple data types

### Eviction Implementation

```java
// In service methods after write operations
cacheHelper.evictAll(CacheKeys.ADMINISTRATIVE_AREAS);
cacheHelper.evictAll(CacheKeys.ADMINISTRATIVE_AREAS_FILTER);
```

## Serialization Details

### Storage Format

**Clean JSON (No Java Metadata):**
```json
{
  "status": true,
  "data": [
    {
      "code": "001",
      "name": "Central Region",
      "latitude": "0.5",
      "longitude": "32.5"
    }
  ],
  "message": "Success"
}
```

**NOT Stored As:**
```json
{
  "@class": "com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto",
  "status": true,
  "data": [
    {
      "@class": "com.wanfadger.AdministrativeareaApi.dto.RegionDto",
      "code": "001",
      ...
    }
  ]
}
```

### Type Conversion

When retrieving from cache:
1. Redis returns `LinkedHashMap` (clean JSON deserialized)
2. `CacheHelperService.get()` uses `ObjectMapper.convertValue()`
3. Converts `LinkedHashMap` to target type using `ParameterizedTypeReference`
4. Returns properly typed object

## Benefits

### 1. **Cross-Language Compatibility**
- Python, Node.js, Go, etc. can read cached JSON directly
- No need for Java-specific deserialization logic

### 2. **Consistency**
- All caching logic in one place (service layer)
- Easy to understand and maintain
- Reduces learning curve

### 3. **Type Safety**
- Uses `ParameterizedTypeReference` for generic types
- Prevents casting errors
- Compile-time type checking

### 4. **Flexibility**
- Different TTLs per operation
- Easy to adjust caching strategy
- Can selectively evict cache entries

### 5. **Debugging**
- Clear cache key format
- Easy to inspect in Redis
- Logging at cache operations

## Configuration

### Redis Configuration

**File:** `application-dev1.properties`

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Cache Serialization

**File:** `CacheConfig.java`

- Uses `Jackson2JsonRedisSerializer` with `deactivateDefaultTyping()`
- Produces clean JSON without `@class` fields
- Configured in `RedisTemplate` bean

## Monitoring

### Health Checks

**Endpoint:** `/actuator/health`

- Database health indicator
- Redis health indicator
- Cache manager health indicator

### Cache Diagnostics

**Endpoint:** `/cache-diagnostics/keys`

- Lists all Redis keys
- Shows cache statistics
- Useful for debugging

## Best Practices

### ✅ DO

1. **Always check cache before database queries**
   ```java
   T cached = cacheHelper.get(cacheName, key, typeRef);
   if (cached != null) return cached;
   ```

2. **Only cache successful responses**
   ```java
   if (result != null && result.isStatus()) {
       cacheHelper.put(...);
   }
   ```

3. **Evict cache after writes**
   ```java
   // After create/update/delete
   cacheHelper.evictAll(CacheKeys.ADMINISTRATIVE_AREAS);
   ```

4. **Use appropriate TTLs**
   - Short TTL (1 hour) for frequently changing data
   - Long TTL (7 days) for stable data

### ❌ DON'T

1. **Don't cache in controllers**
   - Keep controllers caching-agnostic
   - All caching logic in service layer

2. **Don't cache error responses**
   - Only cache successful responses (`status == true`)

3. **Don't forget to evict after writes**
   - Always evict relevant cache namespaces after modifications

4. **Don't use hardcoded cache keys**
   - Use `CacheHelperService.generateKey()` for consistency

## Troubleshooting

### Cache Not Working

1. **Check Redis connection**
   ```bash
   redis-cli ping
   ```

2. **Check cache keys in Redis**
   ```bash
   redis-cli keys "AdministrativeAreas::*"
   ```

3. **Check logs**
   - Look for cache-related warnings
   - Check `CacheHelperService` debug logs

### Type Conversion Errors

- Ensure `ParameterizedTypeReference` matches return type exactly
- Check that cached JSON structure matches DTO structure
- Verify `ObjectMapper` is properly configured

### Cache Not Evicting

- Verify `evictAll()` is called after write operations
- Check that cache namespaces match
- Ensure Redis connection is working

## Migration Notes

### From Controller-Level to Service-Level Caching

**Before:**
```java
@GetMapping("/searchList")
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, 
           keyGenerator = "queryMapKeyGenerator")
public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(...) {
    return service.searchList(...);
}
```

**After:**
```java
@GetMapping("/searchList")
public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(...) {
    return service.searchList(...); // Caching handled in service
}
```

**Service Implementation:**
```java
public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(...) {
    // Cache check, database query, cache put - all in service
}
```

## Examples

### Reading Cached Data from Other Services

**Python:**
```python
import redis
import json

r = redis.Redis(host='localhost', port=6379, db=0)
cached = r.get("AdministrativeAreas::searchList:type=REGION")
data = json.loads(cached)  # Clean JSON, no @class fields!
```

**Node.js:**
```javascript
const redis = require('redis');
const client = redis.createClient();

const cached = await client.get("AdministrativeAreas::searchList:type=REGION");
const data = JSON.parse(cached);  // Clean JSON!
```

**Java (Other Service):**
```java
String cached = redisTemplate.opsForValue().get("AdministrativeAreas::searchList:type=REGION");
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> data = mapper.readValue(cached, Map.class);  // Clean JSON!
```

## Summary

This caching architecture provides:
- ✅ **Clean JSON** for cross-language compatibility
- ✅ **Service-level** caching for consistency
- ✅ **Type-safe** deserialization
- ✅ **Flexible** TTL management
- ✅ **Easy** debugging and monitoring

All caching logic is centralized in the service layer, making it easy to understand, maintain, and extend.
