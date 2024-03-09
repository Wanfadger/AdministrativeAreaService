package com.wanfadger.AdministrativeareaApi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class AdministrativeAreaDtos {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
  public  static class NewAdministrativeAreaDto{
        private String name;
        private String partOf;
        private String administrativeAreaType;
        private String latitude;
        private String longitude;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdministrativeAreaDto{
        private String code;
        private String name;
        private String partOf;
        private String latitude;
        private String longitude;
    }

}
