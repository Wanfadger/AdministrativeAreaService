package com.wanfadger.AdministrativeareaApi.service.localgovernment;

import org.springframework.stereotype.Service;

import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.service.subRegion.SubRegionMapperService;
import com.wanfadger.AdministrativeareaApi.shared.SharedService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalGovernmentMapperService {

    private final SharedService sharedService;
    private final SubRegionMapperService subRegionMapperService;

    public LocalGovernmentDTO toDTO(LocalGovernment localGovernment) {
        return LocalGovernmentDTO.builder()
                .code(localGovernment.getCode())
                .name(localGovernment.getName())
                .description(localGovernment.getDescription())
                .longitude(localGovernment.getLongitude() != null ? String.valueOf(localGovernment.getLongitude()) : "")
                .latitude(localGovernment.getLatitude() != null ? String.valueOf(localGovernment.getLatitude()) : "")
                .build();
    }

      public LocalGovernmentDTO toDetailDTO(LocalGovernment localGovernment) {
        LocalGovernmentDTO dto = toDTO(localGovernment);
        if (localGovernment.getSubRegion() != null) {
            dto.setSubRegion(subRegionMapperService.toDetailDTO(localGovernment.getSubRegion()));
        }
        return dto;
    }

   


     public LocalGovernment toLocalGovernment(NewAdministrativeAreaDTO dto, SubRegion subRegion) {
        return LocalGovernment.builder()
                .code(sharedService.generateCode(AdministrativeAreaType.LOCALGOVERNMENT))
                .name(dto.getName().trim())
                .subRegion(subRegion)
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
