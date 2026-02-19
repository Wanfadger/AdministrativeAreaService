package com.wanfadger.AdministrativeareaApi.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum AdministrativeAreaType {
    REGION("REGION"), // REGION
    SUBREGION("SUBREGION"), // SUB-REGION
    LOCALGOVERNMENT("LOCALGOVERNMENT"), // DISTRICT/LOCAL GOVERNMENT ,
    COUNTY("COUNTY"), // COUNTY/CONSTITUENCY/MUNICIPALITY
    SUBCOUNTY("SUBCOUNTY"), // SUB COUNTY/TOWN COUNCIL/DIVISION
    PARISH("PARISH"); // PARISH/WARD

    private final String areaType;

    public static Optional<AdministrativeAreaType> fromStr(String areaTypeStr) {
        return Arrays.stream(AdministrativeAreaType.values())
                .filter(type -> type.areaType.equalsIgnoreCase(areaTypeStr)).findFirst();
    }

}
