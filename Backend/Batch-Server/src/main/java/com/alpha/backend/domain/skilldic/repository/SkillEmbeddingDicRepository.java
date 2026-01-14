package com.alpha.backend.domain.skilldic.repository;

import com.alpha.backend.domain.skilldic.entity.SkillEmbeddingDicEntity;

import java.util.List;

/**
 * Skill Embedding Dictionary Repository (Domain Interface)
 * 기술 스택 사전에 대한 도메인 레포지토리 인터페이스
 *
 * Clean Architecture: Domain 계층의 추상화 (Port)
 * 실제 구현은 Infrastructure 계층에서 담당
 */
public interface SkillEmbeddingDicRepository {

    /**
     * 단일 엔티티 Upsert
     */
    void upsert(SkillEmbeddingDicEntity entity);

    /**
     * 다수 엔티티 Upsert
     */
    void upsertAll(List<SkillEmbeddingDicEntity> entities);
}
