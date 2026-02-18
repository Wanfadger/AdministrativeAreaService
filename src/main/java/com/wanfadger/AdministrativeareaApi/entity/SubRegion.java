package com.wanfadger.AdministrativeareaApi.entity;

import jakarta.persistence.*;
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
@Table(name = "sub_region", indexes = {
        @Index(name = "idx_sub_region_code", columnList = "code"),
        @Index(name = "idx_sub_region_name", columnList = "name")
})
@SuperBuilder
public class SubRegion extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToOne(targetEntity = Region.class, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Builder.Default
    @OneToMany(mappedBy = "subRegion", targetEntity = LocalGovernment.class, cascade = CascadeType.PERSIST)
    private List<LocalGovernment> localGovernments = new ArrayList<>();

    public SubRegion(Long id) {
        super(id);
    }
}
