package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.BaseEntity;
import com.wanfadger.AdministrativeareaApi.entity.NamedArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * Common base for the six administrative-area repositories.
 *
 * <p>Binding {@code T} here is what lets a single generic handler call
 * {@code repo.findAll(spec, pageable)} and {@code repo.findByCodeIgnoreCase(code)} without casts —
 * the type is known inside the class that declares it, so the six near-identical service branches
 * collapse into one.
 *
 * <p>Each concrete repository RE-DECLARES {@code findByCodeIgnoreCase} with an {@code @EntityGraph}.
 * That is not redundant: an entity graph cannot be attached to an inherited derived query, so the
 * override is the only place to hang it.
 *
 * <p><b>Note on indexing:</b> Spring Data renders {@code IgnoreCase} as {@code upper(code) = upper(?)},
 * which a plain b-tree on {@code code} cannot serve. Migration V3 adds the functional
 * {@code upper(code)} indexes these queries actually need.
 */
@NoRepositoryBean
public interface AreaRepository<T extends BaseEntity & NamedArea>
        extends JpaRepository<T, String>, JpaSpecificationExecutor<T> {

    Optional<T> findByCodeIgnoreCase(String code);
}
