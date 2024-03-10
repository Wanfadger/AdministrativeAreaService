package com.wanfadger.AdministrativeareaApi.entity;

import jakarta.persistence.*;
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
public class SubRegion extends BaseEntity {

    @Column(nullable = false)
    private String partOfCode;

    @ManyToOne(targetEntity = Region.class ,cascade = CascadeType.PERSIST)
    private Region region;

    @OneToMany(mappedBy = "subRegion" , targetEntity = LocalGovernment.class ,cascade = CascadeType.PERSIST)
    private List<LocalGovernment> localGovernments = new ArrayList<>();

    public SubRegion(String id) {
        super(id);
    }
}