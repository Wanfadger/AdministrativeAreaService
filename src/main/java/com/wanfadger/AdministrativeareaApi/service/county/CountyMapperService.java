package com.wanfadger.AdministrativeareaApi.service.county;

import org.springframework.stereotype.Service;

import com.wanfadger.AdministrativeareaApi.dto.CountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.service.localgovernment.LocalGovernmentMapperService;
import com.wanfadger.AdministrativeareaApi.shared.SharedService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CountyMapperService {

    private final SharedService sharedService;
    private final LocalGovernmentMapperService localGovernmentMapperService;

    public CountyDTO toDTO(County county) {
        return CountyDTO.builder()
                .code(county.getCode())
                .name(county.getName())
                .latitude(county.getLatitude() != null ? String.valueOf(county.getLatitude()) : "")
                .longitude(county.getLongitude() != null ? String.valueOf(county.getLongitude()) : "")
                .description(county.getDescription() != null ? county.getDescription().trim() : null)
                .build();
    }

    public CountyDTO toDetailDTO(County county) {
        CountyDTO dto = toDTO(county);
        if (county.getLocalGovernment() != null) {
            dto.setLocalGovernment(localGovernmentMapperService.toDetailDTO(county.getLocalGovernment()));
        }
        return dto;
    }

    public County toCounty(NewAdministrativeAreaDTO dto, LocalGovernment localGovernment) {
        return County.builder()
                .code(sharedService.generateCode(AdministrativeAreaType.COUNTY))
                .name(dto.getName().trim())
                .localGovernment(localGovernment)
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
