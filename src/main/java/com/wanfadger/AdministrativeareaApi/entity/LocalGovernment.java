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
public class LocalGovernment extends BaseEntity {

    @Column(unique = true , nullable = false)
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
