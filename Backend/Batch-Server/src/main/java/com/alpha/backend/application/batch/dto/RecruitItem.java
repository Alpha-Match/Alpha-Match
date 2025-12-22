package com.alpha.backend.application.batch.dto;

import com.alpha.backend.domain.recruit.entity.RecruitDescriptionEntity;
import com.alpha.backend.domain.recruit.entity.RecruitEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillsEmbeddingEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Recruit Item DTO
 * ItemProcessor와 ItemWriter 간 데이터 전달을 위한 DTO
 *
 * Recruit 도메인은 4개 테이블에 분산 저장:
 * 1. RecruitEntity - recruit 테이블 (기본 정보)
 * 2. List<RecruitSkillEntity> - recruit_skill 테이블 (요구 스킬 목록, 1:N)
 * 3. RecruitDescriptionEntity - recruit_description 테이블 (채용 공고 원문)
 * 4. RecruitSkillsEmbeddingEntity - recruit_skills_embedding 테이블 (벡터)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitItem {

    /**
     * 채용 공고 기본 정보 (recruit 테이블)
     */
    private RecruitEntity recruit;

    /**
     * 요구 스킬 목록 (recruit_skill 테이블, 1:N)
     */
    private List<RecruitSkillEntity> skills;

    /**
     * 채용 공고 원문 (recruit_description 테이블)
     */
    private RecruitDescriptionEntity description;

    /**
     * 채용 공고 스킬 벡터 (recruit_skills_embedding 테이블)
     */
    private RecruitSkillsEmbeddingEntity embedding;

    /**
     * 도메인 타입 반환
     */
    public String getDomainType() {
        return "recruit";
    }

    /**
     * Recruit ID 반환 (공통 식별자)
     */
    public java.util.UUID getRecruitId() {
        return recruit != null ? recruit.getRecruitId() : null;
    }
}
