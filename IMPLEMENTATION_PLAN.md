# Implementation Plan: Caching & Performance Improvements

## Overview
This document provides a step-by-step implementation plan for all recommended improvements, ordered by priority and dependencies. Follow this plan sequentially to minimize risk and ensure smooth deployment.

---

## Pre-Implementation Checklist

Before starting, ensure you have:
- [ ] Backup of current codebase (Git commit/push)
- [ ] Database backup
- [ ] Access to Redis for monitoring
- [ ] Test environment available
- [ ] Monitoring tools configured (Spring Actuator)
- [ ] Load testing tools ready (k6, JMeter, or similar)

---

## Phase 1: Critical Fixes (Week 1)

### Step 1.1: Add QueryMapKeyGenerator (Day 1)
**Priority**: 🔴 CRITICAL  
**Risk**: LOW  
**Time**: 30 minutes  
**Dependencies**: None

**Actions**:
1. Copy `QueryMapKeyGenerator.java` to your project:
   ```
   src/main/java/com/wanfadger/AdministrativeareaApi/shared/util/QueryMapKeyGenerator.java
   ```

2. Verify the file compiles:
   ```bash
   mvn clean compile
   ```

3. **Test**: Verify the key generator works:
   ```java
   // Create a simple test
   Map<String, String> map1 = Map.of("type", "REGION", "code", "123");
   Map<String, String> map2 = Map.of("code", "123", "type", "REGION");
   // Both should generate same key
   ```

**Verification**:
- ✅ Code compiles without errors
- ✅ Key generator generates deterministic keys

**Rollback**: Simply delete the file if issues occur.

---

### Step 1.2: Update CacheConfig - Add TTL to Primary Cache (Day 1)
**Priority**: 🔴 CRITICAL  
**Risk**: LOW  
**Time**: 15 minutes  
**Dependencies**: Step 1.1

**Actions**:
1. Open `CacheConfig.java`

2. Update the primary cache manager:
   ```java
   @Bean
   @Primary
   public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
       RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
               .prefixCacheNameWith("adminArea.")  // Change from package name
               .entryTtl(Duration.ofMinutes(30))   // ADD THIS LINE
               .disableCachingNullValues();
       
       return RedisCacheManager.builder(connectionFactory)
               .cacheDefaults(config)
               .build();
   }
   ```

3. Update all other cache managers' prefixes:
   - Change `this.getClass().getPackageName() + "."` to `"adminArea.hour."`, `"adminArea.week."`, etc.

**Verification**:
- ✅ Application starts without errors
- ✅ Check Redis: Keys should have TTL set
- ✅ Verify prefix is shorter: `adminArea.` instead of long package name

**Testing**:
```bash
# Connect to Redis
redis-cli

# Check keys
KEYS adminArea.*

# Check TTL
TTL adminArea.AdministrativeAreas:filterOne:type=REGION
```

**Rollback**: Revert `CacheConfig.java` changes.

---

### Step 1.3: Add Redis Connection Pooling (Day 1)
**Priority**: 🔴 CRITICAL  
**Risk**: LOW  
**Time**: 10 minutes  
**Dependencies**: None

**Actions**:
1. Open `application.properties` (or environment-specific file)

2. Add Redis connection pool configuration:
   ```properties
   # Redis Connection Pool
   spring.data.redis.jedis.pool.max-active=200
   spring.data.redis.jedis.pool.max-idle=50
   spring.data.redis.jedis.pool.min-idle=10
   spring.data.redis.jedis.pool.max-wait=5000ms
   ```

3. Restart application

**Verification**:
- ✅ Application starts successfully
- ✅ Monitor Redis connections (should see pool usage)
- ✅ No connection errors in logs

**Testing**:
- Run load test and monitor Redis connection count
- Should see improved performance under load

**Rollback**: Remove the properties and restart.

---

### Step 1.4: Update Controller - Use Key Generator (Day 2)
**Priority**: 🔴 CRITICAL  
**Risk**: MEDIUM  
**Time**: 30 minutes  
**Dependencies**: Steps 1.1, 1.2

**Actions**:
1. Open `AdministrativeAreaController.java`

2. Update all `@Cacheable` annotations one by one:

   **Before**:
   ```java
   @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, key = "#queryMap")
   ```

   **After**:
   ```java
   @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
              keyGenerator = "queryMapKeyGenerator")
   ```

3. Update endpoints in this order (test after each):
   - `filterOne` (line 51)
   - `filterList` (line 58)
   - `parishListByPartOf` (line 64)
   - `searchList` (line 71)
   - `searchOne` (line 79)

**Verification**:
- ✅ Application compiles
- ✅ Test each endpoint:
   ```bash
   # First call - should hit database
   curl "http://localhost:8084/AdministrativeAreas/filterOne?type=REGION&code=123"
   
   # Second call with same params (different order) - should hit cache
   curl "http://localhost:8084/AdministrativeAreas/filterOne?code=123&type=REGION"
   ```

**Testing Checklist**:
- [ ] Same query parameters generate same cache key
- [ ] Different query parameters generate different cache keys
- [ ] Cache hit rate improves

**Rollback**: Revert controller changes one endpoint at a time.

---

### Step 1.5: Add Cache Manager to Search Endpoints (Day 2)
**Priority**: 🔴 CRITICAL  
**Risk**: LOW  
**Time**: 15 minutes  
**Dependencies**: Step 1.4

**Actions**:
1. Update `searchList` endpoint:
   ```java
   @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, 
              keyGenerator = "queryMapKeyGenerator",
              cacheManager = "hourCacheManager")  // ADD THIS
   ```

2. Update `searchOne` endpoint:
   ```java
   @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, 
              keyGenerator = "queryMapKeyGenerator",
              cacheManager = "hourCacheManager")  // ADD THIS
   ```

**Verification**:
- ✅ Application starts
- ✅ Check Redis TTL on search endpoints (should be 1 hour)
- ✅ Verify cache expiration works

**Rollback**: Remove `cacheManager` attribute.

---

### Step 1.6: Test Phase 1 Changes (Day 3)
**Priority**: 🔴 CRITICAL  
**Risk**: N/A  
**Time**: 2-4 hours  
**Dependencies**: Steps 1.1-1.5

**Actions**:
1. **Functional Testing**:
   - Test all GET endpoints
   - Verify cache behavior
   - Test cache eviction on writes

2. **Performance Testing**:
   ```bash
   # Run load test
   k6 run load-test.js
   
   # Monitor:
   # - Cache hit rate (should be >80%)
   # - Response times (should improve)
   # - Database load (should decrease)
   ```

3. **Monitor Metrics**:
   - Check `/actuator/metrics/cache.gets`
   - Check `/actuator/metrics/cache.evictions`
   - Monitor Redis memory usage

**Success Criteria**:
- ✅ Cache hit rate > 80%
- ✅ Response time improved by 30-50% for cached requests
- ✅ No errors in logs
- ✅ All endpoints work correctly

**If Issues**:
- Review logs
- Check Redis connectivity
- Verify cache keys in Redis
- Rollback if critical issues

---

## Phase 2: Database Optimizations (Week 2)

### Step 2.1: Add Database Indexes (Day 1)
**Priority**: 🟡 HIGH  
**Risk**: LOW (read-only operation)  
**Time**: 1 hour  
**Dependencies**: Database access

**Actions**:
1. **Review Migration Script**:
   - Open `src/main/resources/db/migration/V1__add_performance_indexes.sql`
   - Review indexes for your schema

2. **Option A: Using Flyway** (Recommended):
   ```xml
   <!-- Add to pom.xml -->
   <dependency>
       <groupId>org.flywaydb</groupId>
       <artifactId>flyway-core</artifactId>
   </dependency>
   ```
   - Place migration script in `src/main/resources/db/migration/`
   - Flyway will run automatically on startup

3. **Option B: Manual Execution**:
   ```bash
   # Connect to database
   psql -U your_user -d your_database
   
   # Execute script
   \i src/main/resources/db/migration/V1__add_performance_indexes.sql
   ```

4. **Option C: Using JPA** (Not recommended for production):
   - Add `@Index` annotations to entities
   - Let Hibernate create indexes

**Verification**:
```sql
-- Check indexes were created
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename IN ('region', 'subregion', 'localgovernment', 'county', 'subcounty', 'parish');

-- Verify index usage
EXPLAIN ANALYZE SELECT * FROM region WHERE code = 'test-code';
-- Should show "Index Scan"
```

**Testing**:
- Run queries that should use indexes
- Monitor query execution time (should improve)
- Check database statistics

**Rollback**:
```sql
-- Drop indexes if needed
DROP INDEX IF EXISTS idx_region_code;
-- Repeat for all indexes
```

---

### Step 2.2: Optimize HikariCP Connection Pool (Day 2)
**Priority**: 🟡 HIGH  
**Risk**: LOW  
**Time**: 30 minutes  
**Dependencies**: Step 2.1

**Actions**:
1. Open `application.properties`

2. Update HikariCP settings:
   ```properties
   # HikariCP Connection Pool - Optimized
   spring.datasource.hikari.pool-name=SIIP-AREA-POOL
   spring.datasource.hikari.maximum-pool-size=100
   spring.datasource.hikari.minimum-idle=20
   spring.datasource.hikari.max-lifetime=1800000
   spring.datasource.hikari.connection-timeout=20000
   spring.datasource.hikari.idle-timeout=600000
   spring.datasource.hikari.leak-detection-threshold=60000
   spring.datasource.hikari.register-mbeans=true
   ```

3. Add batch processing (if not already present):
   ```properties
   spring.jpa.properties.hibernate.jdbc.batch_size=50
   spring.jpa.properties.hibernate.order_inserts=true
   spring.jpa.properties.hibernate.order_updates=true
   spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
   ```

**Verification**:
- ✅ Application starts
- ✅ Check connection pool metrics: `/actuator/metrics/hikari.connections.active`
- ✅ Monitor connection pool usage under load

**Testing**:
- Run load test
- Monitor connection pool metrics
- Verify no connection leaks

**Rollback**: Revert to previous values.

---

## Phase 3: Monitoring & Observability (Week 2-3)

### Step 3.1: Add Spring Boot Actuator (Day 1)
**Priority**: 🟡 HIGH  
**Risk**: LOW  
**Time**: 30 minutes  
**Dependencies**: None

**Actions**:
1. Check if actuator is already in `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   ```

2. If not present, add it.

3. Update `application.properties`:
   ```properties
   # Actuator Configuration
   management.endpoints.web.exposure.include=health,info,metrics,prometheus
   management.endpoint.health.show-details=when-authorized
   management.metrics.export.prometheus.enabled=true
   management.metrics.distribution.percentiles-histogram.http.server.requests=true
   management.metrics.distribution.sla.http.server.requests=100ms,500ms,1s,2s
   ```

**Verification**:
- ✅ Access `/actuator/health`
- ✅ Access `/actuator/metrics`
- ✅ Access `/actuator/prometheus`

**Testing**:
- Verify metrics are being collected
- Check cache metrics are available

---

### Step 3.2: Set Up Monitoring Dashboard (Day 2-3)
**Priority**: 🟢 MEDIUM  
**Risk**: LOW  
**Time**: 2-4 hours  
**Dependencies**: Step 3.1

**Actions**:
1. **Option A: Prometheus + Grafana**:
   - Set up Prometheus to scrape `/actuator/prometheus`
   - Create Grafana dashboard
   - Add panels for:
     - Cache hit/miss rate
     - Response times
     - Database connection pool
     - Request rate

2. **Option B: Spring Boot Admin**:
   - Set up Spring Boot Admin server
   - Register your application
   - Monitor metrics

**Verification**:
- ✅ Metrics are being collected
- ✅ Dashboards show data
- ✅ Alerts configured (optional)

---

## Phase 4: Advanced Optimizations (Week 3-4)

### Step 4.1: Implement Cache Conditions (Day 1)
**Priority**: 🟢 MEDIUM  
**Risk**: LOW  
**Time**: 30 minutes  
**Dependencies**: Phase 1 complete

**Actions**:
1. Update `@Cacheable` annotations to include conditions:
   ```java
   @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
              keyGenerator = "queryMapKeyGenerator",
              cacheManager = "weekCacheManager",
              condition = "#result != null && #result.success != null && #result.success == true")
   ```

2. Apply to all cacheable endpoints

**Verification**:
- ✅ Only successful responses are cached
- ✅ Error responses are not cached

---

### Step 4.2: Add Cache Warming (Optional) (Day 2)
**Priority**: 🟢 LOW  
**Risk**: LOW  
**Time**: 1 hour  
**Dependencies**: Phase 1 complete

**Actions**:
1. Create `CacheWarmer.java`:
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
           service.filterList(queryMap);
       }
   }
   ```

2. Add common queries to warm cache

**Verification**:
- ✅ Cache is populated on startup
- ✅ First requests hit cache

---

## Phase 5: Load Testing & Optimization (Week 4)

### Step 5.1: Baseline Performance Test (Day 1)
**Priority**: 🟡 HIGH  
**Risk**: N/A  
**Time**: 2-4 hours  
**Dependencies**: All phases complete

**Actions**:
1. Create load test script (k6 example):
   ```javascript
   import http from 'k6/http';
   import { check } from 'k6';

   export const options = {
     stages: [
       { duration: '1m', target: 50 },
       { duration: '3m', target: 100 },
       { duration: '2m', target: 0 },
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

2. Run baseline test
3. Record metrics:
   - Requests per second
   - Response times (p50, p95, p99)
   - Error rate
   - Cache hit rate
   - Database connection pool usage

---

### Step 5.2: Optimize Based on Results (Day 2-3)
**Priority**: 🟡 HIGH  
**Risk**: MEDIUM  
**Time**: 4-8 hours  
**Dependencies**: Step 5.1

**Actions**:
1. Analyze load test results
2. Identify bottlenecks
3. Adjust:
   - Cache TTLs
   - Connection pool sizes
   - Batch sizes
   - Indexes (if needed)

4. Re-test and compare

---

## Deployment Plan

### Pre-Deployment Checklist
- [ ] All Phase 1 changes tested
- [ ] Database indexes created
- [ ] Monitoring configured
- [ ] Load testing completed
- [ ] Rollback plan documented
- [ ] Team trained on changes

### Deployment Steps

1. **Deploy to Staging**:
   - Deploy Phase 1 changes
   - Monitor for 24-48 hours
   - Verify metrics

2. **Deploy to Production**:
   - Deploy during low-traffic window
   - Monitor closely
   - Have rollback ready

3. **Post-Deployment**:
   - Monitor metrics for 1 week
   - Compare with baseline
   - Document improvements

---

## Rollback Procedures

### Quick Rollback (If Critical Issues)

1. **Revert Controller Changes**:
   ```bash
   git checkout HEAD~1 -- src/main/java/.../AdministrativeAreaController.java
   ```

2. **Revert CacheConfig**:
   ```bash
   git checkout HEAD~1 -- src/main/java/.../CacheConfig.java
   ```

3. **Restart Application**

### Partial Rollback

- Keep `QueryMapKeyGenerator` (it's safe)
- Revert specific endpoints if needed
- Keep database indexes (they're beneficial)

---

## Success Metrics

After full implementation, expect:

- ✅ **Cache Hit Rate**: 85-95% (from 60-70%)
- ✅ **Response Time**: 30-50% improvement
- ✅ **Database Load**: 40-50% reduction
- ✅ **Throughput**: 3-5x increase
- ✅ **Memory Usage**: 20-30% reduction

---

## Timeline Summary

| Phase | Duration | Priority | Risk |
|-------|----------|----------|------|
| Phase 1: Critical Fixes | Week 1 | 🔴 CRITICAL | LOW |
| Phase 2: Database | Week 2 | 🟡 HIGH | LOW |
| Phase 3: Monitoring | Week 2-3 | 🟡 HIGH | LOW |
| Phase 4: Advanced | Week 3-4 | 🟢 MEDIUM | LOW |
| Phase 5: Testing | Week 4 | 🟡 HIGH | MEDIUM |

**Total Estimated Time**: 4 weeks (can be accelerated if needed)

---

## Support & Troubleshooting

### Common Issues

1. **Cache Not Working**:
   - Check Redis connectivity
   - Verify `@EnableCaching` is present
   - Check cache key generator is registered

2. **High Memory Usage**:
   - Check cache TTLs are set
   - Monitor Redis memory
   - Adjust TTLs if needed

3. **Poor Performance**:
   - Check database indexes
   - Verify connection pool settings
   - Monitor cache hit rate

### Getting Help

- Review logs: `application.log`
- Check metrics: `/actuator/metrics`
- Monitor Redis: `redis-cli MONITOR`
- Review database: Slow query log

---

## Next Steps

1. ✅ Review this plan with team
2. ✅ Set up test environment
3. ✅ Begin Phase 1, Step 1.1
4. ✅ Test incrementally
5. ✅ Document learnings

Good luck with the implementation! 🚀
