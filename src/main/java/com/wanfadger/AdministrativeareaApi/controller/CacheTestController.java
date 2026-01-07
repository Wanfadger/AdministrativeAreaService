package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.shared.util.CacheKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple test controller to verify caching is working
 * 
 * Note: We cache the response body (Map) not ResponseEntity to avoid deserialization issues
 */
@Slf4j
@RestController
@RequestMapping("/cache-test")
public class CacheTestController {

    @GetMapping("/simple")
    @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, key = "'test:simple'")
    public Map<String, Object> simpleCache() {
        log.info("CacheTestController.simpleCache() called - this should only appear once if caching works");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a cached response");
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", true);
        return response;
    }

    @GetMapping("/condition-test")
    @Cacheable(
        value = CacheKeys.ADMINISTRATIVE_AREAS,
        key = "'test:condition'",
        unless = "#result == null || #result['status'] != true"
    )
    public Map<String, Object> conditionTest() {
        log.info("CacheTestController.conditionTest() called - checking condition");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Condition test response");
        response.put("status", true);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/no-condition")
    @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, key = "'test:no-condition'")
    public Map<String, Object> noConditionTest() {
        log.info("CacheTestController.noConditionTest() called - no condition, should always cache");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "No condition test - always cached");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
