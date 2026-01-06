# Quick Implementation Guide

This guide provides step-by-step instructions for implementing the scalability improvements.

## Phase 1: Critical Improvements (Do First)

### Step 1: Add Database Indexes

**Option A: Using Flyway/Liquibase**
1. Add Flyway dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

2. Place the migration script at: `src/main/resources/db/migration/V1__add_performance_indexes.sql`

**Option B: Manual SQL Execution**
1. Connect to your PostgreSQL database
2. Execute the SQL script: `src/main/resources/db/migration/V1__add_performance_indexes.sql`

**Option C: Using JPA @Index Annotations**
Add `@Index` annotations to entity classes (requires schema update).

### Step 2: Update Application Properties

Add these optimizations to your `application.properties`:

```properties
# HikariCP Optimization
spring.datasource.hikari.maximum-pool-size=100
spring.datasource.hikari.minimum-idle=20

# Batch Processing
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Server Compression
server.compression.enabled=true
server.compression.mime-types=application/json,application/xml
server.compression.min-response-size=1024
```

### Step 3: Improve Cache Configuration

**Option A: Use Improved Cache Config (Recommended)**
1. Rename or backup existing `CacheConfig.java`
2. Rename `ImprovedCacheConfig.java` to `CacheConfig.java`
3. Update imports if needed

**Option B: Update Existing Cache Config**
1. Add connection pooling to `jedisConnectionFactory()` method
2. Add custom key generator
3. Update cache key prefixes

### Step 4: Add Async Configuration

1. The `AsyncConfig.java` is already created
2. Ensure `@EnableAsync` is present in your main application class (already present)
3. The async executor will be automatically used

## Phase 2: Code Improvements

### Step 1: Implement Granular Cache Eviction

Update `AdministrativeAreaController.java`:

**Before:**
```java
@CacheEvict(value = {CacheKeys.ADMINISTRATIVE_AREAS, CacheKeys.ADMINISTRATIVE_AREAS_FILTER}, allEntries = true)
```

**After (Example):**
```java
@CacheEvict(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
            key = "#dto.code", 
            condition = "#result != null")
```

**Note**: This requires careful implementation based on your cache key structure.

### Step 2: Use Cache Key Utils

Update controller methods to use `CacheKeyUtils`:

```java
import com.wanfadger.AdministrativeareaApi.shared.util.CacheKeyUtils;

@Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
           key = "T(com.wanfadger.AdministrativeareaApi.shared.util.CacheKeyUtils).generateCacheKey(#queryMap)")
public AdministrativeAreaResponseDto<CodeNameDto> filterOne(@RequestParam Map<String, String> queryMap) {
    // ...
}
```

### Step 3: Add Pagination (Future Enhancement)

For list endpoints, consider adding pagination:

```java
@GetMapping("/filterList")
public AdministrativeAreaResponseDto<Page<CodeNameDto>> filterList(
    @RequestParam Map<String, String> queryMap,
    @PageableDefault(size = 50) Pageable pageable) {
    // Implementation
}
```

## Phase 3: Infrastructure Setup

### Step 1: Add Monitoring

1. Add Spring Boot Actuator dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

2. Add to `application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

3. Access metrics at: `http://localhost:8084/actuator/metrics`

### Step 2: Configure Load Balancer

If deploying multiple instances:

1. **NGINX Configuration**:
```nginx
upstream admin_area_api {
    least_conn;
    server app1:8084;
    server app2:8084;
    server app3:8084;
}

server {
    listen 80;
    location / {
        proxy_pass http://admin_area_api;
    }
}
```

2. **Docker Compose** (add to existing):
```yaml
nginx:
  image: nginx:alpine
  ports:
    - "80:80"
  volumes:
    - ./nginx.conf:/etc/nginx/nginx.conf
  depends_on:
    - api
```

### Step 3: Redis Clustering (Optional)

For high availability, set up Redis Sentinel or Cluster:

1. **Redis Sentinel** (recommended for most cases)
2. **Redis Cluster** (for very high scale)

## Testing

### Load Testing

1. **Install k6** (or use JMeter/Gatling):
```bash
# macOS
brew install k6

# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

2. **Create test script** (`load-test.js`):
```javascript
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 100 },  // Ramp up to 100 users
    { duration: '1m', target: 100 },   // Stay at 100 users
    { duration: '30s', target: 200 },  // Ramp up to 200 users
    { duration: '1m', target: 200 },   // Stay at 200 users
    { duration: '30s', target: 0 },    // Ramp down
  ],
};

export default function () {
  const res = http.get('http://localhost:8084/AdministrativeAreas/filterList?type=REGION');
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}
```

3. **Run test**:
```bash
k6 run load-test.js
```

### Performance Baseline

Before implementing changes, record baseline metrics:
- Average response time
- Requests per second
- Error rate
- Database connection pool usage
- Cache hit rate

After implementing changes, compare metrics.

## Monitoring

### Key Metrics to Monitor

1. **Application Metrics** (`/actuator/metrics`):
   - `http.server.requests` - Request latency and count
   - `jvm.memory.used` - Memory usage
   - `hikari.connections.active` - Active database connections
   - `cache.gets` - Cache operations

2. **Database Metrics**:
   - Query execution time
   - Connection pool usage
   - Slow queries

3. **Redis Metrics**:
   - Hit/miss ratio
   - Memory usage
   - Connection count

## Rollback Plan

If issues occur after deployment:

1. **Database Indexes**: Can be dropped if causing issues:
```sql
DROP INDEX IF EXISTS idx_region_code;
-- Repeat for other indexes
```

2. **Configuration**: Revert `application.properties` changes

3. **Code Changes**: Revert to previous version using Git

## Verification Checklist

After implementation, verify:

- [ ] Database indexes are created (check with `\d+ table_name` in psql)
- [ ] Connection pool is configured correctly (check logs)
- [ ] Cache is working (check Redis)
- [ ] Async processing is working (check thread names in logs)
- [ ] Metrics endpoint is accessible
- [ ] Load testing shows improvement
- [ ] No errors in application logs

## Support

For issues or questions:
1. Check application logs
2. Review metrics at `/actuator/metrics`
3. Check database slow query log
4. Review Redis connection status

## Next Steps

After Phase 1-3:
1. Monitor performance for 1-2 weeks
2. Analyze metrics and identify bottlenecks
3. Implement Phase 4 optimizations as needed
4. Consider read replicas if read load is high
5. Implement rate limiting if needed
