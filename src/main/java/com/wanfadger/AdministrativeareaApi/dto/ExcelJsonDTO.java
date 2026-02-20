package com.wanfadger.AdministrativeareaApi.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExcelJsonDTO implements Serializable {
    private String region;
    private String subRegion;
    private String localGovernment;
    private String county;
    private String subCounty;
    private String parish;
}
