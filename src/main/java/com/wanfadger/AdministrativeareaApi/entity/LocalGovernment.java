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
@Table(indexes = {
        @Index(name = "idx_localgovernment_code", columnList = "code"),
        @Index(name = "idx_localgovernment_name", columnList = "name")
})
@SuperBuilder
public class LocalGovernment extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SubRegion.class)
    @JoinColumn(nullable = false)
    private SubRegion subRegion;

    @Builder.Default
    @OneToMany(mappedBy = "localGovernment", cascade = CascadeType.PERSIST, targetEntity = County.class)
    private List<County> counties = new ArrayList<>();

    public LocalGovernment(Long id) {
        super(id);
    }
}
