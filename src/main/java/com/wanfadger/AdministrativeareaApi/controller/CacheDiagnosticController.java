package com.wanfadger.AdministrativeareaApi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Provides cache information for troubleshooting purposes
 */
@Slf4j
@RestController
@RequestMapping("/cache-diagnostics")
@RequiredArgsConstructor
public class CacheDiagnosticController {

    private final RedisTemplate<?, ?> redisTemplate;

    @GetMapping("/info")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> cacheInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Redis connection info
            result.put("redisConnection", "connected");
            result.put("connectionFactory", redisTemplate.getConnectionFactory()
                    .getClass().getSimpleName());
            
            // Use SCAN instead of KEYS for non-blocking operation
            RedisTemplate<String, Object> template = (RedisTemplate<String, Object>) redisTemplate;
            Set<String> allKeys = new HashSet<>();
            List<String> sampleKeys = new ArrayList<>();
            List<String> cacheNamespaceKeys = new ArrayList<>();
            
            ScanOptions options = ScanOptions.scanOptions()
                    .match("*")
                    .count(100)
                    .build();
            
            try (Cursor<String> cursor = template.scan(options)) {
                int count = 0;
                while (cursor.hasNext() && count < 1000) { // Limit scan to prevent long operations
                    String key = cursor.next();
                    allKeys.add(key);
                    
                    // Collect sample keys (first 10)
                    if (sampleKeys.size() < 10) {
                        sampleKeys.add(key);
                    }
                    
                    // Collect cache namespace keys (AdministrativeAreas, AdministrativeAreaFilters)
                    if (key.contains("AdministrativeAreas") || key.contains("AdministrativeAreaFilters")) {
                        if (cacheNamespaceKeys.size() < 20) { // Limit to 20 examples
                            cacheNamespaceKeys.add(key);
                        }
                    }
                    
                    count++;
                }
            }
            
            result.put("totalKeys", allKeys.size());
            result.put("sampleKeys", sampleKeys);
            result.put("cacheNamespaceKeys", cacheNamespaceKeys);
            result.put("cacheNamespaceKeyCount", cacheNamespaceKeys.size());
            result.put("status", "success");
            
        } catch (Exception e) {
            log.warn("Error retrieving cache info: {}", e.getMessage());
            result.put("redisConnection", "error");
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getSimpleName());
        }
        
        return ResponseEntity.ok(result);
    }
}
