package com.wanfadger.AdministrativeareaApi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complete administrative area data with location coordinates")
public class AdministrativeAreaDTO implements Serializable {
    @Schema(description = "Internal unique identifier", example = "1")
    private Long id;

    @Schema(description = "Unique code of the administrative area", example = "001")
    private String code;

    @Schema(description = "Name of the administrative area", example = "Central Region")
    private String name;

    @Schema(description = "Latitude coordinate", example = "0.3476")
    private String latitude;

    @Schema(description = "Longitude coordinate", example = "32.5825")
    private String longitude;
}
