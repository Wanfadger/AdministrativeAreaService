package com.wanfadger.AdministrativeareaApi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BaseEntity {
    @Id
    @UuidGenerator
    private String id;

    /**
     * Unique code identifier for the administrative area.
     * Index is defined here so it's inherited by all entities extending BaseEntity.
     * 
     * For fresh installations: Hibernate's ddl-auto will create this index automatically.
     * For existing installations: Flyway migration V1__add_performance_indexes.sql handles it.
     * 
     * Note: @Index from org.hibernate.annotations is used (deprecated but works for MappedSuperclass).
     * Modern JPA approach would require @Table(indexes = ...) on each entity class.
     */
    @Column(unique = true, nullable = false)
    @Index(name = "idx_code", columnNames = "code")
    private String code;

    private Double latitude;
    private Double longitude;

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    private LocalDateTime updatedDateTime;

    public BaseEntity(String id) {
        this.id = id;
    }

}
