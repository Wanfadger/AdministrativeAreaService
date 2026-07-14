package com.wanfadger.AdministrativeareaApi.repository;

import com.wanfadger.AdministrativeareaApi.entity.Region;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Root of the hierarchy: no parent, so reads need no entity graph. */
@Repository
public interface RegionRepository extends AreaRepository<Region> {

    /** Region names ARE globally unique — a region has no parent to scope uniqueness to. */
    Optional<Region> findByNameIgnoreCase(String name);
}
