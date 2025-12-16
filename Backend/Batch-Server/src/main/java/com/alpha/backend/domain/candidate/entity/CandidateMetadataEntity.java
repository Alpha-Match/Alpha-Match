package com.alpha.backend.domain.candidate.entity;

import com.alpha.backend.domain.common.BaseMetadataEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.UUID;

/**
 * Candidate Metadata Entity
 * 후보자의 메타데이터를 저장하는 엔티티
 */
@Entity
@Table(name = "candidate_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateMetadataEntity extends BaseMetadataEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "skills", columnDefinition = "TEXT[]")
    private String[] skills;  // PostgreSQL 배열 타입

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears;

    @Column(name = "education_level", length = 100)
    private String educationLevel;

    @Column(name = "preferred_location", length = 255)
    private String preferredLocation;

    @Column(name = "expected_salary")
    private Integer expectedSalary;

    @Override
    public String getDomainType() {
        return "candidate";
    }

    /**
     * Builder에 ID 설정 추가
     */
    public static class CandidateMetadataEntityBuilder {
        public CandidateMetadataEntityBuilder id(UUID id) {
            CandidateMetadataEntity entity = new CandidateMetadataEntity();
            entity.setId(id);
            return this;
        }
    }
}
