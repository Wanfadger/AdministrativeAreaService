package com.wanfadger.AdministrativeareaApi.entity;

import org.hibernate.annotations.SQLRestriction;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(indexes = {
    @Index(name = "idx_parish_code", columnList = "code"),
    @Index(name = "idx_parish_name", columnList = "name"),
    @Index(name = "idx_parish_archived", columnList = "archived")
})
@SQLRestriction("archived = false")
public class Parish extends BaseEntity implements NamedArea {

    @Column(nullable = false)
    private String name;

    @ManyToOne(targetEntity = SubCounty.class , fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private SubCounty subCounty;


    public Parish(String id) {
        super(id);
    }
}
