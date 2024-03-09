package com.wanfadger.AdministrativeareaApi.entity;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum AdministrativeAreaType {
    REGION("REGION") ,//REGION
    SUBREGION("SUB-REGION"),//SUB-REGION
    LOCALGOVERNMENT("LOCAL GOVERNMENT"), //DISTRICT/LOCAL GOVERNMENT ,
    COUNTY("COUNTY"), //COUNTY/CONSTITUENCY/MUNICIPALITY
    SUBCOUNTY("SUB COUNTY"), //SUB COUNTY/TOWN COUNCIL/DIVISION
    PARISH("PARISH") //PARISH/WARD
    ;

    private String administrativeAreaType;

    AdministrativeAreaType(String administrativeAreaType) {
        this.administrativeAreaType = administrativeAreaType;
    }

    public static Optional<AdministrativeAreaType> administrativeAreaTypeStr(String administrativeAreaTypeStr) {
        Optional<AdministrativeAreaType> optional = Arrays.stream(AdministrativeAreaType.values()).filter(administrativeAreaType -> administrativeAreaType.administrativeAreaType.equalsIgnoreCase(administrativeAreaTypeStr)).findFirst();
        return optional;
    }


}
