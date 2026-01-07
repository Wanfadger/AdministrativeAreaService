package com.wanfadger.AdministrativeareaApi.shared.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

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
public class ServiceLevelCacheHelper {

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
     */
    @SuppressWarnings("unchecked")
    public void evictAll(String cacheName) {
        try {
            String pattern = cacheName + "::*";
            RedisTemplate<String, Object> template = (RedisTemplate<String, Object>) redisTemplate;
            java.util.Set<String> keys = template.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                template.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to evict all cache entries for {}: {}", cacheName, e.getMessage());
        }
    }

    /**
     * Generate cache key from query map (same logic as QueryMapKeyGenerator)
     */
    public static String generateKey(String methodName, java.util.Map<String, String> queryMap) {
        if (queryMap == null || queryMap.isEmpty()) {
            return methodName + ":default";
        }

        String key = queryMap.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> {
                    String k = entry.getKey() != null ? entry.getKey() : "";
                    String v = entry.getValue() != null ? entry.getValue() : "";
                    return k + "=" + v;
                })
                .collect(java.util.stream.Collectors.joining("&"));

        return methodName + ":" + key;
    }
}
