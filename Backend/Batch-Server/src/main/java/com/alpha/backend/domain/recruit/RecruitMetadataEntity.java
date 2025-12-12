package com.alpha.backend.domain.recruit;

import com.alpha.backend.domain.common.BaseMetadataEntity;
import jakarta.persistence.*;
import lombok.*;

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
public class RecruitMetadataEntity extends BaseMetadataEntity {

    @Column(name = "company_name", nullable = false, columnDefinition = "TEXT")
    private String companyName;

    @Column(name = "exp_years", nullable = false)
    private Integer expYears;

    @Column(name = "english_level", columnDefinition = "TEXT")
    private String englishLevel;

    @Column(name = "primary_keyword", columnDefinition = "TEXT")
    private String primaryKeyword;

    @Override
    public String getDomainType() {
        return "recruit";
    }

    /**
     * Builder에 ID 설정 추가
     */
    public static class RecruitMetadataEntityBuilder {
        public RecruitMetadataEntityBuilder id(UUID id) {
            RecruitMetadataEntity entity = new RecruitMetadataEntity();
            entity.setId(id);
            return this;
        }
    }
}
