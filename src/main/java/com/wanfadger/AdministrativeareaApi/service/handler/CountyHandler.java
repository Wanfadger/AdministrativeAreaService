package com.wanfadger.AdministrativeareaApi.service.handler;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.LocalGovernmentRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubCountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.specification.SpecificationBuilder;
import com.wanfadger.AdministrativeareaApi.service.mapper.AreaDtoMapper;
import com.wanfadger.AdministrativeareaApi.service.query.View;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CountyHandler extends AreaHandler<County, LocalGovernment> {

    private final CountyRepository counties;
    private final LocalGovernmentRepository localGovernments;
    private final SubCountyRepository subCounties;

    public CountyHandler(CountyRepository counties, LocalGovernmentRepository localGovernments,
                         SubCountyRepository subCounties) {
        super(counties);
        this.counties = counties;
        this.localGovernments = localGovernments;
        this.subCounties = subCounties;
    }

    @Override public AdministrativeAreaType type() { return AdministrativeAreaType.COUNTY; }
    @Override protected County newEntity() { return new County(); }

    @Override protected LocalGovernment resolveParent(String partOfCode) {
        return localGovernments.findByCodeIgnoreCase(partOfCode)
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + partOfCode));
    }

    @Override protected void attachParent(County entity, LocalGovernment parent) { entity.setLocalGovernment(parent); }
    @Override protected String parentCodePath() { return "localGovernment.code"; }
    @Override protected String parentCodeOf(County entity) { return entity.getLocalGovernment().getCode(); }

    @Override protected Optional<County> findDuplicate(String name, String partOfCode) {
        return counties.findByNameIgnoreCaseAndLocalGovernment_Code(name, partOfCode);
    }

    @Override protected boolean hasChildren(String code) {
        return subCounties.existsByCounty_Code(code);
    }

    @Override protected AdministrativeAreaDTO toNestedDto(County entity) {
        return AreaDtoMapper.county(entity);
    }

    @Override protected Specification<County> fetchChain(View view) {
        return view == View.FLAT
                ? SpecificationBuilder.fetch("localGovernment")
                : SpecificationBuilder.fetch("localGovernment", "subRegion", "region");
    }

    @Override protected String missingParentMessage() { return "Missing PartOfCode(local government) for county"; }
    @Override protected String duplicateOnCreateMessage() { return "County Already Exists in the local government"; }
    @Override protected String duplicateOnUpdateMessage() { return "County already exists in the local government"; }
    @Override protected String notFoundMessage() { return "County not found"; }
    @Override protected String hasChildrenMessage() { return "Cannot delete a county that still has sub-counties"; }
}
