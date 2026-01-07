package com.wanfadger.AdministrativeareaApi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Complete administrative area data with location coordinates")
public class AdministrativeAreaDto implements Serializable {
    @Schema(description = "Unique code of the administrative area", example = "001")
    private String code;
    
    @Schema(description = "Name of the administrative area", example = "Central Region")
    private String name;
    
    @Schema(description = "Latitude coordinate", example = "0.3476")
    private String latitude;
    
    @Schema(description = "Longitude coordinate", example = "32.5825")
    private String longitude;
}

