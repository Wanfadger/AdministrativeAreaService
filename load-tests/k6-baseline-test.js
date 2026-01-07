/**
 * k6 Load Test Script - PARISH Performance Test
 * 
 * Usage:
 *   k6 run k6-baseline-test.js
 * 
 * This script tests PARISH endpoints which are the performance bottleneck.
 * Tests focus on slow endpoints even with cache enabled.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');

// Test configuration - Adjusted thresholds for PARISH (slower endpoints)
export const options = {
    stages: [
        { duration: '30s', target: 10 },   // Ramp up to 10 users
        { duration: '1m', target: 50 },     // Ramp up to 50 users
        { duration: '2m', target: 100 },    // Stay at 100 users
        { duration: '1m', target: 50 },      // Ramp down to 50 users
        { duration: '30s', target: 0 },       // Ramp down to 0 users
    ],
    thresholds: {
        'http_req_duration': ['p(95)<1500', 'p(99)<3000'], // PARISH is slower: 95% < 1.5s, 99% < 3s
        'http_req_failed': ['rate<0.01'],                  // Error rate < 1%
        'errors': ['rate<0.01'],                            // Custom error rate < 1%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8084';

// Sample codes for testing (adjust based on your test data)
const SAMPLE_REGION_CODE = __ENV.REGION_CODE || '001';
const SAMPLE_COUNTY_CODE = __ENV.COUNTY_CODE || 'CT001';
const SAMPLE_SUB_COUNTY_CODE = __ENV.SUB_COUNTY_CODE || 'SC001';
const SAMPLE_PARISH_CODE = __ENV.PARISH_CODE || 'P001';

export default function () {
    // Test 1: Parish List by Region - Slow hierarchical query
    const parishByRegionRes = http.get(`${BASE_URL}/AdministrativeAreas/parishListByPartOf?type=REGION&partOfCode=${SAMPLE_REGION_CODE}`, {
        tags: { name: 'parishListByPartOf_REGION' },
    });
    
    const parishByRegionCheck = check(parishByRegionRes, {
        'parishListByPartOf_REGION status is 200': (r) => r.status === 200,
        'parishListByPartOf_REGION response time < 2000ms': (r) => r.timings.duration < 2000,
        'parishListByPartOf_REGION has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.status === true && body.data !== null;
            } catch (e) {
                return false;
            }
        },
    });
    
    errorRate.add(!parishByRegionCheck);
    responseTime.add(parishByRegionRes.timings.duration);
    
    sleep(1);
    
    // Test 2: Parish List by County - Common query
    const parishByCountyRes = http.get(`${BASE_URL}/AdministrativeAreas/parishListByPartOf?type=COUNTY&partOfCode=${SAMPLE_COUNTY_CODE}`, {
        tags: { name: 'parishListByPartOf_COUNTY' },
    });
    
    check(parishByCountyRes, {
        'parishListByPartOf_COUNTY status is 200': (r) => parishByCountyRes.status === 200,
        'parishListByPartOf_COUNTY response time < 1500ms': (r) => r.timings.duration < 1500,
    });
    
    sleep(1);
    
    // Test 3: Filter List - PARISH with partOf
    const filterListRes = http.get(`${BASE_URL}/AdministrativeAreas/filterList?type=PARISH&partOf=${SAMPLE_SUB_COUNTY_CODE}`, {
        tags: { name: 'filterList_PARISH' },
    });
    
    check(filterListRes, {
        'filterList_PARISH status is 200': (r) => r.status === 200,
        'filterList_PARISH response time < 1000ms': (r) => r.timings.duration < 1000,
    });
    
    sleep(1);
    
    // Test 4: Search List - PARISH with full details
    const searchListRes = http.get(`${BASE_URL}/AdministrativeAreas/searchList?type=PARISH&partOf=${SAMPLE_SUB_COUNTY_CODE}`, {
        tags: { name: 'searchList_PARISH' },
    });
    
    check(searchListRes, {
        'searchList_PARISH status is 200': (r) => r.status === 200,
        'searchList_PARISH response time < 1500ms': (r) => r.timings.duration < 1500,
    });
    
    sleep(1);
    
    // Test 5: Filter One - Single PARISH lookup
    const filterOneRes = http.get(`${BASE_URL}/AdministrativeAreas/filterOne?type=PARISH&code=${SAMPLE_PARISH_CODE}&partOf=${SAMPLE_SUB_COUNTY_CODE}`, {
        tags: { name: 'filterOne_PARISH' },
    });
    
    check(filterOneRes, {
        'filterOne_PARISH status is 200': (r) => r.status === 200,
        'filterOne_PARISH response time < 500ms': (r) => r.timings.duration < 500,
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
    
    // Per-endpoint metrics
    if (data.metrics.http_req_duration) {
        summary += 'Endpoint Performance:\n';
        const tags = data.metrics.http_req_duration.values.tags || {};
        Object.keys(tags).forEach(tag => {
            if (tags[tag] && tags[tag].values) {
                summary += `${indent}${tag}:\n`;
                summary += `${indent}  p50: ${tags[tag].values.p50?.toFixed(2) || 'N/A'}ms\n`;
                summary += `${indent}  p95: ${tags[tag].values.p95?.toFixed(2) || 'N/A'}ms\n`;
                summary += `${indent}  p99: ${tags[tag].values.p99?.toFixed(2) || 'N/A'}ms\n`;
            }
        });
        summary += '\n';
    }
    
    summary += '='.repeat(60) + '\n';
    summary += 'Note: PARISH endpoints are slower. Monitor cache hit rates.\n';
    summary += '='.repeat(60) + '\n';
    
    return summary;
}
