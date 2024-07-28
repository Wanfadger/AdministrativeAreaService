package com.wanfadger.AdministrativeareaApi.dto;

import lombok.*;

import java.io.Serializable;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CodeNameDto implements Serializable {
   private String code;
   private String name;
}
