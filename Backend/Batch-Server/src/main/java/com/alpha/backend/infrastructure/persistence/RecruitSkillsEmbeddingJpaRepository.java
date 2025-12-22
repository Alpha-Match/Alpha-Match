package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitSkillsEmbeddingEntity;
import com.alpha.backend.domain.recruit.repository.RecruitSkillsEmbeddingRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * RecruitSkillsEmbedding Repository - Infrastructure Layer (JPA Adapter)
 *
 * v2 Schema: recruit_skills_embedding 테이블
 * - Native Query 기반 Upsert (ON CONFLICT)
 * - Batch Upsert 지원
 * - pgvector 유사도 검색 지원
 */
@Repository
public interface RecruitSkillsEmbeddingJpaRepository
        extends JpaRepository<RecruitSkillsEmbeddingEntity, UUID>, RecruitSkillsEmbeddingRepository {

    /**
     * RecruitSkillsEmbedding 단건 Upsert (Native Query)
     *
     * @param entity RecruitSkillsEmbedding Entity
     */
    @Override
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO recruit_skills_embedding (
            recruit_id, skills, skills_vector, created_at, updated_at
        )
        VALUES (
            :#{#entity.recruitId},
            :#{#entity.skills},
            :#{#entity.skillsVector}::vector(384),
            COALESCE(:#{#entity.createdAt}, NOW()),
            COALESCE(:#{#entity.updatedAt}, NOW())
        )
        ON CONFLICT (recruit_id)
        DO UPDATE SET
            skills = EXCLUDED.skills,
            skills_vector = EXCLUDED.skills_vector,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") RecruitSkillsEmbeddingEntity entity);

    /**
     * RecruitSkillsEmbedding 배치 Upsert (Iterative)
     *
     * Spring Batch에서 Chunk 단위로 호출
     *
     * @param entities RecruitSkillsEmbedding Entity 리스트
     */
    @Override
    @Transactional
    default void upsertAll(List<RecruitSkillsEmbeddingEntity> entities) {
        entities.forEach(this::upsert);
    }

    /**
     * 벡터 유사도 검색 (Cosine Distance) - String 변환 버전
     *
     * IVFFlat 인덱스 사용: idx_recruit_skills_vector
     * Distance Operator: <=> (Cosine Distance)
     *
     * @param queryVector 쿼리 벡터 (384차원, float 배열을 String으로 변환하여 전달)
     * @param limit 결과 개수
     * @return 유사한 Recruit ID 리스트 (거리 순)
     */
    @Query(value = """
        SELECT recruit_id
        FROM recruit_skills_embedding
        ORDER BY skills_vector <=> CAST(:queryVector AS vector(384))
        LIMIT :limit
        """, nativeQuery = true)
    List<UUID> findSimilarRecruitsInternal(
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

    /**
     * 벡터 유사도 검색 (float[] 배열 오버로드) - Domain Interface 구현
     *
     * @param queryVector 쿼리 벡터 (384차원)
     * @param limit 결과 개수
     * @return 유사한 Recruit ID 리스트 (거리 순)
     */
    @Override
    default List<UUID> findSimilarRecruits(float[] queryVector, int limit) {
        // float[] → String 변환: [0.1, 0.2, ...] 형식
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < queryVector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(queryVector[i]);
        }
        sb.append("]");
        return findSimilarRecruitsInternal(sb.toString(), limit);
    }
}
