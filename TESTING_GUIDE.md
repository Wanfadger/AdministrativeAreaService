# Testing Guide - Swagger & Load Testing

## Prerequisites

Before testing, ensure:
1. ✅ PostgreSQL is running (port 5438 based on your config)
2. ✅ Redis is running (port 8101)
3. ✅ Application builds successfully
4. ✅ Database is accessible

## Part 1: Swagger Testing

### Step 1: Start the Application

```bash
# Build the application
mvn clean install

# Start the application with dev1 profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev1
```

Or if you have the JAR:
```bash
java -jar target/AdministrativeareaApi-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev1
```

### Step 2: Verify Application Started

Check logs for:
- ✅ Application started successfully
- ✅ Database connection established
- ✅ Redis connection established
- ✅ Swagger UI initialized

### Step 3: Access Swagger UI

Open your browser and navigate to:
```
http://localhost:8084/swagger-ui.html
```

Or try:
```
http://localhost:8084/swagger-ui/index.html
```

### Step 4: Test Endpoints in Swagger UI

#### Test 1: Get List of Regions (Most Common)

1. Find `GET /AdministrativeAreas/filterList`
2. Click "Try it out"
3. Add query parameter:
   - Parameter: `type`
   - Value: `REGION`
4. Click "Execute"
5. **Expected**: 
   - Status: 200 OK
   - Response body with list of regions (code and name)
   - Response should be cached (first call hits DB, second call hits cache)

#### Test 2: Get Single Region

1. Find `GET /AdministrativeAreas/filterOne`
2. Click "Try it out"
3. Add query parameters:
   - `type`: `REGION`
   - `code`: `001` (or any valid region code from Test 1)
4. Click "Execute"
5. **Expected**: Single region object

#### Test 3: Search List (Full Details)

1. Find `GET /AdministrativeAreas/searchList`
2. Click "Try it out"
3. Add query parameter:
   - `type`: `REGION`
4. Click "Execute"
5. **Expected**: List with full details (code, name, latitude, longitude)

#### Test 4: Create New Administrative Area

1. Find `POST /AdministrativeAreas/one`
2. Click "Try it out"
3. Add query parameter:
   - `type`: `REGION`
4. Enter request body:
   ```json
   {
     "name": "Test Region",
     "description": "Test Region Description",
     "latitude": "0.3476",
     "longitude": "32.5825"
   }
   ```
5. Click "Execute"
6. **Expected**: Success response
7. **Note**: Cache will be evicted after this operation

#### Test 5: Verify Cache Eviction

1. After creating a new area, immediately call `GET /AdministrativeAreas/filterList?type=REGION`
2. **Expected**: New area should appear (cache was evicted)

### Step 5: Verify Swagger Documentation

Check that:
- ✅ All endpoints are visible
- ✅ Request/response examples are shown
- ✅ Query parameters are documented
- ✅ Response schemas are correct
- ✅ Try it out functionality works

## Part 2: Load Testing

### Prerequisites for Load Testing

1. Application must be running
2. Some data should exist in database (run Swagger tests first)
3. Choose a load testing tool:
   - **k6** (recommended) - Install from https://k6.io/docs/getting-started/installation/
   - **curl script** - No installation needed
   - **Apache Bench (ab)** - Usually pre-installed

### Option A: Using k6 (Recommended)

#### Install k6

**Linux**:
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**macOS**:
```bash
brew install k6
```

#### Run Baseline Load Test

```bash
cd load-tests
k6 run k6-baseline-test.js
```

#### Customize Test

Edit `load-tests/k6-baseline-test.js` to:
- Change BASE_URL (default: http://localhost:8084)
- Adjust load stages
- Modify thresholds

#### Monitor During Test

In another terminal, monitor metrics:
```bash
# Health check
watch -n 1 'curl -s http://localhost:8084/actuator/health | jq'

# Metrics
watch -n 1 'curl -s http://localhost:8084/actuator/metrics/http.server.requests | jq'
```

### Option B: Using curl Script

```bash
cd load-tests
chmod +x curl-load-test.sh
./curl-load-test.sh http://localhost:8084 10 100
```

Parameters:
- URL: http://localhost:8084
- Concurrent requests: 10
- Total requests: 100

### Option C: Using Apache Bench

```bash
# Simple test
ab -n 1000 -c 50 http://localhost:8084/AdministrativeAreas/filterList?type=REGION

# More aggressive test
ab -n 5000 -c 100 http://localhost:8084/AdministrativeAreas/filterList?type=REGION
```

## Monitoring During Load Tests

### Key Metrics to Watch

1. **Response Times**:
   ```bash
   curl http://localhost:8084/actuator/metrics/http.server.requests
   ```
   - Target: p95 < 500ms, p99 < 1000ms

2. **Cache Hit Rate**:
   ```bash
   curl http://localhost:8084/actuator/metrics/cache.gets
   curl http://localhost:8084/actuator/metrics/cache.misses
   ```
   - Target: Hit rate > 80% after warm-up

3. **Database Connection Pool**:
   ```bash
   curl http://localhost:8084/actuator/metrics/hikari.connections.active
   ```
   - Target: < 80% of max pool size

4. **Error Rate**:
   ```bash
   curl http://localhost:8084/actuator/metrics/http.server.requests | grep status
   ```
   - Target: < 1%

5. **JVM Memory**:
   ```bash
   curl http://localhost:8084/actuator/metrics/jvm.memory.used
   ```
   - Target: < 80% of heap

### Real-time Monitoring Dashboard

Access Prometheus metrics:
```bash
curl http://localhost:8084/actuator/prometheus | grep -E "(http_server_requests|cache_|hikari_)"
```

## Expected Results

### Swagger Testing

- ✅ All endpoints accessible
- ✅ Request/response formats correct
- ✅ Cache working (second request faster)
- ✅ Cache eviction working (after writes)

### Load Testing

**Baseline Test (k6)**:
- ✅ p95 response time < 500ms
- ✅ p99 response time < 1000ms
- ✅ Error rate < 1%
- ✅ Cache hit rate > 80% (after warm-up)

**Sustained Load**:
- ✅ No memory leaks
- ✅ Stable response times
- ✅ Connection pool not exhausted

## Troubleshooting

### Swagger UI Not Loading

1. Check application is running:
   ```bash
   curl http://localhost:8084/actuator/health
   ```

2. Check Swagger path:
   ```bash
   curl http://localhost:8084/swagger-ui.html
   curl http://localhost:8084/v3/api-docs
   ```

3. Check application logs for errors

### Load Test Failures

1. **Connection Refused**:
   - Verify application is running
   - Check port number (default: 8084)

2. **High Error Rate**:
   - Check database connection
   - Check Redis connection
   - Review application logs

3. **High Response Times**:
   - Check cache hit rate
   - Review database query performance
   - Check connection pool utilization

4. **Out of Memory**:
   - Increase JVM heap size
   - Check for memory leaks
   - Review cache size

## Next Steps After Testing

1. **Document Results**: Record baseline metrics
2. **Identify Bottlenecks**: Analyze slow endpoints
3. **Optimize**: Adjust cache TTLs, connection pools, etc.
4. **Re-test**: Verify improvements
5. **Production Readiness**: Ensure all targets met

---

**Ready to Test!** Start with Swagger, then proceed to load testing.
