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
    @Index(name = "idx_localgovernment_code", columnList = "code"),
    @Index(name = "idx_localgovernment_name", columnList = "name"),
    @Index(name = "idx_localgovernment_archived", columnList = "archived")
})
@SQLRestriction("archived = false")
public class LocalGovernment extends BaseEntity implements NamedArea {

    // NOT globally unique — unique only WITHIN a sub-region. See the note on SubRegion.name.
    // Enforced by uk_localgovernment_name_in_subregion (migration V3).
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY , targetEntity = SubRegion.class )
    @JoinColumn(nullable = false)
    private SubRegion subRegion;

    @OneToMany(mappedBy = "localGovernment" , cascade = CascadeType.PERSIST , targetEntity = County.class)
    private List<County> counties = new ArrayList<>();

    public LocalGovernment(String id) {
        super(id);
    }
}
