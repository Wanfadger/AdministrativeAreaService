/**
 * k6 Load Test Script - Baseline Performance Test
 * 
 * Usage:
 *   k6 run k6-baseline-test.js
 * 
 * This script tests the most common endpoints with gradual load increase
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');

// Test configuration
export const options = {
    stages: [
        { duration: '30s', target: 10 },   // Ramp up to 10 users
        { duration: '1m', target: 50 },     // Ramp up to 50 users
        { duration: '2m', target: 100 },    // Stay at 100 users
        { duration: '1m', target: 50 },      // Ramp down to 50 users
        { duration: '30s', target: 0 },       // Ramp down to 0 users
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500', 'p(99)<1000'], // 95% of requests < 500ms, 99% < 1s
        'http_req_failed': ['rate<0.01'],                  // Error rate < 1%
        'errors': ['rate<0.01'],                            // Custom error rate < 1%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8084';

export default function () {
    // Test 1: Filter List - Most common endpoint
    const filterListRes = http.get(`${BASE_URL}/AdministrativeAreas/filterList?type=REGION`, {
        tags: { name: 'filterList' },
    });
    
    const filterListCheck = check(filterListRes, {
        'filterList status is 200': (r) => r.status === 200,
        'filterList response time < 500ms': (r) => r.timings.duration < 500,
        'filterList has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.status === true && body.data !== null;
            } catch (e) {
                return false;
            }
        },
    });
    
    errorRate.add(!filterListCheck);
    responseTime.add(filterListRes.timings.duration);
    
    sleep(1);
    
    // Test 2: Search List - Common search endpoint
    const searchListRes = http.get(`${BASE_URL}/AdministrativeAreas/searchList?type=REGION`, {
        tags: { name: 'searchList' },
    });
    
    check(searchListRes, {
        'searchList status is 200': (r) => r.status === 200,
        'searchList response time < 500ms': (r) => r.timings.duration < 500,
    });
    
    sleep(1);
    
    // Test 3: Filter One - Single item lookup
    const filterOneRes = http.get(`${BASE_URL}/AdministrativeAreas/filterOne?type=REGION&code=001`, {
        tags: { name: 'filterOne' },
    });
    
    check(filterOneRes, {
        'filterOne status is 200': (r) => r.status === 200,
        'filterOne response time < 300ms': (r) => r.timings.duration < 300,
    });
    
    sleep(1);
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'summary.json': JSON.stringify(data),
    };
}

function textSummary(data, options) {
    const indent = options.indent || '  ';
    const enableColors = options.enableColors || false;
    
    let summary = '\n';
    summary += '='.repeat(60) + '\n';
    summary += 'LOAD TEST SUMMARY\n';
    summary += '='.repeat(60) + '\n\n';
    
    // HTTP metrics
    if (data.metrics.http_req_duration) {
        summary += 'Response Times:\n';
        summary += `${indent}p50: ${data.metrics.http_req_duration.values.p50.toFixed(2)}ms\n`;
        summary += `${indent}p95: ${data.metrics.http_req_duration.values.p95.toFixed(2)}ms\n`;
        summary += `${indent}p99: ${data.metrics.http_req_duration.values.p99.toFixed(2)}ms\n`;
        summary += `${indent}max: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms\n\n`;
    }
    
    if (data.metrics.http_reqs) {
        summary += 'Requests:\n';
        summary += `${indent}total: ${data.metrics.http_reqs.values.count}\n`;
        summary += `${indent}rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n\n`;
    }
    
    if (data.metrics.http_req_failed) {
        summary += 'Errors:\n';
        summary += `${indent}rate: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
        summary += `${indent}count: ${data.metrics.http_req_failed.values.passes}\n\n`;
    }
    
    summary += '='.repeat(60) + '\n';
    
    return summary;
}
