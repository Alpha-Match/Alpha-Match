package com.alpha.backend.application.batch.dto;

import com.alpha.backend.domain.candidate.entity.CandidateDescriptionEntity;
import com.alpha.backend.domain.candidate.entity.CandidateEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillsEmbeddingEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Candidate Item DTO (v2)
 * ItemProcessor와 ItemWriter 간 데이터 전달을 위한 DTO
 *
 * Candidate 도메인은 4개 테이블에 분산 저장:
 * 1. CandidateEntity - candidate 테이블 (기본 정보)
 * 2. List<CandidateSkillEntity> - candidate_skill 테이블 (스킬 목록, 1:N)
 * 3. CandidateDescriptionEntity - candidate_description 테이블 (이력서 원문)
 * 4. CandidateSkillsEmbeddingEntity - candidate_skills_embedding 테이블 (벡터)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateItem {

    /**
     * 후보자 기본 정보 (candidate 테이블)
     */
    private CandidateEntity candidate;

    /**
     * 후보자 스킬 목록 (candidate_skill 테이블, 1:N)
     */
    private List<CandidateSkillEntity> skills;

    /**
     * 이력서 원문 (candidate_description 테이블)
     */
    private CandidateDescriptionEntity description;

    /**
     * 후보자 스킬 벡터 (candidate_skills_embedding 테이블)
     */
    private CandidateSkillsEmbeddingEntity embedding;

    /**
     * 도메인 타입 반환
     */
    public String getDomainType() {
        return "candidate";
    }

    /**
     * 후보자 ID 반환 (공통 식별자)
     */
    public java.util.UUID getCandidateId() {
        return candidate != null ? candidate.getCandidateId() : null;
    }
}
