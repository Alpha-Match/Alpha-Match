package com.alpha.backend.domain.embedding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Embedding Repository
 * Recruit Embedding 테이블에 대한 데이터 액세스 레이어
 * pgvector 타입을 지원
 */
@Repository
public interface EmbeddingRepository extends JpaRepository<EmbeddingEntity, UUID> {

    /**
     * Batch Upsert using Native Query with pgvector
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     */
    @Modifying
    @Query(value = """
        INSERT INTO recruit_embedding (id, vector, updated_at)
        VALUES (:#{#entity.id}, CAST(:#{#entity.vector} AS vector), NOW())
        ON CONFLICT (id)
        DO UPDATE SET
            vector = CAST(EXCLUDED.vector AS vector),
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") EmbeddingEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    default void upsertAll(List<EmbeddingEntity> entities) {
        entities.forEach(this::upsert);
    }

    /**
     * Vector 유사도 검색 (L2 distance)
     * @param queryVector 검색할 벡터
     * @param limit 결과 개수
     * @return 유사한 벡터 리스트
     */
    @Query(value = """
        SELECT * FROM recruit_embedding
        ORDER BY vector <-> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<EmbeddingEntity> findSimilarVectors(@Param("queryVector") String queryVector,
                                              @Param("limit") int limit);
}
