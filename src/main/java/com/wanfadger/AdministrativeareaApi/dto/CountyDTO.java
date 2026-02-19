package com.wanfadger.AdministrativeareaApi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CountyDTO extends AdministrativeAreaDTO {
    private LocalGovernmentDTO localGovernment;
}
