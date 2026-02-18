package com.wanfadger.AdministrativeareaApi.dto.uniqueDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UParish {
    @NonNull
    private String name;
    @NonNull
    private Long id;
}
