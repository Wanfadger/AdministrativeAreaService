#!/bin/bash

# Simple Load Test Script using curl
# 
# Usage:
#   chmod +x curl-load-test.sh
#   ./curl-load-test.sh [BASE_URL] [CONCURRENT_REQUESTS] [TOTAL_REQUESTS]
#
# Example:
#   ./curl-load-test.sh http://localhost:8084 10 100

BASE_URL=${1:-"http://localhost:8084"}
CONCURRENT=${2:-10}
TOTAL=${3:-100}

echo "=========================================="
echo "Load Test Configuration"
echo "=========================================="
echo "Base URL: $BASE_URL"
echo "Concurrent Requests: $CONCURRENT"
echo "Total Requests: $TOTAL"
echo "=========================================="
echo ""

# Test endpoints
ENDPOINTS=(
    "/AdministrativeAreas/filterList?type=REGION"
    "/AdministrativeAreas/searchList?type=REGION"
    "/AdministrativeAreas/filterOne?type=REGION&code=001"
    "/AdministrativeAreas/parishListByPartOf?type=REGION&partOfCode=001"
)

# Results storage
SUCCESS=0
FAILED=0
TOTAL_TIME=0
MIN_TIME=999999
MAX_TIME=0

# Function to make a request
make_request() {
    local endpoint=$1
    local url="${BASE_URL}${endpoint}"
    
    local start_time=$(date +%s%N)
    local response=$(curl -s -w "\n%{http_code}\n%{time_total}" -o /tmp/response.json "$url")
    local end_time=$(date +%s%N)
    
    local http_code=$(echo "$response" | tail -n 2 | head -n 1)
    local time_total=$(echo "$response" | tail -n 1)
    local time_ms=$(echo "$time_total * 1000" | bc)
    
    if [ "$http_code" -eq 200 ]; then
        SUCCESS=$((SUCCESS + 1))
    else
        FAILED=$((FAILED + 1))
        echo "ERROR: $url - HTTP $http_code"
    fi
    
    # Update timing stats
    TOTAL_TIME=$(echo "$TOTAL_TIME + $time_ms" | bc)
    if (( $(echo "$time_ms < $MIN_TIME" | bc -l) )); then
        MIN_TIME=$time_ms
    fi
    if (( $(echo "$time_ms > $MAX_TIME" | bc -l) )); then
        MAX_TIME=$time_ms
    fi
    
    echo "Request: $endpoint - ${time_ms}ms - HTTP $http_code"
}

# Run load test
echo "Starting load test..."
echo ""

for ((i=1; i<=TOTAL; i++)); do
    # Select random endpoint
    endpoint=${ENDPOINTS[$RANDOM % ${#ENDPOINTS[@]}]}
    
    # Run concurrent requests
    for ((j=1; j<=CONCURRENT; j++)); do
        make_request "$endpoint" &
    done
    
    # Wait for all concurrent requests to complete
    wait
    
    # Progress indicator
    if [ $((i % 10)) -eq 0 ]; then
        echo "Progress: $i/$TOTAL requests completed"
    fi
done

# Calculate statistics
AVG_TIME=$(echo "scale=2; $TOTAL_TIME / ($SUCCESS + $FAILED)" | bc)
SUCCESS_RATE=$(echo "scale=2; $SUCCESS * 100 / ($SUCCESS + $FAILED)" | bc)

# Print summary
echo ""
echo "=========================================="
echo "Load Test Results"
echo "=========================================="
echo "Total Requests: $((SUCCESS + FAILED))"
echo "Successful: $SUCCESS"
echo "Failed: $FAILED"
echo "Success Rate: ${SUCCESS_RATE}%"
echo ""
echo "Response Times:"
echo "  Min: ${MIN_TIME}ms"
echo "  Max: ${MAX_TIME}ms"
echo "  Avg: ${AVG_TIME}ms"
echo "=========================================="

# Cleanup
rm -f /tmp/response.json
