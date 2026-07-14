package com.wanfadger.AdministrativeareaApi.service.handler;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.LocalGovernmentRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubRegionRepository;
import com.wanfadger.AdministrativeareaApi.repository.specification.SpecificationBuilder;
import com.wanfadger.AdministrativeareaApi.service.mapper.AreaDtoMapper;
import com.wanfadger.AdministrativeareaApi.service.query.View;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LocalGovernmentHandler extends AreaHandler<LocalGovernment, SubRegion> {

    private final LocalGovernmentRepository localGovernments;
    private final SubRegionRepository subRegions;
    private final CountyRepository counties;

    public LocalGovernmentHandler(LocalGovernmentRepository localGovernments,
                                  SubRegionRepository subRegions, CountyRepository counties) {
        super(localGovernments);
        this.localGovernments = localGovernments;
        this.subRegions = subRegions;
        this.counties = counties;
    }

    @Override public AdministrativeAreaType type() { return AdministrativeAreaType.LOCALGOVERNMENT; }
    @Override protected LocalGovernment newEntity() { return new LocalGovernment(); }

    @Override protected SubRegion resolveParent(String partOfCode) {
        return subRegions.findByCodeIgnoreCase(partOfCode)
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + partOfCode));
    }

    @Override protected void attachParent(LocalGovernment entity, SubRegion parent) { entity.setSubRegion(parent); }
    @Override protected String parentCodePath() { return "subRegion.code"; }
    @Override protected String parentCodeOf(LocalGovernment entity) { return entity.getSubRegion().getCode(); }

    @Override protected Optional<LocalGovernment> findDuplicate(String name, String partOfCode) {
        return localGovernments.findByNameIgnoreCaseAndSubRegion_Code(name, partOfCode);
    }

    @Override protected boolean hasChildren(String code) {
        return counties.existsByLocalGovernment_Code(code);
    }

    @Override protected AdministrativeAreaDTO toNestedDto(LocalGovernment entity) {
        return AreaDtoMapper.localGovernment(entity);
    }

    /** FLAT needs only the immediate parent — one join instead of two. */
    @Override protected Specification<LocalGovernment> fetchChain(View view) {
        return view == View.FLAT
                ? SpecificationBuilder.fetch("subRegion")
                : SpecificationBuilder.fetch("subRegion", "region");
    }

    @Override protected String missingParentMessage() { return "Missing PartOfCode(sub region) for localgovernment"; }
    @Override protected String duplicateOnCreateMessage() { return "Local Government Already Exists in the sub region"; }
    @Override protected String duplicateOnUpdateMessage() { return "Local Government already exists in the sub region"; }
    @Override protected String notFoundMessage() { return "LocalGovernment not found"; }
    @Override protected String hasChildrenMessage() { return "Cannot delete a local government that still has counties"; }
}
