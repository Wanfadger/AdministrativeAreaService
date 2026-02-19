package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalGovernmentRepository
        extends JpaRepository<LocalGovernment, Long>, JpaSpecificationExecutor<LocalGovernment> {

    @Override
    @EntityGraph(attributePaths = { "subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
    @NonNull
    List<LocalGovernment> findAll();

    @Override
    @EntityGraph(attributePaths = { "subRegion.region" }, type = EntityGraph.EntityGraphType.FETCH)
    @NonNull
    Page<LocalGovernment> findAll(@NonNull Pageable pageable);

    Optional<LocalGovernment> findByNameIgnoreCaseAndSubRegion_Code(String name, String code);

    Optional<LocalGovernment> findByCodeIgnoreCase(String code);

    Optional<LocalGovernment> findByCodeIgnoreCaseAndSubRegion_Code(String code, String parentCode);

    boolean existsByNameIgnoreCaseAndSubRegion_Code(String name, String code);

    List<LocalGovernment> findByCodeIgnoreCaseIn(List<String> codes);

}
