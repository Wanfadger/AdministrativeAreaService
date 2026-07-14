package com.wanfadger.AdministrativeareaApi.entity;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
    @Index(name = "idx_region_code", columnList = "code"),
    @Index(name = "idx_region_name", columnList = "name"),
    @Index(name = "idx_region_archived", columnList = "archived")
})
@SQLRestriction("archived = false")
public class Region extends BaseEntity implements NamedArea {

    @Column(unique = true , nullable = false)
    private String name;

    @OneToMany(mappedBy = "region" , targetEntity = SubRegion.class ,cascade = CascadeType.PERSIST)
    private List<SubRegion> subRegions = new ArrayList<>();

    public Region(String id) {
        super(id);
    }
}
