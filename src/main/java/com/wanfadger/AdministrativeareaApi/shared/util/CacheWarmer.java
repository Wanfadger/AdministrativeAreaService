package com.wanfadger.AdministrativeareaApi.shared.util;

import com.wanfadger.AdministrativeareaApi.service.administrativearea.AdministrativeAreaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache warmer component to pre-load frequently accessed data on application startup.
 * This improves response times for the first requests after startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmer {

    private final AdministrativeAreaService administrativeAreaService;

    /**
     * Warm cache with common queries after application is ready.
     * This runs asynchronously to avoid blocking application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmCache() {
        log.info("Starting cache warming...");
        
        try {
            // Warm cache with top-level administrative areas (most frequently accessed)
            warmRegionCache();
            
            log.info("Cache warming completed successfully");
        } catch (Exception e) {
            // Log but don't fail startup if cache warming fails
            log.warn("Cache warming encountered an error: {}", e.getMessage());
        }
    }

    /**
     * Warm cache with region queries (top-level administrative areas)
     */
    private void warmRegionCache() {
        try {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("type", "REGION");
            
            // Warm filterList cache (most common query)
            administrativeAreaService.filterList(queryMap);
            log.debug("Warmed cache for: type=REGION (filterList)");
            
            // Warm searchList cache
            administrativeAreaService.searchList(queryMap);
            log.debug("Warmed cache for: type=REGION (searchList)");

            Map<String, String> subRegionQuery = Map.of("type", "SUB REGION");
            String subRegionKey = CacheHelperService.generateKey(subRegionQuery);
            log.info("Warming cache for SUB REGION - generated key: '{}'", subRegionKey);
            administrativeAreaService.searchList(subRegionQuery);
            log.info("Warmed cache for: type=SUB REGION (searchList) with key: '{}'", subRegionKey);

            administrativeAreaService.searchList(Map.of("type", "LOCAL GOVERNMENT"));
            log.debug("Warmed cache for: type=LOCAL GOVERNMENT (searchList)");

            administrativeAreaService.searchList(Map.of("type", "COUNTY"));
            log.debug("Warmed cache for: type=COUNTY (searchList)");

            administrativeAreaService.searchList(Map.of("type", "SUB COUNTY"));
            log.debug("Warmed cache for: type=SUB COUNTY (searchList)");

            administrativeAreaService.searchList(Map.of("type", "PARISH"));
            log.debug("Warmed cache for: type=PARISH (searchList)");

            
        } catch (Exception e) {
            log.warn("Failed to warm region cache: {}", e.getMessage());
        }
    }
}
