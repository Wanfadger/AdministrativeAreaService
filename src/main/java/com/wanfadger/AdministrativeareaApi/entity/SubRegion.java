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

    @Column(unique = true , nullable = false)
    private String name;

    @ManyToOne(targetEntity = Region.class ,cascade = CascadeType.PERSIST)
    @JoinColumn(nullable = false)
    private Region region;

    @OneToMany(mappedBy = "subRegion" , targetEntity = LocalGovernment.class ,cascade = CascadeType.PERSIST)
    private List<LocalGovernment> localGovernments = new ArrayList<>();

    public SubRegion(String id) {
        super(id);
    }
}
