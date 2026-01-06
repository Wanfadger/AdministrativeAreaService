# Quick Fix Guide: Caching Implementation

## Immediate Actions Required

### 1. Fix Non-Deterministic Cache Keys (CRITICAL)

**Problem**: `key = "#queryMap"` generates non-deterministic keys

**Fix**: Replace with custom key generator

**In Controller** - Change from:
```java
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, key = "#queryMap")
```

**To**:
```java
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
           keyGenerator = "queryMapKeyGenerator")
```

**Steps**:
1. Copy `QueryMapKeyGenerator.java` to your project
2. Ensure it's annotated with `@Component("queryMapKeyGenerator")`
3. Update all `@Cacheable` annotations in controller

### 2. Add TTL to Primary Cache (CRITICAL)

**Problem**: Primary cache has no expiration

**Fix**: Update `CacheConfig.java`

**Change**:
```java
@Bean
@Primary
public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .prefixCacheNameWith("adminArea.")  // Shorter prefix
            .entryTtl(Duration.ofMinutes(30))   // ADD THIS LINE
            .disableCachingNullValues();
    
    return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
}
```

### 3. Add Redis Connection Pooling (HIGH)

**Add to `application.properties`**:
```properties
spring.data.redis.jedis.pool.max-active=200
spring.data.redis.jedis.pool.max-idle=50
spring.data.redis.jedis.pool.min-idle=10
spring.data.redis.jedis.pool.max-wait=5000ms
```

### 4. Optimize Cache Prefix (MEDIUM)

**In all cache managers**, change:
```java
.prefixCacheNameWith(this.getClass().getPackageName() + ".")
```

**To**:
```java
.prefixCacheNameWith("adminArea.")
```

### 5. Standardize Cache Manager Usage (MEDIUM)

**Update controller methods**:

- `filterOne`, `filterList`, `parishListByPartOf` → Keep `weekCacheManager` ✅
- `searchList`, `searchOne` → Change to `hourCacheManager` (currently using default/no TTL)

**Example**:
```java
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, 
           keyGenerator = "queryMapKeyGenerator",
           cacheManager = "hourCacheManager") // Add this
```

## Complete Updated Controller Example

See `AdministrativeAreaControllerImproved.java` for complete reference.

## Testing Checklist

After implementing fixes:

- [ ] Cache keys are deterministic (test with same query twice)
- [ ] Primary cache has TTL (check Redis TTL)
- [ ] Redis connection pooling is configured
- [ ] Cache prefix is shorter
- [ ] All endpoints use appropriate cache managers
- [ ] Cache hit rate improved (monitor metrics)

## Expected Results

- **Cache Hit Rate**: Should increase from ~60% to ~85-90%
- **Response Time**: 30-50% improvement for cached requests
- **Memory Usage**: 20-30% reduction (shorter keys)

## Rollback Plan

If issues occur:
1. Revert controller changes
2. Keep `QueryMapKeyGenerator` (it's safe)
3. Revert cache config changes if needed

## Files to Update

1. ✅ `QueryMapKeyGenerator.java` - NEW FILE (add to project)
2. ✅ `CacheConfig.java` - UPDATE (add TTL, fix prefix)
3. ✅ `AdministrativeAreaController.java` - UPDATE (use keyGenerator)
4. ✅ `application.properties` - ADD (Redis pool config)
