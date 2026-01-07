package com.wanfadger.AdministrativeareaApi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
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
     * 
     * Indexes are defined at entity level using @Table(indexes = ...) on each entity class.
     * This gives entity-level annotations priority and ensures table-specific index names
     * (e.g., idx_region_code, idx_subregion_code) to avoid naming conflicts.
     * 
     * Flyway migration V1__add_performance_indexes.sql uses IF NOT EXISTS to safely
     * handle cases where Hibernate already created the indexes, ensuring idempotency.
     */
    @Column(unique = true, nullable = false)
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
