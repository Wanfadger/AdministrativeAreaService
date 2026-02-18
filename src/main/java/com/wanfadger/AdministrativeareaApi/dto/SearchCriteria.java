package com.wanfadger.AdministrativeareaApi.dto;

import com.wanfadger.AdministrativeareaApi.enums.MatchType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchCriteria {
    private String key;
    private Object value;
    private MatchType matchType;
}
