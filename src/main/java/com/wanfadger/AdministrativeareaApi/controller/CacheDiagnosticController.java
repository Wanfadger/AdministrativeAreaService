package com.wanfadger.AdministrativeareaApi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Diagnostic controller to verify Redis caching
 * This helps debug cache key generation and storage
 */
@RestController
@RequestMapping("/cache-diagnostics")
@RequiredArgsConstructor
public class CacheDiagnosticController {

    private final RedisTemplate<?, ?> redisTemplate;

    @GetMapping("/keys")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> listCacheKeys() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get all keys using RedisTemplate with proper casting
            Set<Object> keysObj = ((RedisTemplate<Object, Object>) redisTemplate).keys("*");
            Set<String> keys = new HashSet<>();
            
            if (keysObj != null) {
                keysObj.forEach(key -> keys.add(key.toString()));
            }
            
            result.put("totalKeys", keys.size());
            result.put("keys", new ArrayList<>(keys));
            result.put("status", "success");
            
            // Also check for Spring Cache format (cacheName::key)
            List<String> springCacheKeys = new ArrayList<>();
            keys.forEach(key -> {
                if (key.contains("::") || key.contains("AdministrativeAreas") || key.contains("AdministrativeAreaFilters")) {
                    springCacheKeys.add(key);
                }
            });
            result.put("springCacheKeys", springCacheKeys);
            result.put("springCacheKeyCount", springCacheKeys.size());
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getName());
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> cacheInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("redisConnection", "connected");
            result.put("redisHost", redisTemplate.getConnectionFactory().getConnection().getClass().getSimpleName());
            
            // Try to get a sample key to verify serialization
            @SuppressWarnings("unchecked")
            Set<Object> allKeysObj = ((RedisTemplate<Object, Object>) redisTemplate).keys("*");
            List<String> sampleKeys = new ArrayList<>();
            if (allKeysObj != null) {
                allKeysObj.stream().limit(5).forEach(key -> sampleKeys.add(key.toString()));
            }
            result.put("sampleKeys", sampleKeys);
            result.put("totalKeys", allKeysObj != null ? allKeysObj.size() : 0);
            
        } catch (Exception e) {
            result.put("redisConnection", "error");
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}
