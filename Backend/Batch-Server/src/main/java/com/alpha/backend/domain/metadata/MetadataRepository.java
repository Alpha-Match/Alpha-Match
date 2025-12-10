package com.alpha.backend.domain.metadata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Metadata Repository
 * Recruit Metadata 테이블에 대한 데이터 액세스 레이어
 */
@Repository
public interface MetadataRepository extends JpaRepository<MetadataEntity, UUID> {

    /**
     * Batch Upsert using Native Query
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     */
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
    void upsert(@Param("entity") MetadataEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    default void upsertAll(List<MetadataEntity> entities) {
        entities.forEach(this::upsert);
    }
}
