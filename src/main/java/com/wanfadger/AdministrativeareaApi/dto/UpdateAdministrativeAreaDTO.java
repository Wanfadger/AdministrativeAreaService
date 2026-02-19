package com.wanfadger.AdministrativeareaApi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAdministrativeAreaDTO extends NewAdministrativeAreaDTO {
    @NotBlank(message = "Code is required")
    private String code;
}
