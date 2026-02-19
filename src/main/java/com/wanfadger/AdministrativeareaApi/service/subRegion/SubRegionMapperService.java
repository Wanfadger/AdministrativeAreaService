package com.wanfadger.AdministrativeareaApi.service.subRegion;

import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.service.region.RegionMapperService;
import com.wanfadger.AdministrativeareaApi.shared.SharedService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubRegionMapperService {

        private final SharedService sharedService;
        private final RegionMapperService regionMapperService;

        public SubRegionDTO toDTO(SubRegion subRegion) {
                return SubRegionDTO.builder()
                                .code(subRegion.getCode())
                                .name(subRegion.getName())
                                .latitude(subRegion.getLatitude() != null ? String.valueOf(subRegion.getLatitude())
                                                : null)
                                .longitude(subRegion.getLongitude() != null ? String.valueOf(subRegion.getLongitude())
                                                : null)
                                .description(subRegion.getDescription() != null ? subRegion.getDescription().trim()
                                                : null)
                                .build();
        }

        public SubRegionDTO toDetailDTO(SubRegion subRegion) {
                SubRegionDTO subRegionDTO = toDTO(subRegion);
                if (subRegion.getRegion() != null) {
                        subRegionDTO.setRegion(regionMapperService.toDTO(subRegion.getRegion()));
                }
                return subRegionDTO;
        }

        public SubRegion toSubRegion(NewAdministrativeAreaDTO dto, Region region) {
                return SubRegion.builder()
                                .code(sharedService.generateCode(AdministrativeAreaType.SUBREGION))
                                .name(dto.getName().trim())
                                .region(region)
                                .latitude(dto.getLatitude() != null && !dto.getLatitude().isEmpty()
                                                ? Double.valueOf(dto.getLatitude())
                                                : null)
                                .longitude(
                                                dto.getLongitude() != null && !dto.getLongitude().isEmpty()
                                                                ? Double.valueOf(dto.getLongitude())
                                                                : null)
                                .description(
                                                dto.getDescription() != null && !dto.getDescription().isEmpty()
                                                                ? dto.getDescription().trim()
                                                                : null)
                                .build();
        }
        
}
