package com.wanfadger.AdministrativeareaApi.shared.util;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for generating consistent cache keys
 * 
 * This ensures cache keys are deterministic and consistent across requests
 */
public class CacheKeyUtils {

    /**
     * Generate a cache key from query parameters
     * Keys are sorted to ensure consistency regardless of parameter order
     * 
     * @param queryMap Query parameters map
     * @return Deterministic cache key string
     */
    public static String generateCacheKey(Map<String, String> queryMap) {
        if (queryMap == null || queryMap.isEmpty()) {
            return "default";
        }
        
        return queryMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + (entry.getValue() != null ? entry.getValue() : ""))
                .collect(Collectors.joining("&"));
    }

    /**
     * Generate a cache key with a prefix
     * 
     * @param prefix Cache key prefix
     * @param queryMap Query parameters map
     * @return Cache key with prefix
     */
    public static String generateCacheKey(String prefix, Map<String, String> queryMap) {
        return prefix + ":" + generateCacheKey(queryMap);
    }

    /**
     * Generate a cache key for a specific administrative area type and code
     * 
     * @param type Administrative area type
     * @param code Administrative area code
     * @return Cache key
     */
    public static String generateCacheKey(String type, String code) {
        return type + ":" + code;
    }

    /**
     * Generate a cache key for hierarchical queries
     * 
     * @param type Administrative area type
     * @param partOf Parent administrative area code
     * @return Cache key
     */
    public static String generateHierarchicalCacheKey(String type, String partOf) {
        return type + ":partOf:" + partOf;
    }
}
