package com.wanfadger.AdministrativeareaApi.shared.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service-level cache helper that stores clean JSON (no @class fields)
 * and deserializes using explicit type information.
 * 
 * This approach:
 * 1. Stores clean JSON that other services/languages can read
 * 2. Uses explicit type information during deserialization (no casting errors)
 * 3. Works seamlessly with ParameterizedTypeReference for generic types
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheHelperService {

    private final RedisTemplate<?, ?> redisTemplate;
    
    @Autowired(required = false)
    private ObjectMapper objectMapper;

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        return objectMapper;
    }

    /**
     * Get cached value with explicit type conversion
     * 
     * @param cacheName Cache name (e.g., "administrativeAreas")
     * @param key Cache key
     * @param typeReference Type reference for deserialization
     * @return Cached value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key, ParameterizedTypeReference<T> typeReference) {
        try {
            String fullKey = cacheName + "::" + key;
            Object cached = ((RedisTemplate<String, Object>) redisTemplate).opsForValue().get(fullKey);
            
            if (cached == null) {
                return null;
            }
            
            // Convert LinkedHashMap (from clean JSON) to target type
            ObjectMapper mapper = getObjectMapper();
            return mapper.convertValue(cached, 
                mapper.getTypeFactory().constructType(typeReference.getType()));
        } catch (Exception e) {
            log.warn("Failed to get cache value for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Put value in cache with TTL
     * 
     * @param cacheName Cache name
     * @param key Cache key
     * @param value Value to cache (will be serialized to clean JSON)
     * @param ttl Time to live
     * @param timeUnit Time unit for TTL
     */
    @SuppressWarnings("unchecked")
    public <T> void put(String cacheName, String key, T value, long ttl, TimeUnit timeUnit) {
        try {
            String fullKey = cacheName + "::" + key;
            ((RedisTemplate<String, Object>) redisTemplate).opsForValue().set(fullKey, value, ttl, timeUnit);
            log.debug("Cached value for key: {}", fullKey);
        } catch (Exception e) {
            log.warn("Failed to cache value for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Evict cache entry
     */
    @SuppressWarnings("unchecked")
    public void evict(String cacheName, String key) {
        try {
            String fullKey = cacheName + "::" + key;
            ((RedisTemplate<String, Object>) redisTemplate).delete(fullKey);
        } catch (Exception e) {
            log.warn("Failed to evict cache key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Evict all entries in a cache
     * 
     * Uses SCAN instead of KEYS for better performance (non-blocking).
     * SCAN iterates through keys without blocking the Redis server.
     */
    @SuppressWarnings("unchecked")
    public void evictAll(String cacheName) {
        try {
            String pattern = cacheName + "::*";
            RedisTemplate<String, Object> template = (RedisTemplate<String, Object>) redisTemplate;
            
            // Use SCAN instead of KEYS for better performance (non-blocking)
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100) // Process 100 keys at a time
                    .build();
            
            try (Cursor<String> cursor = template.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            
            if (!keys.isEmpty()) {
                template.delete(keys);
                log.debug("Evicted {} cache entries for {}", keys.size(), cacheName);
            }
        } catch (Exception e) {
            log.warn("Failed to evict all cache entries for {}: {}", cacheName, e.getMessage());
        }
    }

    /**
     * Get cache diagnostic information for troubleshooting
     * 
     * Returns information about Redis connection, cache keys, and cache statistics.
     * Uses SCAN instead of KEYS for non-blocking operation.
     * 
     * @return Map containing cache diagnostic information
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCacheInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Redis connection info
            result.put("redisConnection", "connected");
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory != null) {
                result.put("connectionFactory", connectionFactory.getClass().getSimpleName());
            } else {
                result.put("connectionFactory", "unknown");
            }
            
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
        
        return result;
    }

    /**
     * Generate cache key from query map
     * 
     * Creates deterministic cache keys by sorting query parameters alphabetically.
     * This ensures consistent keys regardless of Map iteration order.
     * 
     * Note: Method name is not included to allow external services to construct
     * keys using only query parameters. Cache namespace (cacheName) provides
     * logical separation between different operation types.
     * 
     * @param queryMap Query parameters map
     * @return Deterministic cache key string
     */
    public static String generateKey(java.util.Map<String, String> queryMap) {
        if (queryMap == null || queryMap.isEmpty()) {
            return "default";
        }

        return queryMap.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> {
                    String k = entry.getKey() != null ? entry.getKey() : "";
                    String v = entry.getValue() != null ? entry.getValue() : "";
                    return k + "=" + v;
                })
                .collect(java.util.stream.Collectors.joining("&"));
    }
}
