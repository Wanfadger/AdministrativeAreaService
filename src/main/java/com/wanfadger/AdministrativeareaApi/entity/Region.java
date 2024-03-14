package com.wanfadger.AdministrativeareaApi.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Region extends BaseEntity {

    @Column(unique = true , nullable = false)
    private String name;

    @OneToMany(mappedBy = "region" , targetEntity = SubRegion.class ,cascade = CascadeType.PERSIST)
    private List<SubRegion> subRegions = new ArrayList<>();

    public Region(String id) {
        super(id);
    }
}
