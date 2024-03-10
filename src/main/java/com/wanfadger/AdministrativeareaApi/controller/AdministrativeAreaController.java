package com.wanfadger.AdministrativeareaApi.controller;

import com.wanfadger.AdministrativeareaApi.service.administrativearea.AdministrativeAreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("AdministrativeAreas")
@RequiredArgsConstructor
public class AdministrativeAreaController {

    private final AdministrativeAreaService administrativeAreaService;

//    @PostMapping("/one")
//    private ResponseEntity<AdministrativeAreaResponseDto<String>> newOne(@RequestBody AdministrativeAreaDtos.NewAdministrativeAreaDto dto){
//        return administrativeAreaService.newOne(dto);
//    }
//
//    @PostMapping("/list")
//    private ResponseEntity<AdministrativeAreaResponseDto<String>> newList(@RequestBody List<AdministrativeAreaDtos.NewAdministrativeAreaDto> dtos){
//        return administrativeAreaService.newList(dtos);
//    }
//
//    @GetMapping("/filterOne")
//    private AdministrativeAreaResponseDto<AdministrativeAreaDtos.AdministrativeAreaDto> filterOne(@RequestParam Map<String , String> queryMap){
//        return administrativeAreaService.filterOne(queryMap);
//    }
//
//    @GetMapping("/filterList")
//    private AdministrativeAreaResponseDto<List<AdministrativeAreaDtos.AdministrativeAreaDto>> filterList(@RequestParam Map<String , String> queryMap){
//        return administrativeAreaService.filterList(queryMap);
//    }


}
