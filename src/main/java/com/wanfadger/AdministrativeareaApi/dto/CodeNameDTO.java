package com.wanfadger.AdministrativeareaApi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Simplified administrative area data containing only code and name")
public class CodeNameDTO implements Serializable {
    @Schema(description = "Unique code of the administrative area", example = "001")
    private String code;
    
    @Schema(description = "Name of the administrative area", example = "Central Region")
    private String name;
}
