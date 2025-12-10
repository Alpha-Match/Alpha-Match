package com.alpha.backend.domain.metadata;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Recruit Metadata Entity
 * 채용 공고의 메타데이터를 저장하는 엔티티
 */
@Entity
@Table(name = "recruit_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "company_name", nullable = false, columnDefinition = "TEXT")
    private String companyName;

    @Column(name = "exp_years", nullable = false)
    private Integer expYears;

    @Column(name = "english_level", columnDefinition = "TEXT")
    private String englishLevel;

    @Column(name = "primary_keyword", columnDefinition = "TEXT")
    private String primaryKeyword;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
