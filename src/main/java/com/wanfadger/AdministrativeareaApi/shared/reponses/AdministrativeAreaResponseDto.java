package com.wanfadger.AdministrativeareaApi.shared.reponses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Standard API response wrapper")
public class AdministrativeAreaResponseDto<T> implements Serializable {
    @Schema(description = "Response data payload", example = "Success or data object")
    private T data;
    
    @Schema(description = "Response message", example = "success")
    private String message;
    
    @Schema(description = "Operation status", example = "true")
    private boolean status;

    public AdministrativeAreaResponseDto(T data, String message) {
        this.data = data;
        this.message = message;
        this.status = true;
    }

    public AdministrativeAreaResponseDto(T data) {
        this.data = data;
        this.message = "success";
        this.status = true;
    }
}
