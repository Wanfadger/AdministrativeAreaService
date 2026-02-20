package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.Region;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long>, JpaSpecificationExecutor<Region> {

    Optional<Region> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    Optional<Region> findByCodeIgnoreCase(String code);

    List<Region> findByCodeIgnoreCaseIn(List<String> codes);

    boolean existsByCodeIgnoreCase(String code);

}
