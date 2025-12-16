package com.alpha.backend.domain.recruit.repository;

import com.alpha.backend.domain.recruit.entity.RecruitEmbeddingEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Recruit Embedding Repository (Domain Interface)
 * 채용 공고 임베딩 벡터에 대한 도메인 레포지토리 인터페이스
 *
 * Clean Architecture: Domain 계층의 추상화 (Port)
 * 실제 구현은 Infrastructure 계층에서 담당
 */
public interface RecruitEmbeddingRepository {

    /**
     * 단일 엔티티 Upsert
     */
    void upsert(RecruitEmbeddingEntity entity);

    /**
     * 다수 엔티티 Upsert
     */
    void upsertAll(List<RecruitEmbeddingEntity> entities);

    /**
     * Vector 유사도 검색 (L2 distance)
     */
    List<RecruitEmbeddingEntity> findSimilarVectors(String queryVector, int limit);
}
