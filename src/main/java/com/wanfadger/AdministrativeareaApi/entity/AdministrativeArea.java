package com.wanfadger.AdministrativeareaApi.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Deprecated
public class AdministrativeArea extends BaseEntity{
    private String name;
    private String code;
//    private AdministrativeAreaType administrativeAreaType;
    private Double latitude;
    private Double longitude;
    private String partOf;//code
}
