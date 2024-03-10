package com.wanfadger.AdministrativeareaApi.entity;


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
public class Parish extends BaseEntity {


    @ManyToOne(targetEntity = SubCounty.class , fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private SubCounty subCounty;


    public Parish(String id) {
        super(id);
    }
}
