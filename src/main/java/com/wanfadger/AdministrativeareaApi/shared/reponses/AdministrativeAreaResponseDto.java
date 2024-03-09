package com.wanfadger.AdministrativeareaApi.shared.reponses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdministrativeAreaResponseDto<T> {
    private T data;
    private String message;
    private boolean status;

    public AdministrativeAreaResponseDto(T data, String message) {
        this.data = data;
        this.message = message;
        this.status = true;
    }


}
