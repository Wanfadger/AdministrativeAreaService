package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.shared.util.CacheHelperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Diagnostic controller to verify Redis caching
 * Provides cache information for troubleshooting purposes
 */
@RestController
@RequestMapping("/cache-diagnostics")
@RequiredArgsConstructor
public class CacheDiagnosticController {

    private final CacheHelperService cacheHelperService;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> cacheInfo() {
        Map<String, Object> result = cacheHelperService.getCacheInfo();
        return ResponseEntity.ok(result);
    }
}
