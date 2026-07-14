package com.wanfadger.AdministrativeareaApi.service.handler;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.LocalGovernmentRepository;
import com.wanfadger.AdministrativeareaApi.repository.RegionRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubRegionRepository;
import com.wanfadger.AdministrativeareaApi.repository.specification.SpecificationBuilder;
import com.wanfadger.AdministrativeareaApi.service.mapper.AreaDtoMapper;
import com.wanfadger.AdministrativeareaApi.service.query.View;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SubRegionHandler extends AreaHandler<SubRegion, Region> {

    private final SubRegionRepository subRegions;
    private final RegionRepository regions;
    private final LocalGovernmentRepository localGovernments;

    public SubRegionHandler(SubRegionRepository subRegions, RegionRepository regions,
                            LocalGovernmentRepository localGovernments) {
        super(subRegions);
        this.subRegions = subRegions;
        this.regions = regions;
        this.localGovernments = localGovernments;
    }

    @Override public AdministrativeAreaType type() { return AdministrativeAreaType.SUBREGION; }
    @Override protected SubRegion newEntity() { return new SubRegion(); }

    @Override protected Region resolveParent(String partOfCode) {
        return regions.findByCodeIgnoreCase(partOfCode)
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + partOfCode));
    }

    @Override protected void attachParent(SubRegion entity, Region parent) { entity.setRegion(parent); }
    @Override protected String parentCodePath() { return "region.code"; }
    @Override protected String parentCodeOf(SubRegion entity) { return entity.getRegion().getCode(); }

    @Override protected Optional<SubRegion> findDuplicate(String name, String partOfCode) {
        return subRegions.findByNameIgnoreCaseAndRegion_Code(name, partOfCode);
    }

    @Override protected boolean hasChildren(String code) {
        return localGovernments.existsBySubRegion_Code(code);
    }

    @Override protected AdministrativeAreaDTO toNestedDto(SubRegion entity) {
        return AreaDtoMapper.subRegion(entity);
    }

    /** Region is the only ancestor, so both views join the same single association. */
    @Override protected Specification<SubRegion> fetchChain(View view) {
        return SpecificationBuilder.fetch("region");
    }

    @Override protected String missingParentMessage() { return "Missing PartOfCode(region) for the sub region"; }
    @Override protected String duplicateOnCreateMessage() { return "Sub region Already Exists in the region"; }
    @Override protected String duplicateOnUpdateMessage() { return "Sub region already exists in the region"; }
    @Override protected String notFoundMessage() { return "SubRegion not found"; }
    @Override protected String hasChildrenMessage() { return "Cannot delete a sub-region that still has local governments"; }
}
