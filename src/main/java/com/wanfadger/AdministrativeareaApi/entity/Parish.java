package com.wanfadger.AdministrativeareaApi.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Parish extends BaseEntity {

    @Column(nullable = false)
    private String partOfCode;

    @ManyToOne(targetEntity = SubCounty.class , fetch = FetchType.LAZY)
    private SubCounty subCounty;


    public Parish(String id) {
        super(id);
    }
}
