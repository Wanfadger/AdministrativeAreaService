package com.wanfadger.AdministrativeareaApi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "parish", indexes = {
        @Index(name = "idx_parish_code", columnList = "code"),
        @Index(name = "idx_parish_name", columnList = "name")
})
@SuperBuilder
public class Parish extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(targetEntity = SubCounty.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_county_id", nullable = false)
    private SubCounty subCounty;

    public Parish(Long id) {
        super(id);
    }
}
