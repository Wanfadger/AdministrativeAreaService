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

@RestController
@RequiredArgsConstructor
@CrossOrigin()
@RequestMapping("/AdministrativeAreas")
public class AdministrativeAreaController {

    private final AdministrativeAreaService administrativeAreaService;

    @PostMapping("/one")
    @CacheEvict(value = {CacheKeys.ADMINISTRATIVE_AREAS , CacheKeys.ADMINISTRATIVE_AREAS_FILTER}, allEntries = true)
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(@RequestParam Map<String, String> queryMap, @RequestBody NewAdministrativeAreaDto dto) {
        return administrativeAreaService.newOne(queryMap, dto);
    }

    @PostMapping("/list")
    @CacheEvict(value = {CacheKeys.ADMINISTRATIVE_AREAS , CacheKeys.ADMINISTRATIVE_AREAS_FILTER}, allEntries = true)
    public ResponseEntity<AdministrativeAreaResponseDto<String>> newList(@RequestParam Map<String, String> queryMap, @RequestBody List<NewAdministrativeAreaDto> dtos) {
        return administrativeAreaService.newList(queryMap, dtos);
    }

    @PostMapping("/upload")
    @CacheEvict(value = {CacheKeys.ADMINISTRATIVE_AREAS , CacheKeys.ADMINISTRATIVE_AREAS_FILTER}, allEntries = true)
    public AdministrativeAreaResponseDto<String> upload(@RequestBody List<AdministrativeAreaExcelDto> administrativeAreaExcelDtos) {
        return administrativeAreaService.upload(administrativeAreaExcelDtos);
    }


    @PutMapping("/one")
    @CacheEvict(value = {CacheKeys.ADMINISTRATIVE_AREAS , CacheKeys.ADMINISTRATIVE_AREAS_FILTER}, allEntries = true)
    public AdministrativeAreaResponseDto<String> updateOne(@RequestParam Map<String, String> queryMap, @RequestBody UpdateAdministrativeAreaDto dto) {
        return administrativeAreaService.updateOne(queryMap, dto);
    }


    @GetMapping("/filterOne")
    @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, key = "#queryMap")
    public AdministrativeAreaResponseDto<CodeNameDto> filterOne(@RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.filterOne(queryMap);
    }


    @GetMapping("/filterList")
    @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS_FILTER, key = "#queryMap" , cacheManager = "weekCacheManager")
    public AdministrativeAreaResponseDto<List<CodeNameDto>> filterList(@RequestParam Map<String, String> queryMap ) {
        return administrativeAreaService.filterList(queryMap);
    }

    @GetMapping("/parishListByPartOf")
    @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, key = "#queryMap", cacheManager = "weekCacheManager")
    public AdministrativeAreaResponseDto<List<CodeNameDto>> getParishByPartOf(@RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.getParishByPartOf(queryMap);
    }


    @GetMapping("/searchList")
    @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS, key = "#queryMap")
    public AdministrativeAreaResponseDto<List<? extends AdministrativeAreaDto>> searchList(@RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.searchList(queryMap);
    }



    @GetMapping("/searchOne")
    @Cacheable(value = CacheKeys.ADMINISTRATIVE_AREAS , key = "#queryMap" )
    public AdministrativeAreaResponseDto<? extends AdministrativeAreaDto> searchOne(@RequestParam Map<String, String> queryMap) {
        return administrativeAreaService.searchOne(queryMap);
    }


}

