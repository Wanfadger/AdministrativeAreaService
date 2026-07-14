package com.wanfadger.AdministrativeareaApi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * An area without its ancestry — just {@code partOfCode} pointing at the immediate parent.
 *
 * <p>Returned only for {@code ?view=flat}. The default nested shape is unchanged, so this is
 * purely additive: existing clients never see it.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Administrative area with only its parent's code, instead of the full nested "
        + "ancestry. Returned when the request carries ?view=flat.")
public class FlatAreaDTO extends AdministrativeAreaDTO {

    @Schema(description = "Code of the immediate parent area. Null for REGION, which has no parent.")
    private String partOfCode;
}
