package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.candidate.entity.CandidateMetadataEntity;
import com.alpha.backend.domain.candidate.repository.CandidateMetadataRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Candidate Metadata JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 */
@Repository
public interface CandidateMetadataJpaRepository
        extends JpaRepository<CandidateMetadataEntity, UUID>, CandidateMetadataRepository {

    /**
     * Batch Upsert using Native Query
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     */
    @Override
    @Modifying
    @Query(value = """
        INSERT INTO candidate_metadata (id, name, skills, experience_years, education_level, preferred_location, expected_salary, updated_at)
        VALUES (:#{#entity.id}, :#{#entity.name}, :#{#entity.skills}, :#{#entity.experienceYears},
                :#{#entity.educationLevel}, :#{#entity.preferredLocation}, :#{#entity.expectedSalary}, NOW())
        ON CONFLICT (id)
        DO UPDATE SET
            name = EXCLUDED.name,
            skills = EXCLUDED.skills,
            experience_years = EXCLUDED.experience_years,
            education_level = EXCLUDED.education_level,
            preferred_location = EXCLUDED.preferred_location,
            expected_salary = EXCLUDED.expected_salary,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") CandidateMetadataEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    @Override
    default void upsertAll(List<CandidateMetadataEntity> entities) {
        entities.forEach(this::upsert);
    }
}
