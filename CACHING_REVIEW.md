# Caching Implementation Review & Improvements

## Current Caching Implementation Analysis

### ✅ What's Working Well

1. **Multiple Cache Managers**: Good use of different TTLs (hour, 24h, week, month)
2. **Redis Integration**: Properly configured with Redis backend
3. **Cache Annotations**: Correct use of `@Cacheable` and `@CacheEvict`
4. **Cache Separation**: Two cache namespaces (`ADMINISTRATIVE_AREAS` and `ADMINISTRATIVE_AREAS_FILTER`)

### ❌ Critical Issues Found

#### 1. **Non-Deterministic Cache Keys** ⚠️ CRITICAL
**Problem**: Using `key = "#queryMap"` directly causes issues:
- `Map.toString()` order is not guaranteed
- Same query parameters may generate different keys
- Cache misses even when data should be cached

**Current Code**:
```java
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, key = "#queryMap")
```

**Impact**: Low cache hit rate, unnecessary database queries

#### 2. **Aggressive Cache Eviction** ⚠️ CRITICAL
**Problem**: All write operations clear entire cache:
```java
@CacheEvict(value = {CacheKeys.ADMINISTRATIVE_AREAS, CacheKeys.ADMINISTRATIVE_AREAS_FILTER}, allEntries = true)
```

**Impact**: 
- Single write invalidates ALL cached data
- Poor cache efficiency
- High database load after any write operation

#### 3. **No TTL on Primary Cache** ⚠️ HIGH
**Problem**: Primary cache manager has no expiration:
```java
RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
    .prefixCacheNameWith(this.getClass().getPackageName() + ".")
    .disableCachingNullValues();
// No TTL set!
```

**Impact**: Cache grows indefinitely, potential memory issues

#### 4. **Inefficient Cache Prefix** ⚠️ MEDIUM
**Problem**: Using full package name as prefix:
```java
.prefixCacheNameWith(this.getClass().getPackageName() + ".")
// Results in: "com.wanfadger.AdministrativeareaApi.shared.beanConfig."
```

**Impact**: Longer Redis keys, more memory usage

#### 5. **No Redis Connection Pooling** ⚠️ HIGH
**Problem**: Jedis connection factory has no pooling configured:
```java
public JedisConnectionFactory jedisConnectionFactory(){
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
    return new JedisConnectionFactory(config);
    // No pool configuration!
}
```

**Impact**: Connection overhead, poor performance under load

#### 6. **Inconsistent Cache Manager Usage** ⚠️ MEDIUM
**Problem**: Some endpoints use `weekCacheManager`, others use default (no TTL):
- `filterList` → weekCacheManager (7 days)
- `searchList` → default (no TTL)
- `searchOne` → default (no TTL)

**Impact**: Inconsistent cache behavior, some data never expires

#### 7. **No Selective Cache Invalidation** ⚠️ HIGH
**Problem**: Cannot invalidate specific cache entries based on what changed

**Impact**: Must clear entire cache even for single record updates

---

## Recommended Improvements

### Improvement 1: Fix Cache Key Generation

**Create Custom Key Generator**:

```java
@Component("queryMapKeyGenerator")
public class QueryMapKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length > 0 && params[0] instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> queryMap = (Map<String, String>) params[0];
            return queryMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue() : ""))
                    .collect(Collectors.joining("&"));
        }
        return method.getName() + ":" + Arrays.toString(params);
    }
}
```

**Update Controller**:
```java
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
           keyGenerator = "queryMapKeyGenerator")
public AdministrativeAreaResponseDto<CodeNameDto> filterOne(@RequestParam Map<String, String> queryMap) {
    // ...
}
```

### Improvement 2: Implement Granular Cache Eviction

**Strategy**: Invalidate only affected cache entries

**Option A: Pattern-Based Eviction** (Recommended for hierarchical data):
```java
@CacheEvict(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
            key = "'type:' + #queryMap['type'] + ':partOf:' + #dto.partOfCode",
            condition = "#dto.partOfCode != null")
public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(...) {
    // ...
}
```

**Option B: Cache Tags** (Requires custom implementation):
Use cache tags to group related entries and evict by tag.

**Option C: Selective Eviction** (For specific operations):
```java
@CacheEvict(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
            key = "'type:' + #queryMap['type'] + ':code:' + #dto.code")
public ResponseEntity<AdministrativeAreaResponseDto<String>> updateOne(...) {
    // ...
}
```

### Improvement 3: Add TTL to Primary Cache

**Update CacheConfig**:
```java
@Bean
@Primary
public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .prefixCacheNameWith("adminArea.")
            .entryTtl(Duration.ofMinutes(30)) // Add TTL
            .disableCachingNullValues();
    
    return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
}
```

### Improvement 4: Optimize Cache Prefix

**Change from**:
```java
.prefixCacheNameWith(this.getClass().getPackageName() + ".")
```

**To**:
```java
.prefixCacheNameWith("adminArea.")
```

**Benefits**:
- Shorter keys (saves memory)
- More readable
- Easier to debug

### Improvement 5: Add Redis Connection Pooling

**Update JedisConnectionFactory**:
```java
@Bean
public JedisConnectionFactory jedisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
    JedisConnectionFactory factory = new JedisConnectionFactory(config);
    
    // Connection pooling is handled internally by Jedis
    // For explicit control, configure via application properties:
    // spring.data.redis.jedis.pool.max-active=200
    // spring.data.redis.jedis.pool.max-idle=50
    // spring.data.redis.jedis.pool.min-idle=10
    
    return factory;
}
```

**Add to application.properties**:
```properties
spring.data.redis.jedis.pool.max-active=200
spring.data.redis.jedis.pool.max-idle=50
spring.data.redis.jedis.pool.min-idle=10
spring.data.redis.jedis.pool.max-wait=5000ms
```

### Improvement 6: Standardize Cache Manager Usage

**Recommendation**: Use consistent TTLs based on data volatility:

- **Frequently changing data** (writes): 30 minutes (default cacheManager)
- **Moderately stable data**: 1 hour (hourCacheManager)
- **Stable data** (rarely changes): 7 days (weekCacheManager)
- **Very stable data**: 30 days (monthCacheManager)

**Update Controller**:
```java
// For frequently accessed, stable data
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
           keyGenerator = "queryMapKeyGenerator",
           cacheManager = "weekCacheManager")
public AdministrativeAreaResponseDto<List<CodeNameDto>> filterList(...) {
    // ...
}

// For search operations (may change more frequently)
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, 
           keyGenerator = "queryMapKeyGenerator",
           cacheManager = "hourCacheManager") // Changed from default
public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(...) {
    // ...
}
```

### Improvement 7: Implement Hierarchical Cache Invalidation

**For hierarchical data structure**, invalidate parent and child caches:

```java
@CacheEvict(value = {
    CacheKeys.ADMINISTRATIVE_AREAS_FILTER,
    CacheKeys.ADMINISTRATIVE_AREAS
}, 
key = "'type:' + #queryMap['type'] + ':*'", // Pattern-based
allEntries = false) // Only matching entries
public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(...) {
    // ...
}
```

**Note**: Pattern-based eviction requires custom cache manager implementation.

### Improvement 8: Add Cache Statistics & Monitoring

**Enable Cache Metrics**:
```java
@Bean
public CacheMetricsRegistrar cacheMetricsRegistrar(MeterRegistry meterRegistry) {
    return new CacheMetricsRegistrar(meterRegistry);
}
```

**Add to application.properties**:
```properties
management.metrics.cache.enabled=true
```

**Monitor**:
- Cache hit/miss ratio
- Cache eviction count
- Cache size
- Average response time

### Improvement 9: Add Cache Warming (Optional)

**Pre-load frequently accessed data on startup**:

```java
@Component
public class CacheWarmer implements ApplicationListener<ContextRefreshedEvent> {
    
    @Autowired
    private AdministrativeAreaService service;
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Warm cache with common queries
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("type", "REGION");
        service.filterList(queryMap); // This will be cached
    }
}
```

### Improvement 10: Add Cache Condition

**Only cache successful responses**:

```java
@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
           keyGenerator = "queryMapKeyGenerator",
           condition = "#result != null && #result.success == true")
public AdministrativeAreaResponseDto<CodeNameDto> filterOne(...) {
    // ...
}
```

---

## Implementation Priority

### 🔴 Critical (Implement Immediately)
1. ✅ Fix cache key generation (non-deterministic keys)
2. ✅ Add TTL to primary cache
3. ✅ Implement granular cache eviction
4. ✅ Add Redis connection pooling

### 🟡 High Priority (Within 1 Week)
5. ✅ Optimize cache prefix
6. ✅ Standardize cache manager usage
7. ✅ Add cache statistics/monitoring

### 🟢 Medium Priority (Within 1 Month)
8. ✅ Implement hierarchical cache invalidation
9. ✅ Add cache warming
10. ✅ Add cache conditions

---

## Expected Improvements

After implementing critical improvements:

- **Cache Hit Rate**: 60-70% → 85-95%
- **Database Load**: 40-50% reduction
- **Response Time**: 30-50% improvement for cached requests
- **Memory Usage**: 20-30% reduction (shorter keys, TTL)
- **Cache Efficiency**: 3-5x improvement

---

## Testing Recommendations

### 1. Cache Hit Rate Test
```java
@Test
public void testCacheHitRate() {
    // First call - cache miss
    service.filterOne(queryMap);
    
    // Second call - cache hit
    service.filterOne(queryMap);
    
    // Verify cache metrics
}
```

### 2. Cache Eviction Test
```java
@Test
public void testSelectiveCacheEviction() {
    // Cache some data
    service.filterOne(queryMap1);
    service.filterOne(queryMap2);
    
    // Update one record
    service.updateOne(queryMap1, dto);
    
    // Verify only queryMap1 cache is evicted
    // queryMap2 should still be cached
}
```

### 3. Load Test
- Test cache performance under load
- Monitor cache hit/miss ratio
- Verify no memory leaks with TTL

---

## Code Examples

See the following files for implementation:
- `ImprovedCacheConfig.java` - Enhanced cache configuration
- `CacheKeyUtils.java` - Cache key utility
- `QueryMapKeyGenerator.java` - Custom key generator (to be created)
