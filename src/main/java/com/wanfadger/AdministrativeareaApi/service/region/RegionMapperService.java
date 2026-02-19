package com.wanfadger.AdministrativeareaApi.service.region;

import org.springframework.stereotype.Service;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.shared.SharedService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegionMapperService {

    private final SharedService sharedService;

    public RegionDTO toDTO(Region region) {
        return RegionDTO.builder()
                .code(region.getCode())
                .name(region.getName())
                .description(region.getDescription())
                .longitude(region.getLongitude() != null ? String.valueOf(region.getLongitude()) : "")
                .latitude(region.getLatitude() != null ? String.valueOf(region.getLatitude()) : "")
                .build();
    }


    public Region toRegion(NewAdministrativeAreaDTO dto) {
        return Region.builder()
                .code(sharedService.generateCode(AdministrativeAreaType.REGION))
                .name(dto.getName().trim())
                .latitude(dto.getLatitude() != null && !dto.getLatitude().isEmpty() ? Double.valueOf(dto.getLatitude()) : null)
                .longitude(dto.getLongitude() != null && !dto.getLongitude().isEmpty() ? Double.valueOf(dto.getLongitude()) : null)
                .description(dto.getDescription() != null && !dto.getDescription().isEmpty() ? dto.getDescription().trim() : null)
                .areaType(AdministrativeAreaType.REGION)
                .build();
    }   

}
