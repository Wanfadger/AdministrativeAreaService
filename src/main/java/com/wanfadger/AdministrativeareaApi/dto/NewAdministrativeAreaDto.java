package com.wanfadger.AdministrativeareaApi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public   class NewAdministrativeAreaDto{
    private String name;
    private String partOfCode;
    private String latitude;
    private String longitude;
}
