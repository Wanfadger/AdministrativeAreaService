package com.wanfadger.AdministrativeareaApi.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private Double latitude;
    private Double longitude;

    private String description;

    @Column(name = "area_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AdministrativeAreaType areaType;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false)
    @Schema(description = "Date and time when the area was created", example = "2024-03-27T14:30:00")
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "updated_date_time")
    @Schema(description = "Date and time when the area was last updated", example = "2024-03-27T14:30:00")
    private LocalDateTime updatedDateTime;

    public BaseEntity(Long id) {
        this.id = id;
    }

}
