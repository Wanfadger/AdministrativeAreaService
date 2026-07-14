package com.wanfadger.AdministrativeareaApi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Write payload for create and update.
 *
 * <p>The same DTO serves both, and update is PARTIAL — only the fields present are applied. So the
 * "required" constraints live in the {@link OnCreate} group rather than the default one; putting a
 * bare {@code @NotBlank} on {@code name} would reject every update that isn't a rename.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for creating or updating an administrative area. On update, only the "
        + "fields you send are applied.")
public class NewAdministrativeAreaDTO {

    @NotBlank(groups = OnCreate.class, message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    @Schema(description = "Name of the administrative area", example = "Central Region",
            requiredMode = RequiredMode.REQUIRED)
    private String name;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    @Schema(description = "Description of the administrative area", example = "Central Region of Uganda")
    private String description;

    @Size(max = 64, message = "partOfCode must be at most 64 characters")
    @Schema(description = "Code of the parent administrative area (required for all types except REGION)",
            example = "001")
    private String partOfCode;

    // An empty string is legal and means ABSENT — the coordinate is then stored as null, never 0
    // (zero is a real coordinate). A non-empty value must be a decimal number.
    @Pattern(regexp = "^$|^[+-]?\\d{1,3}(\\.\\d+)?$", message = "latitude must be a decimal number")
    @Schema(description = "Latitude. Omit or send \"\" when unknown — it is stored as null, not 0.",
            example = "0.3476")
    private String latitude;

    @Pattern(regexp = "^$|^[+-]?\\d{1,3}(\\.\\d+)?$", message = "longitude must be a decimal number")
    @Schema(description = "Longitude. Omit or send \"\" when unknown — it is stored as null, not 0.",
            example = "32.5825")
    private String longitude;
}
