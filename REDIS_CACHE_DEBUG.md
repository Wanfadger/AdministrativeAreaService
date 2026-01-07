# Redis Cache Debugging Guide

## Issue
Cache keys are not visible in Redis Insight after calling the endpoint:
`http://localhost:8084/AdministrativeAreas/improved/searchList?type=REGION`

## Spring Cache Redis Key Format

Spring Cache Redis stores keys in the format: `cacheName::key`

For your endpoint:
- Cache name: `AdministrativeAreas` (from `CacheKeys.ADMINISTRATIVE_AREAS`)
- Key generator: `queryMapKeyGenerator` 
- Generated key: `searchList:type=REGION`
- **Full Redis key**: `AdministrativeAreas::searchList:type=REGION`

## Debugging Steps

### 1. Check if the endpoint is working
```bash
curl "http://localhost:8084/AdministrativeAreas/improved/searchList?type=REGION"
```

### 2. Check cache diagnostics endpoint
```bash
# List all cache keys
curl "http://localhost:8084/cache-diagnostics/keys"

# Get Redis connection info
curl "http://localhost:8084/cache-diagnostics/info"
```

### 3. Check Redis directly
```bash
# Connect to Redis
redis-cli -h localhost -p 8101

# List all keys (Spring Cache format uses ::)
KEYS *

# List keys matching cache name
KEYS AdministrativeAreas*

# List keys with double colon (Spring Cache separator)
KEYS *::*

# Get a specific key value
GET "AdministrativeAreas::searchList:type=REGION"
```

### 4. Check cache condition

The cache condition is: `#result != null && #result.status == true`

Verify the response has `status: true`:
```bash
curl "http://localhost:8084/AdministrativeAreas/improved/searchList?type=REGION" | jq '.status'
```

### 5. Enable cache logging

Add to `application-dev1.properties`:
```properties
logging.level.org.springframework.cache=DEBUG
logging.level.org.springframework.data.redis=DEBUG
```

### 6. Common Issues

1. **Cache condition not met**: Response status is false or null
2. **Wrong Redis database**: Check `spring.data.redis.database` (default is 0)
3. **Key format mismatch**: Spring Cache uses `::` separator
4. **TTL expired**: Keys might have expired (check TTL in CacheConfig)
5. **Cache not enabled**: Verify `@EnableCaching` is present

### 7. Verify in Redis Insight

In Redis Insight, search for:
- `AdministrativeAreas*` (all keys in this cache)
- `*::*` (all Spring Cache keys)
- `searchList*` (all searchList keys)

## Expected Behavior

1. First call: Cache miss → Query database → Store in Redis
2. Second call: Cache hit → Return from Redis (no database query)
3. Keys visible in Redis Insight with format: `AdministrativeAreas::searchList:type=REGION`

## Cache Configuration

- **Cache Name**: `AdministrativeAreas`
- **Cache Manager**: `hourCacheManager` (1 hour TTL)
- **Key Generator**: `queryMapKeyGenerator`
- **Condition**: `#result != null && #result.status == true`
