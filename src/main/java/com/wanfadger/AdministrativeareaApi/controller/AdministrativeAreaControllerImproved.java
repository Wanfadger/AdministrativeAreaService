package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.dto.*;
import com.wanfadger.AdministrativeareaApi.service.administrativearea.AdministrativeAreaService;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import com.wanfadger.AdministrativeareaApi.shared.util.CacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Improved Administrative Area Controller with optimized caching
 * 
 * Key improvements:
 * 1. Uses queryMapKeyGenerator for deterministic cache keys
 * 2. Selective cache eviction (where possible)
 * 3. Consistent cache manager usage
 * 4. Cache conditions for error handling
 * 
 * Migration steps:
 * 1. Review changes
 * 2. Test thoroughly
 * 3. Replace existing controller
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin()
@RequestMapping("/AdministrativeAreas")
public class AdministrativeAreaControllerImproved {

    private final AdministrativeAreaService administrativeAreaService;

    /**
     * Create single administrative area
     * 
     * Cache eviction strategy:
     * - Evicts all entries in affected caches
     * 
     * Note: Spring Cache doesn't support pattern-based eviction natively.
     * For selective eviction, consider implementing custom cache manager
     * or use cache tags. For now, evicting all entries ensures consistency.
     */
    @PostMapping("/one")
    @CacheEvict(value = {
        CacheKeys.ADMINISTRATIVE_AREAS_FILTER,
        CacheKeys.ADMINISTRATIVE_AREAS
    }, 
    allEntries = true) // Evict all to ensure consistency
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(
            @RequestParam Map<String, String> queryMap, 
            @RequestBody NewAdministrativeAreaDto dto) {
        return administrativeAreaService.newOne(queryMap, dto);
    }

    /**
     * Create multiple administrative areas
     * Evicts all cache entries
     */
    @PostMapping("/list")
    @CacheEvict(value = {
        CacheKeys.ADMINISTRATIVE_AREAS_FILTER,
        CacheKeys.ADMINISTRATIVE_AREAS
    }, 
    allEntries = true)
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newList(
            @RequestParam Map<String, String> queryMap, 
            @RequestBody List<NewAdministrativeAreaDto> dtos) {
        return administrativeAreaService.newList(queryMap, dtos);
    }

    /**
     * Bulk upload administrative areas
     * Evicts all cache (bulk operation affects multiple types)
     */
    @PostMapping("/upload")
    @CacheEvict(value = {
        CacheKeys.ADMINISTRATIVE_AREAS,
        CacheKeys.ADMINISTRATIVE_AREAS_FILTER
    }, 
    allEntries = true) // Bulk upload affects multiple types, clear all
    public AdministrativeAreaResponseDto<String> upload(
            @RequestBody List<AdministrativeAreaExcelDto> administrativeAreaExcelDtos) {
        return administrativeAreaService.upload(administrativeAreaExcelDtos);
    }

    /**
     * Update single administrative area
     * Evicts all cache entries to ensure consistency
     * 
     * Note: For selective eviction, you could evict specific keys:
     * key = "'type:' + #queryMap['type'] + ':code:' + #dto.code"
     * But this requires knowing all possible cache keys that might be affected
     */
    @PutMapping("/one")
    @CacheEvict(value = {
        CacheKeys.ADMINISTRATIVE_AREAS_FILTER,
        CacheKeys.ADMINISTRATIVE_AREAS
    }, 
    allEntries = true) // Evict all to ensure consistency
    public AdministrativeAreaResponseDto<String> updateOne(
            @RequestParam Map<String, String> queryMap, 
            @RequestBody UpdateAdministrativeAreaDto dto) {
        return administrativeAreaService.updateOne(queryMap, dto);
    }

    /**
     * Filter single administrative area
     * Uses deterministic key generator and week cache (stable data)
     */
    @GetMapping("/filterOne")
    @Cacheable(
        value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
        keyGenerator = "queryMapKeyGenerator",
        cacheManager = "weekCacheManager",
        condition = "#result != null && #result.success != null && #result.success == true"
    )
    public AdministrativeAreaResponseDto<CodeNameDto> filterOne(
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.filterOne(queryMap);
    }

    /**
     * Filter list of administrative areas
     * Uses deterministic key generator and week cache (stable data)
     */
    @GetMapping("/filterList")
    @Cacheable(
        value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, 
        keyGenerator = "queryMapKeyGenerator",
        cacheManager = "weekCacheManager",
        condition = "#result != null && #result.success != null && #result.success == true"
    )
    public AdministrativeAreaResponseDto<List<CodeNameDto>> filterList(
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.filterList(queryMap);
    }

    /**
     * Get parishes by part of
     * Uses deterministic key generator and week cache (stable hierarchical data)
     */
    @GetMapping("/parishListByPartOf")
    @Cacheable(
        value = CacheKeys.ADMINISTRATIVE_AREAS, 
        keyGenerator = "queryMapKeyGenerator",
        cacheManager = "weekCacheManager",
        condition = "#result != null && #result.success != null && #result.success == true"
    )
    public AdministrativeAreaResponseDto<List<CodeNameDto>> getParishByPartOf(
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.getParishByPartOf(queryMap);
    }

    /**
     * Search list of administrative areas
     * Uses deterministic key generator and hour cache (may change more frequently)
     */
    @GetMapping("/searchList")
    @Cacheable(
        value = CacheKeys.ADMINISTRATIVE_AREAS, 
        keyGenerator = "queryMapKeyGenerator",
        cacheManager = "hourCacheManager", // Changed from default
        condition = "#result != null && #result.success != null && #result.success == true"
    )
    public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.searchList(queryMap);
    }

    /**
     * Search single administrative area
     * Uses deterministic key generator and hour cache
     */
    @GetMapping("/searchOne")
    @Cacheable(
        value = CacheKeys.ADMINISTRATIVE_AREAS, 
        keyGenerator = "queryMapKeyGenerator",
        cacheManager = "hourCacheManager", // Changed from default
        condition = "#result != null && #result.success != null && #result.success == true"
    )
    public AdministrativeAreaResponseDto<? extends AdministrativeAreaDto> searchOne(
            @RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.searchOne(queryMap);
    }
}
