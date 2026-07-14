package com.wanfadger.AdministrativeareaApi.service.handler;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.ParishRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubCountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.specification.SpecificationBuilder;
import com.wanfadger.AdministrativeareaApi.service.mapper.AreaDtoMapper;
import com.wanfadger.AdministrativeareaApi.service.query.View;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SubCountyHandler extends AreaHandler<SubCounty, County> {

    private final SubCountyRepository subCounties;
    private final CountyRepository counties;
    private final ParishRepository parishes;

    public SubCountyHandler(SubCountyRepository subCounties, CountyRepository counties,
                            ParishRepository parishes) {
        super(subCounties);
        this.subCounties = subCounties;
        this.counties = counties;
        this.parishes = parishes;
    }

    @Override public AdministrativeAreaType type() { return AdministrativeAreaType.SUBCOUNTY; }
    @Override protected SubCounty newEntity() { return new SubCounty(); }

    /** NOTE: SUBCOUNTY alone omits the offending code from this message. Preserved verbatim. */
    @Override protected County resolveParent(String partOfCode) {
        return counties.findByCodeIgnoreCase(partOfCode)
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode"));
    }

    @Override protected void attachParent(SubCounty entity, County parent) { entity.setCounty(parent); }
    @Override protected String parentCodePath() { return "county.code"; }
    @Override protected String parentCodeOf(SubCounty entity) { return entity.getCounty().getCode(); }

    /**
     * The bug that started this refactor: {@code createOne} called
     * {@code findByNameIgnoreCaseAndCounty_Id(name, partOfCode)} — a CODE passed to an ID
     * parameter. It compiled, never matched, and duplicate sub-counties were created with a 201.
     * There is now ONE duplicate check for all six levels, so create and update cannot diverge.
     */
    @Override protected Optional<SubCounty> findDuplicate(String name, String partOfCode) {
        return subCounties.findByNameIgnoreCaseAndCounty_Code(name, partOfCode);
    }

    @Override protected boolean hasChildren(String code) {
        return parishes.existsBySubCounty_Code(code);
    }

    @Override protected AdministrativeAreaDTO toNestedDto(SubCounty entity) {
        return AreaDtoMapper.subCounty(entity);
    }

    @Override protected Specification<SubCounty> fetchChain(View view) {
        return view == View.FLAT
                ? SpecificationBuilder.fetch("county")
                : SpecificationBuilder.fetch("county", "localGovernment", "subRegion", "region");
    }

    @Override protected String missingParentMessage() { return "Missing PartOfCode(county) for sub county"; }
    @Override protected String duplicateOnCreateMessage() { return "Sub County Already Exists in the county"; }
    @Override protected String duplicateOnUpdateMessage() { return "Sub County already exists in the county"; }
    @Override protected String notFoundMessage() { return "SubCounty not found"; }
    @Override protected String hasChildrenMessage() { return "Cannot delete a sub-county that still has parishes"; }
}
