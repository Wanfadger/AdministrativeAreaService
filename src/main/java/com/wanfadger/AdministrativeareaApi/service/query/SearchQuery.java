package com.wanfadger.AdministrativeareaApi.service.query;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * A validated, clamped search request. Produced only by {@link AreaQueryFactory}.
 *
 * @param type     the level being searched
 * @param search   free-text, matched against name/code (may be null)
 * @param partOf   parent code filter (may be null)
 * @param view     nested (default) or flat
 * @param pageable already clamped and with a whitelisted sort
 * @param filters  the surviving {@code field:OPERATOR=value} entries, whitelisted
 * @param page     the 1-BASED page number, echoed back in the response envelope
 * @param size     the clamped page size, echoed back in the response envelope
 */
public record SearchQuery(
        AdministrativeAreaType type,
        String search,
        String partOf,
        View view,
        Pageable pageable,
        Map<String, String> filters,
        int page,
        int size) {
}
