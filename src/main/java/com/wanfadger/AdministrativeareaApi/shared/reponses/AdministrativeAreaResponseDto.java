package com.wanfadger.AdministrativeareaApi.shared.reponses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdministrativeAreaResponseDto<T> implements Serializable {
    private T data;
    private String message;
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
