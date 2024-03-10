package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDto;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDto;
import com.wanfadger.AdministrativeareaApi.repository.projections.CodeNameProjection;
import com.wanfadger.AdministrativeareaApi.service.administrativearea.AdministrativeAreaService;
import com.wanfadger.AdministrativeareaApi.shared.reponses.AdministrativeAreaResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/AdministrativeAreas")
@RequiredArgsConstructor
public class AdministrativeAreaController {

    private final AdministrativeAreaService administrativeAreaService;

    @PostMapping("/one/{type}")
    private ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(@PathVariable String type, @RequestBody NewAdministrativeAreaDto dto){
        return administrativeAreaService.newOne(type,dto);
    }

    @PostMapping("/list/{type}")
    private ResponseEntity<AdministrativeAreaResponseDto<String>> newList(@PathVariable String type , @RequestBody List<NewAdministrativeAreaDto> dtos){
        return administrativeAreaService.newList(type , dtos);
    }

    @GetMapping("/filterOne")
    private AdministrativeAreaResponseDto<CodeNameProjection> filterOne(@RequestParam Map<String , String> queryMap){
        return administrativeAreaService.filterOne(queryMap);
    }

    @GetMapping("/filterList")
    private AdministrativeAreaResponseDto<List<CodeNameProjection>> filterList(@RequestParam Map<String , String> queryMap){
        return administrativeAreaService.filterList(queryMap);
    }


    @GetMapping("/searchList")
    private AdministrativeAreaResponseDto<List<?>> searchList(@RequestParam Map<String , String> queryMap){
        return administrativeAreaService.searchList(queryMap);
    }




}
