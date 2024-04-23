package com.wanfadger.AdministrativeareaApi.entity;

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
public class SubCounty extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(targetEntity = County.class , fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private County county;

    @OneToMany(mappedBy = "subCounty" , cascade = CascadeType.PERSIST , targetEntity = Parish.class)
    private List<Parish> parishes = new ArrayList<>();

    public SubCounty(String id) {
        super(id);
    }






}
