package com.wanfadger.AdministrativeareaApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wanfadger.AdministrativeareaApi.entity.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
//@JsonIgnoreProperties(ignoreUnknown = true)
public class AdministrativeAreaExcelDto {

    @JsonProperty("REGION")
    private String region;
    private Region dbRegion;

    @JsonProperty("SUB_REGION")
    private String subRegion;
    private SubRegion dbSubRegion;

    @JsonProperty("LOCAL_GOVERNMENT")
    private String localGovernment;
    private LocalGovernment dbLocalGovernment;

    @JsonProperty("COUNTY")
    private String county;
    private County dbCounty;

    @JsonProperty("SUB_COUNTY")
    private String subCounty;
    private SubCounty dbSubCounty;

    @JsonProperty("PARISH")
    private String parish;
    private Parish dbParish;

    // upload level per get codes, then upload again


}
