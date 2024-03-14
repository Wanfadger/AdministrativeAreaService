package com.wanfadger.AdministrativeareaApi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public   class UpdateAdministrativeAreaDto extends NewAdministrativeAreaDto {
    private String code;
}
