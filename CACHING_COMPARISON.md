# Caching at Controller vs Service Level - Comparison

## Problem Statement
When caching clean JSON (without `@class` fields) for cross-language compatibility, Spring Cache's `@Cacheable` annotation at the controller level causes `LinkedHashMap` casting errors because:
- Clean JSON deserializes to `LinkedHashMap`
- Spring Cache doesn't know the target type at deserialization time
- Results in: `LinkedHashMap cannot be cast to AdministrativeAreaResponseDto`

## Solution Comparison

### ❌ Controller-Level Caching (Previous Approach)

**Implementation:**
```java
@GetMapping("/searchList")
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, 
           keyGenerator = "queryMapKeyGenerator",
           cacheManager = "hourCacheManager")
public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(...) {
    return administrativeAreaService.searchList(queryMap);
}
```

**Pros:**
- ✅ Simple, declarative with `@Cacheable`
- ✅ Automatic cache management
- ✅ Works well with type-aware serializers (GenericJackson2JsonRedisSerializer)

**Cons:**
- ❌ **Cannot use clean JSON** - requires `@class` metadata for type information
- ❌ **Cross-language incompatibility** - other services can't read cached data
- ❌ **Casting errors** - when using clean JSON, deserializes to `LinkedHashMap`
- ❌ **No explicit type control** - Spring Cache infers types from method signature

**When to Use:**
- Single-language applications (Java-only)
- When you can accept Java-specific metadata in cached JSON
- Simple caching needs without cross-service requirements

---

### ✅ Service-Level Caching (Current Approach)

**Implementation:**
```java
// Service Layer
@Override
public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(...) {
    // Check cache with explicit type
    String cacheKey = ServiceLevelCacheHelper.generateKey("searchList", queryMap);
    ParameterizedTypeReference<AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>>> typeRef = 
        new ParameterizedTypeReference<AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>>>() {};
    
    AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> cached = 
        cacheHelper.get(CacheKeys.ADMINISTRATIVE_AREAS, cacheKey, typeRef);
    
    if (cached != null) {
        return cached;
    }

    // Cache miss - fetch from database
    AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> result = ...;
    
    // Cache the result
    if (result != null && result.isStatus()) {
        cacheHelper.put(CacheKeys.ADMINISTRATIVE_AREAS, cacheKey, result, 1, TimeUnit.HOURS);
    }
    
    return result;
}

// Controller Layer (no caching annotation)
@GetMapping("/searchList")
public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(...) {
    return administrativeAreaService.searchList(queryMap);
}
```

**Pros:**
- ✅ **Clean JSON storage** - no `@class` fields, language-agnostic
- ✅ **Cross-language compatible** - any service can read cached JSON
- ✅ **Explicit type conversion** - uses `ParameterizedTypeReference` for accurate deserialization
- ✅ **No casting errors** - type is known at deserialization time
- ✅ **Full control** - manual cache put/get with explicit types
- ✅ **Flexible TTL** - can vary TTL per operation

**Cons:**
- ❌ More verbose - requires manual cache check/put logic
- ❌ More code - need to write cache helper and use it in each method
- ❌ Easy to forget - must remember to add caching to new methods

**When to Use:**
- ✅ **Multi-language/microservices** - when cached data needs to be consumed by other services
- ✅ **Clean JSON requirement** - when you need language-agnostic cached data
- ✅ **Complex type scenarios** - when you need explicit type control (generics, wildcards)

---

## Technical Details

### Service-Level Cache Helper

The `ServiceLevelCacheHelper` provides:
1. **Type-safe get**: Uses `ParameterizedTypeReference` to convert `LinkedHashMap` to target type
2. **Clean JSON put**: Stores data without Java class metadata
3. **Key generation**: Consistent key generation matching `QueryMapKeyGenerator`
4. **TTL management**: Flexible time-to-live configuration

### How It Works

1. **Serialization (PUT)**:
   - Object → Clean JSON (no `@class` fields) → Redis
   - Uses `Jackson2JsonRedisSerializer` with `deactivateDefaultTyping()`

2. **Deserialization (GET)**:
   - Redis → Clean JSON → `LinkedHashMap`
   - `LinkedHashMap` → Target Type (using `ParameterizedTypeReference`)
   - Uses `ObjectMapper.convertValue()` for type conversion

3. **Type Safety**:
   - `ParameterizedTypeReference` preserves generic type information
   - `ObjectMapper.convertValue()` handles complex nested types
   - No runtime casting errors

---

## Recommendation

**Use Service-Level Caching** when:
- ✅ You need clean JSON for cross-language compatibility
- ✅ Multiple services will consume cached data
- ✅ You need explicit control over type conversion
- ✅ You're building microservices or multi-language systems

**Use Controller-Level Caching** when:
- ✅ Single-language Java application
- ✅ You can accept Java-specific metadata
- ✅ You want simple, declarative caching
- ✅ No cross-service requirements

---

## Migration Path

1. **Create `ServiceLevelCacheHelper`** ✅
2. **Update service methods** to use cache helper ✅
3. **Remove `@Cacheable` from controllers** ✅
4. **Test with clean JSON** - verify other services can read cached data
5. **Update other service methods** (filterList, searchOne, etc.) as needed

---

## Example: Reading Cached Data from Other Services

**Python Example:**
```python
import redis
import json

r = redis.Redis(host='localhost', port=6379, db=0)
cached_data = r.get("AdministrativeAreas::searchList:type=REGION")
result = json.loads(cached_data)  # Clean JSON, no @class fields!
```

**Node.js Example:**
```javascript
const redis = require('redis');
const client = redis.createClient();

const cachedData = await client.get("AdministrativeAreas::searchList:type=REGION");
const result = JSON.parse(cachedData);  // Clean JSON!
```

**Java (Other Service):**
```java
String cached = redisTemplate.opsForValue().get("AdministrativeAreas::searchList:type=REGION");
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> result = mapper.readValue(cached, Map.class);  // Clean JSON!
```

All services can read the same cached JSON seamlessly! 🎉
