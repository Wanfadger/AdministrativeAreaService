package com.wanfadger.AdministrativeareaApi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(indexes = {
        @Index(name = "idx_subcounty_code", columnList = "code"),
        @Index(name = "idx_subcounty_name", columnList = "name")
})
@SuperBuilder
public class SubCounty extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(targetEntity = County.class, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private County county;

    @Builder.Default
    @OneToMany(mappedBy = "subCounty", cascade = CascadeType.PERSIST, targetEntity = Parish.class)
    private List<Parish> parishes = new ArrayList<>();

    public SubCounty(Long id) {
        super(id);
    }

}
