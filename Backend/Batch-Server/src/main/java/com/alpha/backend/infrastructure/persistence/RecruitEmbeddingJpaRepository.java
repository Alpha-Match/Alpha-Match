package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitEmbeddingEntity;
import com.alpha.backend.domain.recruit.repository.RecruitEmbeddingRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Recruit Embedding JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 */
@Repository
public interface RecruitEmbeddingJpaRepository
        extends JpaRepository<RecruitEmbeddingEntity, UUID>, RecruitEmbeddingRepository {

    /**
     * Batch Upsert using Native Query with pgvector
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     */
    @Override
    @Modifying
    @Query(value = """
        INSERT INTO recruit_embedding (id, vector, updated_at)
        VALUES (:#{#entity.id}, CAST(:#{#entity.vector} AS vector), NOW())
        ON CONFLICT (id)
        DO UPDATE SET
            vector = CAST(EXCLUDED.vector AS vector),
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") RecruitEmbeddingEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    @Override
    default void upsertAll(List<RecruitEmbeddingEntity> entities) {
        entities.forEach(this::upsert);
    }

    /**
     * Vector 유사도 검색 (L2 distance)
     */
    @Override
    @Query(value = """
        SELECT * FROM recruit_embedding
        ORDER BY vector <-> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<RecruitEmbeddingEntity> findSimilarVectors(@Param("queryVector") String queryVector,
                                                     @Param("limit") int limit);
}
