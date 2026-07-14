package com.wanfadger.AdministrativeareaApi.service.handler;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.Parish;
import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.repository.ParishRepository;
import com.wanfadger.AdministrativeareaApi.repository.SubCountyRepository;
import com.wanfadger.AdministrativeareaApi.repository.specification.SpecificationBuilder;
import com.wanfadger.AdministrativeareaApi.service.mapper.AreaDtoMapper;
import com.wanfadger.AdministrativeareaApi.service.query.View;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** The leaf: no children, so it can always be deleted. */
@Component
public class ParishHandler extends AreaHandler<Parish, SubCounty> {

    private final ParishRepository parishes;
    private final SubCountyRepository subCounties;

    public ParishHandler(ParishRepository parishes, SubCountyRepository subCounties) {
        super(parishes);
        this.parishes = parishes;
        this.subCounties = subCounties;
    }

    @Override public AdministrativeAreaType type() { return AdministrativeAreaType.PARISH; }
    @Override protected Parish newEntity() { return new Parish(); }

    @Override protected SubCounty resolveParent(String partOfCode) {
        return subCounties.findByCodeIgnoreCase(partOfCode)
                .orElseThrow(() -> new InvalidException("Invalid PartOfCode: " + partOfCode));
    }

    @Override protected void attachParent(Parish entity, SubCounty parent) { entity.setSubCounty(parent); }
    @Override protected String parentCodePath() { return "subCounty.code"; }
    @Override protected String parentCodeOf(Parish entity) { return entity.getSubCounty().getCode(); }

    @Override protected Optional<Parish> findDuplicate(String name, String partOfCode) {
        return parishes.findByNameIgnoreCaseAndSubCounty_Code(name, partOfCode);
    }

    @Override protected boolean hasChildren(String code) { return false; }
    @Override protected String hasChildrenMessage() { return null; }

    @Override protected AdministrativeAreaDTO toNestedDto(Parish entity) {
        return AreaDtoMapper.parish(entity);
    }

    /**
     * The hottest path in the app: the map view pulls ~2,000 parishes at once. NESTED join-fetches
     * five ancestors; FLAT needs only the sub-county, dropping a six-table join to two.
     */
    @Override protected Specification<Parish> fetchChain(View view) {
        return view == View.FLAT
                ? SpecificationBuilder.fetch("subCounty")
                : SpecificationBuilder.fetch("subCounty", "county", "localGovernment", "subRegion", "region");
    }

    @Override protected String missingParentMessage() { return "Missing PartOfCode(sub county) for parish"; }
    @Override protected String duplicateOnCreateMessage() { return "Parish Already Exists in the sub county"; }
    @Override protected String duplicateOnUpdateMessage() { return "Parish already exists in the sub county"; }
    @Override protected String notFoundMessage() { return "Parish not found"; }
}
