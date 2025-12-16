package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.candidate.entity.CandidateEmbeddingEntity;
import com.alpha.backend.domain.candidate.repository.CandidateEmbeddingRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Candidate Embedding JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 */
@Repository
public interface CandidateEmbeddingJpaRepository
        extends JpaRepository<CandidateEmbeddingEntity, UUID>, CandidateEmbeddingRepository {

    /**
     * Batch Upsert using Native Query with pgvector
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     */
    @Override
    @Modifying
    @Query(value = """
        INSERT INTO candidate_embedding (id, vector, updated_at)
        VALUES (:#{#entity.id}, CAST(:#{#entity.vector} AS vector), NOW())
        ON CONFLICT (id)
        DO UPDATE SET
            vector = CAST(EXCLUDED.vector AS vector),
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") CandidateEmbeddingEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    @Override
    default void upsertAll(List<CandidateEmbeddingEntity> entities) {
        entities.forEach(this::upsert);
    }

    /**
     * Vector 유사도 검색 (L2 distance)
     */
    @Override
    @Query(value = """
        SELECT * FROM candidate_embedding
        ORDER BY vector <-> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<CandidateEmbeddingEntity> findSimilarVectors(@Param("queryVector") String queryVector,
                                                       @Param("limit") int limit);
}
