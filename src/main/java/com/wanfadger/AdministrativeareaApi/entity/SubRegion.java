package com.wanfadger.AdministrativeareaApi.entity;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(indexes = {
    @Index(name = "idx_subregion_code", columnList = "code"),
    @Index(name = "idx_subregion_name", columnList = "name"),
    @Index(name = "idx_subregion_archived", columnList = "archived")
})
@SQLRestriction("archived = false")
public class SubRegion extends BaseEntity implements NamedArea {

    // NOT globally unique: sub-region names are unique only WITHIN a region, which is what the
    // service checks. A global unique here contradicts that check and turns a legitimate create
    // into a raw DataIntegrityViolationException -> 500. Enforced by the parent-scoped partial
    // unique index uk_subregion_name_in_region (migration V3).
    @Column(nullable = false)
    private String name;

    // fetch was omitted, and @ManyToOne defaults to EAGER — so EVERY SubRegion load anywhere in the
    // app silently dragged a Region with it, including on the search path that already join-fetches
    // it. Explicitly LAZY, matching the other five levels. Safe: findByCodeIgnoreCase carries an
    // @EntityGraph on "region", and the search specs join-fetch it.
    @ManyToOne(targetEntity = Region.class, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(nullable = false)
    private Region region;

    @OneToMany(mappedBy = "subRegion" , targetEntity = LocalGovernment.class ,cascade = CascadeType.PERSIST)
    private List<LocalGovernment> localGovernments = new ArrayList<>();

    public SubRegion(String id) {
        super(id);
    }
}
