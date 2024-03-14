package com.wanfadger.AdministrativeareaApi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CountyDto extends AdministrativeAreaDto {
    private LocalGovernmentDto localGovernment;
}
