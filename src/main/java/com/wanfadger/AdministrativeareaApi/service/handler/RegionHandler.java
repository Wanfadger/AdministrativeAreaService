package com.wanfadger.AdministrativeareaApi.service.handler;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.repository.RegionRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubRegionRepository;
import com.wanfadger.AdministrativeareaApi.service.mapper.AreaDtoMapper;
import com.wanfadger.AdministrativeareaApi.service.query.View;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Root of the hierarchy: no parent, so {@code P} is {@link Void}. */
@Component
public class RegionHandler extends AreaHandler<Region, Void> {

    private final RegionRepository regions;
    private final SubRegionRepository subRegions;

    public RegionHandler(RegionRepository regions, SubRegionRepository subRegions) {
        super(regions);
        this.regions = regions;
        this.subRegions = subRegions;
    }

    @Override public AdministrativeAreaType type() { return AdministrativeAreaType.REGION; }
    @Override protected Region newEntity() { return new Region(); }
    @Override protected boolean requiresParent() { return false; }

    @Override protected Void resolveParent(String partOfCode) { return null; }
    @Override protected void attachParent(Region entity, Void parent) { /* no parent */ }
    @Override protected String parentCodePath() { return null; }
    @Override protected String parentCodeOf(Region entity) { return null; }

    /** Globally scoped, correctly: a region has no parent to scope uniqueness to. */
    @Override protected Optional<Region> findDuplicate(String name, String partOfCode) {
        return regions.findByNameIgnoreCase(name);
    }

    @Override protected boolean hasChildren(String code) {
        return subRegions.existsByRegion_Code(code);
    }

    @Override protected AdministrativeAreaDTO toNestedDto(Region entity) {
        return AreaDtoMapper.region(entity);
    }

    /** No ancestors to fetch, at either view: an AND-neutral predicate. */
    @Override protected Specification<Region> fetchChain(View view) {
        return (root, query, cb) -> cb.conjunction();
    }

    // ---- messages (verbatim) ----
    @Override protected String missingParentMessage() { return "Missing PartOfCode"; }
    @Override protected String duplicateOnCreateMessage() { return "Administrative Area Already Exists"; }
    @Override protected String duplicateOnUpdateMessage() { return "Administrative Area Already Exists"; }
    @Override protected String notFoundMessage() { return "Region not found"; }
    @Override protected String hasChildrenMessage() { return "Cannot delete a region that still has sub-regions"; }

    /** REGION alone reports a missing area on UPDATE as a 400, not a 404. Preserved deliberately. */
    @Override protected RuntimeException notFoundOnUpdate() {
        return new InvalidException("Invalid PartOfCode");
    }
}
