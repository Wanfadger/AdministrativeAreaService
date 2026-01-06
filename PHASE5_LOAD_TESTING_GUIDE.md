# Phase 5: Load Testing & Optimization Guide

## Overview

This guide provides comprehensive instructions for load testing the Administrative Area API and optimizing based on the results.

## Prerequisites

1. Application running and accessible
2. Monitoring endpoints available (`/actuator/metrics`, `/actuator/prometheus`)
3. Redis and PostgreSQL running (via docker-compose)
4. Load testing tool installed (k6, Apache Bench, or curl)

## Key Metrics to Monitor

### 1. Application Metrics (via Actuator)

**HTTP Server Metrics**:
```bash
curl http://localhost:8084/actuator/metrics/http.server.requests
```

Key metrics:
- `http.server.requests` - Total request count
- `http.server.requests.duration` - Response time (p50, p95, p99)
- `http.server.requests.active` - Active requests

**Cache Metrics**:
```bash
curl http://localhost:8084/actuator/metrics/cache.gets
curl http://localhost:8084/actuator/metrics/cache.puts
curl http://localhost:8084/actuator/metrics/cache.evictions
```

Key metrics:
- `cache.gets` - Cache hit count
- `cache.misses` - Cache miss count
- Cache hit rate = `cache.gets / (cache.gets + cache.misses)`

**Database Connection Pool Metrics**:
```bash
curl http://localhost:8084/actuator/metrics/hikari.connections.active
curl http://localhost:8084/actuator/metrics/hikari.connections.idle
curl http://localhost:8084/actuator/metrics/hikari.connections.pending
```

**JVM Metrics**:
```bash
curl http://localhost:8084/actuator/metrics/jvm.memory.used
curl http://localhost:8084/actuator/metrics/jvm.gc.pause
```

### 2. Prometheus Metrics

Access Prometheus-formatted metrics:
```bash
curl http://localhost:8084/actuator/prometheus | grep -E "(http_server_requests|cache_|hikari_)"
```

## Load Testing Tools

### Option 1: k6 (Recommended)

**Installation**:
```bash
# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# macOS
brew install k6

# Or download from https://k6.io/docs/getting-started/installation/
```

**Run Baseline Test**:
```bash
cd load-tests
k6 run k6-baseline-test.js
```

**Customize Test**:
```bash
# Set custom base URL
k6 run --env BASE_URL=http://your-server:8084 k6-baseline-test.js

# Increase load
# Edit k6-baseline-test.js and modify stages
```

### Option 2: Apache Bench (ab)

**Installation**:
```bash
# Ubuntu/Debian
sudo apt-get install apache2-utils

# macOS (pre-installed)
```

**Run Test**:
```bash
# Test filterList endpoint
ab -n 1000 -c 50 http://localhost:8084/AdministrativeAreas/filterList?type=REGION

# Test with more concurrency
ab -n 5000 -c 100 http://localhost:8084/AdministrativeAreas/filterList?type=REGION
```

### Option 3: curl Script

**Run Simple Load Test**:
```bash
chmod +x load-tests/curl-load-test.sh
./load-tests/curl-load-test.sh http://localhost:8084 10 100
```

## Test Scenarios

### Scenario 1: Baseline Performance

**Goal**: Establish baseline metrics before optimization

**Configuration**:
- Duration: 5 minutes
- Ramp up: 10 → 50 → 100 users
- Endpoints: All GET endpoints

**Expected Results**:
- p95 response time < 500ms
- p99 response time < 1000ms
- Error rate < 1%
- Cache hit rate > 80% (after warm-up)

### Scenario 2: Sustained Load

**Goal**: Test performance under sustained load

**Configuration**:
- Duration: 30 minutes
- Constant load: 100 concurrent users
- Endpoints: Mix of all endpoints

**Expected Results**:
- No memory leaks
- Stable response times
- Connection pool utilization < 80%

### Scenario 3: Spike Test

**Goal**: Test behavior under sudden load increase

**Configuration**:
- Duration: 10 minutes
- Load pattern: 50 → 500 → 50 users (sudden spikes)
- Endpoints: Most common endpoints

**Expected Results**:
- Graceful degradation
- No crashes
- Recovery after spike

### Scenario 4: Stress Test

**Goal**: Find breaking point

**Configuration**:
- Gradually increase load until errors occur
- Monitor at each level
- Endpoints: All endpoints

**Expected Results**:
- Identify maximum capacity
- Document failure points
- Plan scaling strategy

## Monitoring During Tests

### 1. Real-time Monitoring

**Health Check**:
```bash
watch -n 1 'curl -s http://localhost:8084/actuator/health | jq'
```

**Metrics Dashboard** (if using Prometheus + Grafana):
- Set up Prometheus to scrape `/actuator/prometheus`
- Create Grafana dashboard with key metrics

### 2. Key Metrics to Watch

**During Load Test**:
1. **Response Times**: Should remain stable
   - p50 < 200ms
   - p95 < 500ms
   - p99 < 1000ms

2. **Error Rate**: Should be < 1%
   ```bash
   curl -s http://localhost:8084/actuator/metrics/http.server.requests | grep "status=\"5"
   ```

3. **Cache Hit Rate**: Should be > 80% after warm-up
   ```bash
   # Calculate: cache.gets / (cache.gets + cache.misses)
   ```

4. **Database Connection Pool**: Should not be exhausted
   ```bash
   curl -s http://localhost:8084/actuator/metrics/hikari.connections.active
   ```

5. **Memory Usage**: Should be stable (no leaks)
   ```bash
   curl -s http://localhost:8084/actuator/metrics/jvm.memory.used
   ```

## Optimization Based on Results

### If Response Times Are High

1. **Check Cache Hit Rate**:
   - If < 70%: Review cache TTLs and keys
   - Verify cache warming is working
   - Check for cache eviction patterns

2. **Check Database Queries**:
   - Review slow query logs
   - Verify indexes are being used
   - Check connection pool utilization

3. **Check Application Logs**:
   - Look for errors or warnings
   - Check for N+1 query problems
   - Verify batch processing is working

### If Error Rate Is High

1. **Check Connection Pool**:
   - Increase `spring.datasource.hikari.maximum-pool-size` if needed
   - Check for connection leaks

2. **Check Redis Connection**:
   - Verify Redis is accessible
   - Check Redis connection pool settings
   - Monitor Redis memory usage

3. **Check Application Resources**:
   - Monitor JVM memory
   - Check CPU usage
   - Review thread pool utilization

### If Cache Hit Rate Is Low

1. **Review Cache Configuration**:
   - Verify cache keys are deterministic
   - Check cache conditions are correct
   - Review cache TTLs

2. **Check Cache Warming**:
   - Verify cache warmer is running
   - Add more common queries to cache warmer

3. **Review Eviction Patterns**:
   - Check if eviction is too aggressive
   - Review write patterns

## Performance Targets

### Response Time Targets

| Endpoint | p50 | p95 | p99 |
|----------|-----|-----|-----|
| filterOne | < 100ms | < 300ms | < 500ms |
| filterList | < 200ms | < 500ms | < 1000ms |
| searchList | < 300ms | < 800ms | < 1500ms |
| searchOne | < 150ms | < 400ms | < 800ms |

### Throughput Targets

- **Minimum**: 100 requests/second
- **Target**: 500 requests/second
- **Peak**: 1000+ requests/second

### Resource Utilization Targets

- **Database Connection Pool**: < 80% utilization
- **Redis Connection Pool**: < 80% utilization
- **JVM Heap**: < 80% utilization
- **CPU**: < 70% utilization

## Post-Test Analysis

### 1. Collect Metrics

```bash
# Export all metrics
curl http://localhost:8084/actuator/prometheus > metrics-export.txt

# Export specific metrics
curl http://localhost:8084/actuator/metrics/http.server.requests > http-metrics.json
curl http://localhost:8084/actuator/metrics/cache.gets > cache-metrics.json
```

### 2. Analyze Results

Compare:
- Response times (before vs after)
- Error rates
- Cache hit rates
- Resource utilization

### 3. Document Findings

Create a report with:
- Test configuration
- Results summary
- Bottlenecks identified
- Optimization recommendations
- Next steps

## Continuous Monitoring

### Production Monitoring

Set up alerts for:
- Response time p95 > 1000ms
- Error rate > 1%
- Cache hit rate < 70%
- Connection pool utilization > 90%
- Memory usage > 85%

### Regular Load Testing

Schedule regular load tests:
- Weekly: Baseline performance check
- Monthly: Full load test suite
- After deployments: Quick smoke test
- Before major releases: Comprehensive stress test

## Troubleshooting

### Common Issues

1. **High Response Times**:
   - Check database indexes
   - Verify cache is working
   - Review query performance

2. **High Error Rate**:
   - Check application logs
   - Verify database connectivity
   - Check Redis connectivity

3. **Low Cache Hit Rate**:
   - Verify cache keys are correct
   - Check cache eviction patterns
   - Review cache TTLs

4. **Connection Pool Exhaustion**:
   - Increase pool size
   - Check for connection leaks
   - Review connection timeout settings

## Next Steps

After completing load testing:

1. **Document Results**: Create performance baseline report
2. **Implement Optimizations**: Based on test results
3. **Re-test**: Verify improvements
4. **Set Up Monitoring**: Configure production alerts
5. **Plan Scaling**: Based on capacity findings

---

**Status**: Phase 5 Documentation Complete  
**Ready for**: Load Testing Execution
