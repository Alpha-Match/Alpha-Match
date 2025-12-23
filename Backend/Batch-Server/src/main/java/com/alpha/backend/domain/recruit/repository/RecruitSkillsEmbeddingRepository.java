package com.alpha.backend.domain.recruit.repository;

import com.alpha.backend.domain.recruit.entity.RecruitSkillsEmbeddingEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RecruitSkillsEmbedding (채용 공고 스킬 임베딩) Repository - Domain Layer Interface (Port)
 *
 * v2 Schema: recruit_skills_embedding 테이블 (v1의 recruit_embedding 대체)
 * - recruit_id (UUID PK/FK) - 1:1 with recruit
 * - skills (TEXT[]) - 스킬 목록 배열
 * - skills_vector (VECTOR(384)) - 384차원 임베딩
 * - created_at, updated_at
 *
 * Relationship: recruit (1) ↔ (1) recruit_skills_embedding (CASCADE DELETE)
 *
 * 변경 사항:
 * - 테이블명: recruit_embedding → recruit_skills_embedding
 * - 벡터 차원: 384d (통일)
 * - 필드 추가: skills (TEXT[])
 */
public interface RecruitSkillsEmbeddingRepository {

    /**
     * RecruitSkillsEmbedding 단건 조회
     *
     * @param recruitId 채용 공고 ID
     * @return RecruitSkillsEmbedding Entity (Optional)
     */
    Optional<RecruitSkillsEmbeddingEntity> findById(UUID recruitId);

    /**
     * RecruitSkillsEmbedding 전체 조회
     *
     * @return RecruitSkillsEmbedding Entity 리스트
     */
    List<RecruitSkillsEmbeddingEntity> findAll();

    /**
     * RecruitSkillsEmbedding 단건 Upsert (INSERT or UPDATE)
     *
     * @param entity RecruitSkillsEmbedding Entity
     */
    void upsert(RecruitSkillsEmbeddingEntity entity);

    /**
     * RecruitSkillsEmbedding 배치 Upsert (Bulk INSERT or UPDATE)
     *
     * Spring Batch Writer에서 사용
     *
     * @param entities RecruitSkillsEmbedding Entity 리스트
     */
    void upsertAll(List<RecruitSkillsEmbeddingEntity> entities);

    /**
     * RecruitSkillsEmbedding 삭제
     *
     * @param recruitId 채용 공고 ID
     */
    void deleteById(UUID recruitId);

    /**
     * RecruitSkillsEmbedding 존재 여부 확인
     *
     * @param recruitId 채용 공고 ID
     * @return 존재 여부
     */
    boolean existsById(UUID recruitId);

    /**
     * 벡터 유사도 검색 (Cosine Distance)
     *
     * @param queryVector 쿼리 벡터 (384차원)
     * @param limit 결과 개수
     * @return 유사한 Recruit ID 리스트 (거리 순)
     */
    List<UUID> findSimilarRecruits(float[] queryVector, int limit);
}
