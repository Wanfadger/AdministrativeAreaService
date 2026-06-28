package com.wanfadger.AdministrativeareaApi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Shared base for all administrative-area entities.
 *
 * <p>Soft delete: {@code delete}/{@code deleteAll} issue an UPDATE that sets
 * {@code archived = true} ({@link SQLDelete}, inherited by subclasses). Each concrete entity
 * additionally declares {@code @SQLRestriction("archived = false")} so queries only return
 * non-archived rows — Hibernate does NOT inherit {@code @SQLRestriction} from a
 * {@code @MappedSuperclass}, so it must live on the entities themselves. NOTE: the unique
 * {@code code} constraint still spans archived rows, so a soft-deleted code cannot be reused.
 */
@MappedSuperclass
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE {h-table} SET archived = true WHERE id = ?")
public class BaseEntity {
    @Id
    @UuidGenerator
    private String id;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean archived = false;

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
