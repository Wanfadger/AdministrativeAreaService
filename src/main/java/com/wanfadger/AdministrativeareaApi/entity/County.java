package com.wanfadger.AdministrativeareaApi.entity;

import org.hibernate.annotations.SQLRestriction;

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
@Table(indexes = {
    @Index(name = "idx_county_code", columnList = "code"),
    @Index(name = "idx_county_name", columnList = "name"),
    @Index(name = "idx_county_archived", columnList = "archived")
})
@SQLRestriction("archived = false")
public class County extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY , targetEntity = LocalGovernment.class)
    @JoinColumn(nullable = false)
    private LocalGovernment localGovernment;

    @OneToMany(mappedBy = "county" , cascade = CascadeType.PERSIST , targetEntity = SubCounty.class)
    private List<SubCounty> subCounties = new ArrayList<>();

    public County(String id) {
        super(id);
    }
}
