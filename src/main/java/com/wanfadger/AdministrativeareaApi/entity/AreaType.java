package com.wanfadger.AdministrativeareaApi.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum AreaType {
    REGION("REGION") ,//REGION
    SUBREGION("SUB REGION"),//SUB-REGION
    LOCALGOVERNMENT("LOCAL GOVERNMENT"), //DISTRICT/LOCAL GOVERNMENT ,
    COUNTY("COUNTY"), //COUNTY/CONSTITUENCY/MUNICIPALITY
    SUBCOUNTY("SUB COUNTY"), //SUB COUNTY/TOWN COUNCIL/DIVISION
    PARISH("PARISH") //PARISH/WARD
    ;

    private final String areaType;

 

    public static Optional<AreaType> fromStr(String areaTypeStr) {
        return Arrays.stream(AreaType.values()).filter(type -> type.areaType.equalsIgnoreCase(areaTypeStr)).findFirst();
    }


}
