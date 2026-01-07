package com.wanfadger.AdministrativeareaApi.dto.reponses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response wrapper for API responses")
public class ResponseDTO<T> implements Serializable {
    @Schema(description = "Data returned by the API", example = "[]")
    private T data;
    @Schema(description = "Message returned by the API", example = "success")
    private String message;
    @Schema(description = "Status of the API response", example = "true")
    private boolean status;

    public ResponseDTO(T data, String message) {
        this.data = data;
        this.message = message;
        this.status = true;
    }

    public ResponseDTO(T data) {
        this.data = data;
        this.message = "success";
        this.status = true;
    }


}
