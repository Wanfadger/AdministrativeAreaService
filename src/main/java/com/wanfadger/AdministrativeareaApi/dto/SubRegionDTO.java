package com.wanfadger.AdministrativeareaApi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SubRegionDTO extends AdministrativeAreaDTO {
    private RegionDTO region;
}
