package com.wanfadger.AdministrativeareaApi.service.mapper;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.CountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.FlatAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.LocalGovernmentDTO;
import com.wanfadger.AdministrativeareaApi.dto.ParishDTO;
import com.wanfadger.AdministrativeareaApi.dto.RegionDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubCountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubRegionDTO;
import com.wanfadger.AdministrativeareaApi.entity.BaseEntity;
import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.entity.NamedArea;
import com.wanfadger.AdministrativeareaApi.entity.Parish;
import com.wanfadger.AdministrativeareaApi.entity.Region;
import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.entity.SubRegion;

/**
 * Entity → DTO mapping for all six levels.
 *
 * <p>Replaces six copy-pasted mappers. The shared columns are written once by {@link #fill}, whose
 * intersection-type bound {@code <E extends BaseEntity & NamedArea>} is what lets one method touch
 * both the base columns and the name.
 *
 * <p><b>Must run inside the transaction.</b> The ancestor walks below traverse lazy associations;
 * with {@code open-in-view=false} they would throw {@code LazyInitializationException} anywhere
 * outside the {@code @Transactional} service. That is why mapping stays in the service and not in
 * the caching facade that sits above it.
 */
public final class AreaDtoMapper {

    private AreaDtoMapper() {
    }

    /**
     * Shared columns.
     *
     * <p>Coordinates are absent as {@code null}, NOT as {@code ""} and NOT as {@code 0}. Zero is a
     * legitimate coordinate — the equator and the prime meridian — so collapsing "unknown" onto it
     * would make an unset area claim to sit off the coast of Ghana. (Every one of the 5,443
     * restored areas currently has null coordinates.)
     */
    private static <E extends BaseEntity & NamedArea, D extends AdministrativeAreaDTO> D fill(D dto, E e) {
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setLatitude(e.getLatitude() != null ? String.valueOf(e.getLatitude()) : null);
        dto.setLongitude(e.getLongitude() != null ? String.valueOf(e.getLongitude()) : null);
        return dto;
    }

    // ---------------------------------------------------------------- nested (default)

    public static RegionDTO region(Region r) {
        return fill(new RegionDTO(), r);
    }

    public static SubRegionDTO subRegion(SubRegion s) {
        SubRegionDTO dto = fill(new SubRegionDTO(), s);
        dto.setRegion(region(s.getRegion()));
        return dto;
    }

    public static LocalGovernmentDTO localGovernment(LocalGovernment l) {
        LocalGovernmentDTO dto = fill(new LocalGovernmentDTO(), l);
        dto.setSubRegion(subRegion(l.getSubRegion()));
        return dto;
    }

    public static CountyDTO county(County c) {
        CountyDTO dto = fill(new CountyDTO(), c);
        dto.setLocalGovernment(localGovernment(c.getLocalGovernment()));
        return dto;
    }

    public static SubCountyDTO subCounty(SubCounty s) {
        SubCountyDTO dto = fill(new SubCountyDTO(), s);
        dto.setCounty(county(s.getCounty()));
        return dto;
    }

    public static ParishDTO parish(Parish p) {
        ParishDTO dto = fill(new ParishDTO(), p);
        dto.setSubCounty(subCounty(p.getSubCounty()));
        return dto;
    }

    // ---------------------------------------------------------------- flat (?view=flat)

    public static <E extends BaseEntity & NamedArea> FlatAreaDTO flat(E e, String partOfCode) {
        FlatAreaDTO dto = fill(new FlatAreaDTO(), e);
        dto.setPartOfCode(partOfCode);
        return dto;
    }
}
