package com.wanfadger.AdministrativeareaApi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public   class UpdateAdministrativeAreaDto extends NewAdministrativeAreaDto {
    private String code;
}
