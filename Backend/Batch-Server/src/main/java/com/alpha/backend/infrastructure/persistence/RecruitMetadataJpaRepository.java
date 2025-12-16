package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitMetadataEntity;
import com.alpha.backend.domain.recruit.repository.RecruitMetadataRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Recruit Metadata JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 */
@Repository
public interface RecruitMetadataJpaRepository
        extends JpaRepository<RecruitMetadataEntity, UUID>, RecruitMetadataRepository {

    /**
     * Batch Upsert using Native Query
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     */
    @Override
    @Modifying
    @Query(value = """
        INSERT INTO recruit_metadata (id, company_name, exp_years, english_level, primary_keyword, updated_at)
        VALUES (:#{#entity.id}, :#{#entity.companyName}, :#{#entity.expYears},
                :#{#entity.englishLevel}, :#{#entity.primaryKeyword}, NOW())
        ON CONFLICT (id)
        DO UPDATE SET
            company_name = EXCLUDED.company_name,
            exp_years = EXCLUDED.exp_years,
            english_level = EXCLUDED.english_level,
            primary_keyword = EXCLUDED.primary_keyword,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") RecruitMetadataEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    @Override
    default void upsertAll(List<RecruitMetadataEntity> entities) {
        entities.forEach(this::upsert);
    }
}
