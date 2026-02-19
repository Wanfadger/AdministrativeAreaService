package com.wanfadger.AdministrativeareaApi.service.parish;

import org.springframework.stereotype.Service;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.ParishDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.Parish;
import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.service.subcounty.SubCountyMapperService;
import com.wanfadger.AdministrativeareaApi.shared.SharedService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParishMapperService {

    private final SharedService sharedService;
    private final SubCountyMapperService subCountyMapperService;

    public ParishDTO toDTO(Parish parish) {
        return ParishDTO.builder()
                .code(parish.getCode())
                .name(parish.getName())
                .latitude(parish.getLatitude() != null ? String.valueOf(parish.getLatitude()) : "")
                .longitude(parish.getLongitude() != null ? String.valueOf(parish.getLongitude()) : "")
                .description(parish.getDescription() != null ? parish.getDescription().trim() : null)
                .build();
    }

    public ParishDTO toDetailDTO(Parish parish) {
        ParishDTO dto = toDTO(parish);
        if (parish.getSubCounty() != null) {
            dto.setSubCounty(subCountyMapperService.toDetailDTO(parish.getSubCounty()));
        }
        return dto;
    }

    public Parish toParish(NewAdministrativeAreaDTO dto, SubCounty subCounty) {
        return Parish.builder()
                .code(sharedService.generateCode(AdministrativeAreaType.PARISH))
                .name(dto.getName().trim())
                .subCounty(subCounty)
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
