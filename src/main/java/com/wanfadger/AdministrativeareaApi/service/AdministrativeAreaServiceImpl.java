package com.wanfadger.AdministrativeareaApi.service;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.beanConfig.CacheValueKeyConfig;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.service.handler.AreaHandler;
import com.wanfadger.AdministrativeareaApi.service.handler.AreaHandlerRegistry;
import com.wanfadger.AdministrativeareaApi.service.query.AreaQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Administrative-area service (Region › Sub-Region › Local Government › County › Sub-County › Parish).
 *
 * <p>This used to be 627 lines in which every method was a six-branch switch, each branch a copy of
 * the other five. All six levels now share one code path per operation
 * ({@link AreaHandler}); this class only dispatches.
 *
 * <p><b>Entity → DTO mapping happens inside these transactional methods, and must stay there.</b>
 * Mapping walks lazy associations, and with {@code open-in-view=false} doing it anywhere above the
 * transaction throws {@code LazyInitializationException}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdministrativeAreaServiceImpl implements AdministrativeAreaService {

    private final AreaHandlerRegistry registry;
    private final AreaQueryFactory queryFactory;
    private final NewAreaValidator newAreaValidator;

    private static AdministrativeAreaType requireType(Map<String, String> queryMap) {
        return AdministrativeAreaType.fromStr(queryMap.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing Administrative Area Type"));
    }

    private static String requireCode(Map<String, String> queryMap) {
        String code = queryMap.get("code");
        if (code == null || code.isEmpty()) {
            throw new MissingDataException("Missing Administrative Area Code");
        }
        return code;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH, allEntries = true)
    })
    public ResponseDTO<List<String>> create(Map<String, String> queryMap, List<NewAdministrativeAreaDTO> dtos) {
        AdministrativeAreaType type = requireType(queryMap);
        if (dtos == null || dtos.isEmpty()) {
            throw new MissingDataException("Request body must contain at least one administrative area");
        }
        // Validated here, not via @Valid on the controller: @Valid on a List body validates the
        // ArrayList itself (which has no constraints), so every element would sail through.
        newAreaValidator.validateForCreate(dtos);

        AreaHandler<?, ?> handler = registry.get(type);
        List<String> codes = dtos.stream().map(handler::create).toList();
        return new ResponseDTO<>(codes, "successfully created " + codes.size() + " administrative area(s)");
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH, allEntries = true)
    })
    public ResponseDTO<String> updateOne(Map<String, String> queryMap, NewAdministrativeAreaDTO dto) {
        AdministrativeAreaType type = requireType(queryMap);
        registry.get(type).update(requireCode(queryMap), dto);
        return new ResponseDTO<>("SUCCESS");
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS, allEntries = true),
            @CacheEvict(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH, allEntries = true)
    })
    public ResponseDTO<String> delete(Map<String, String> queryMap) {
        String code = requireCode(queryMap);
        AdministrativeAreaType type = requireType(queryMap);
        registry.get(type).delete(code);
        return new ResponseDTO<>("SUCCESS", "Administrative area deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS, keyGenerator = "searchKeyGenerator")
    public ResponseDTO<AdministrativeAreaDTO> getOne(Map<String, String> queryMap) {
        if (queryMap.get("code") == null) {
            throw new MissingDataException("Missing required data");
        }
        AdministrativeAreaType type = requireType(queryMap);
        return new ResponseDTO<>(registry.get(type).getOne(queryMap.get("code")));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheValueKeyConfig.ADMINISTRATIVE_AREAS_SEARCH, keyGenerator = "searchKeyGenerator")
    public PaginatedResponseDTO<AdministrativeAreaDTO> search(Map<String, String> queryMap) {
        var query = queryFactory.toQuery(queryMap);
        return registry.get(query.type()).search(query);
    }
}
