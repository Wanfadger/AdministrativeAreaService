package com.wanfadger.AdministrativeareaApi.dto;

import com.wanfadger.AdministrativeareaApi.enums.MatchType;
import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchCriteria {
    private String key;
    private Object value;
    private MatchType matchType;
}
