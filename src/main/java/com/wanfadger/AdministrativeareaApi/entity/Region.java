package com.wanfadger.AdministrativeareaApi.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "region", indexes = {
        @Index(name = "idx_region_code", columnList = "code"),
        @Index(name = "idx_region_name", columnList = "name")
})
@SuperBuilder
public class Region extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    @Builder.Default
    @OneToMany(mappedBy = "region", targetEntity = SubRegion.class, cascade = CascadeType.PERSIST)
    private List<SubRegion> subRegions = new ArrayList<>();
}
