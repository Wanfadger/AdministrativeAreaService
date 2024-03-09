package com.wanfadger.AdministrativeareaApi.shared.reponses;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdministrativeAreaTokenDto {
    private String token;
    private String expiredAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
   public static class SiipTokenRequestDto{
        @NotBlank(message = "email is mandatory")
        private String email;
    }
}
