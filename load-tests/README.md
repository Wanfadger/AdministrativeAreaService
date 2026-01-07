# Load Testing Scripts - Quick Start Guide

This directory contains load testing scripts for the Administrative Area API.

## Prerequisites

1. **Application Running**: Ensure your API is running on `http://localhost:8084` (or your configured port)
2. **Redis Running**: Cache must be available
3. **Database Running**: PostgreSQL must be accessible
4. **Monitoring Ready**: Actuator endpoints should be enabled

## Option 1: k6 Load Test (Recommended)

### Installation

**Linux (Ubuntu/Debian)**:
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

**Windows/Other**:
Download from https://k6.io/docs/getting-started/installation/

### Basic Usage

**Run baseline test with default settings**:
```bash
cd load-tests
k6 run k6-baseline-test.js
```

**Run with custom base URL**:
```bash
k6 run --env BASE_URL=http://your-server:8084 k6-baseline-test.js
```

**Run with output to file**:
```bash
k6 run k6-baseline-test.js --out json=results.json
```

### What the k6 Test Does

- **Duration**: ~5 minutes total
- **Load Pattern**:
  - 30s: Ramp up to 10 users
  - 1m: Ramp up to 50 users
  - 2m: Stay at 100 users
  - 1m: Ramp down to 50 users
  - 30s: Ramp down to 0 users

- **Endpoints Tested** (PARISH-focused - the performance bottleneck):
  - `GET /AdministrativeAreas/parishListByPartOf?type=REGION&partOfCode=001` (slow hierarchical query)
  - `GET /AdministrativeAreas/parishListByPartOf?type=COUNTY&partOfCode=CT001`
  - `GET /AdministrativeAreas/filterList?type=PARISH&partOf=SC001`
  - `GET /AdministrativeAreas/searchList?type=PARISH&partOf=SC001`
  - `GET /AdministrativeAreas/filterOne?type=PARISH&code=P001&partOf=SC001`

- **Success Criteria** (adjusted for PARISH endpoints):
  - p95 response time < 1500ms (PARISH is slower)
  - p99 response time < 3000ms
  - Error rate < 1%

- **Customizable**: Set sample codes via environment variables:
  ```bash
  k6 run --env REGION_CODE=001 --env COUNTY_CODE=CT001 k6-baseline-test.js
  ```

### Customizing the k6 Test

Edit `k6-baseline-test.js` to:
- Change load stages (line 20-26)
- Modify thresholds (line 27-31) - currently set for PARISH endpoints
- Add more endpoints
- Adjust sleep times between requests
- Change sample codes (or set via environment variables)

**Set custom test data codes**:
```bash
k6 run --env REGION_CODE=001 --env COUNTY_CODE=CT001 --env SUB_COUNTY_CODE=SC001 --env PARISH_CODE=P001 k6-baseline-test.js
```

## Option 2: curl Load Test Script

### Basic Usage

**Make script executable**:
```bash
chmod +x load-tests/curl-load-test.sh
```

**Run with default settings** (10 concurrent, 100 total requests):
```bash
./load-tests/curl-load-test.sh
```

**Run with custom parameters**:
```bash
./load-tests/curl-load-test.sh [BASE_URL] [CONCURRENT] [TOTAL]
```

**Examples**:
```bash
# Test localhost with 10 concurrent requests, 100 total
./load-tests/curl-load-test.sh http://localhost:8084 10 100

# Test remote server with 20 concurrent requests, 500 total
./load-tests/curl-load-test.sh http://your-server:8084 20 500

# Light test: 5 concurrent, 50 total
./load-tests/curl-load-test.sh http://localhost:8084 5 50
```

### What the curl Script Does

- **Endpoints Tested** (randomly selected):
  - `/AdministrativeAreas/filterList?type=REGION`
  - `/AdministrativeAreas/searchList?type=REGION`
  - `/AdministrativeAreas/filterOne?type=REGION&code=001`
  - `/AdministrativeAreas/parishListByPartOf?type=REGION&partOfCode=001`

- **Output**:
  - Success/failure counts
  - Response time statistics (min, max, avg)
  - Success rate percentage

### Prerequisites for curl Script

- `curl` installed (usually pre-installed)
- `bc` calculator (for statistics):
  ```bash
  # Ubuntu/Debian
  sudo apt-get install bc
  
  # macOS (usually pre-installed)
  ```

## Monitoring During Tests

### Real-time Health Check
```bash
# Watch health endpoint every second
watch -n 1 'curl -s http://localhost:8084/actuator/health | jq'
```

### Check Metrics
```bash
# HTTP metrics
curl -s http://localhost:8084/actuator/metrics/http.server.requests | jq

# Cache metrics
curl -s http://localhost:8084/actuator/metrics/cache.gets | jq
curl -s http://localhost:8084/actuator/metrics/cache.misses | jq

# Database connection pool
curl -s http://localhost:8084/actuator/metrics/hikari.connections.active | jq
```

### Export Metrics for Analysis
```bash
# Export all Prometheus metrics
curl http://localhost:8084/actuator/prometheus > metrics-export.txt

# Export specific metrics
curl http://localhost:8084/actuator/metrics/http.server.requests > http-metrics.json
```

## Quick Test Scenarios

### 1. Quick Smoke Test (30 seconds)
```bash
# Using curl script - light load
./load-tests/curl-load-test.sh http://localhost:8084 5 20
```

### 2. Baseline Performance Test (5 minutes)
```bash
# Using k6 - recommended
cd load-tests
k6 run k6-baseline-test.js
```

### 3. Sustained Load Test
```bash
# Using curl script - run multiple times
for i in {1..10}; do
  echo "Round $i"
  ./load-tests/curl-load-test.sh http://localhost:8084 20 100
  sleep 10
done
```

### 4. Stress Test
```bash
# Using k6 - modify stages in script to increase load
# Edit k6-baseline-test.js and change target values
k6 run k6-baseline-test.js
```

## Expected Results

### Response Times
- **filterOne**: p95 < 300ms
- **filterList**: p95 < 500ms
- **searchList**: p95 < 800ms

### Success Metrics
- **Error Rate**: < 1%
- **Cache Hit Rate**: > 80% (after warm-up)
- **Success Rate**: > 99%

## Troubleshooting

### k6 Installation Issues
```bash
# Verify installation
k6 version

# If issues, try alternative installation
# Download from: https://github.com/grafana/k6/releases
```

### curl Script Issues
```bash
# Check if bc is installed
which bc

# Check if curl is available
which curl

# Test single request manually
curl -v http://localhost:8084/AdministrativeAreas/filterList?type=REGION
```

### Application Not Responding
1. Check if application is running:
   ```bash
   curl http://localhost:8084/actuator/health
   ```

2. Check application logs for errors

3. Verify Redis is accessible:
   ```bash
   # If using docker-compose
   docker-compose ps
   ```

## Next Steps

After running load tests:

1. **Analyze Results**: Review response times and error rates
2. **Check Metrics**: Compare with targets in `PHASE5_LOAD_TESTING_GUIDE.md`
3. **Optimize**: Based on findings (cache hit rate, connection pool, etc.)
4. **Re-test**: Verify improvements
5. **Document**: Record baseline performance metrics

## Additional Resources

- Full guide: `../PHASE5_LOAD_TESTING_GUIDE.md`
- Performance targets: See guide for detailed targets
- Monitoring setup: See guide for Prometheus/Grafana setup
