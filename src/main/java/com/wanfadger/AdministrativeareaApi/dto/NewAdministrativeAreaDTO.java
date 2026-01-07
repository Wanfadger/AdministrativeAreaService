package com.wanfadger.AdministrativeareaApi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Data transfer object for creating a new administrative area")
public class NewAdministrativeAreaDTO {
    @Schema(description = "Name of the administrative area", example = "Central Region", requiredMode = RequiredMode.REQUIRED)
    private String name;
    
    @Schema(description = "Description of the administrative area", example = "Central Region of Uganda")
    private String description;
    
    @Schema(description = "Code of the parent administrative area (required for all types except REGION)", example = "001")
    private String partOfCode;
    
    @Schema(description = "Latitude coordinate", example = "0.3476")
    private String latitude;
    
    @Schema(description = "Longitude coordinate", example = "32.5825")
    private String longitude;
}
