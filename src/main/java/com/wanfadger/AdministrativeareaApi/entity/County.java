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
public class County extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY , targetEntity = LocalGovernment.class)
    @JoinColumn(nullable = false)
    private LocalGovernment localGovernment;

    @OneToMany(mappedBy = "county" , cascade = CascadeType.PERSIST , targetEntity = SubCounty.class)
    private List<SubCounty> subCounties = new ArrayList<>();

    public County(String id) {
        super(id);
    }
}
