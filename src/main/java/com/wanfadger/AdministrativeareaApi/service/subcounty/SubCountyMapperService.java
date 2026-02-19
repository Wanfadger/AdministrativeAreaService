package com.wanfadger.AdministrativeareaApi.service.subcounty;

import org.springframework.stereotype.Service;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubCountyDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.service.county.CountyMapperService;
import com.wanfadger.AdministrativeareaApi.shared.SharedService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubCountyMapperService {

    private final SharedService sharedService;
    private final CountyMapperService countyMapperService;

    public SubCountyDTO toDTO(SubCounty subCounty) {
        return SubCountyDTO.builder()
                .code(subCounty.getCode())
                .name(subCounty.getName())
                .latitude(subCounty.getLatitude() != null ? String.valueOf(subCounty.getLatitude()) : "")
                .longitude(subCounty.getLongitude() != null ? String.valueOf(subCounty.getLongitude()) : "")
                .description(subCounty.getDescription() != null ? subCounty.getDescription().trim() : null)
                .build();
    }

    public SubCountyDTO toDetailDTO(SubCounty subCounty) {
        SubCountyDTO dto = toDTO(subCounty);
        if (subCounty.getCounty() != null) {
            dto.setCounty(countyMapperService.toDetailDTO(subCounty.getCounty()));
        }
        return dto;
    }

    public SubCounty toSubCounty(NewAdministrativeAreaDTO dto, County county) {
        return SubCounty.builder()
                .code(sharedService.generateCode(AdministrativeAreaType.SUBCOUNTY))
                .name(dto.getName().trim())
                .county(county)
                .latitude(dto.getLatitude() != null && !dto.getLatitude().isEmpty() ? Double.valueOf(dto.getLatitude())
                        : null)
                .longitude(
                        dto.getLongitude() != null && !dto.getLongitude().isEmpty() ? Double.valueOf(dto.getLongitude())
                                : null)
                .description(
                        dto.getDescription() != null && !dto.getDescription().isEmpty() ? dto.getDescription().trim()
                                : null)
                .build();
    }

}
