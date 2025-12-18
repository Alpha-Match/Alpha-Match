"""
도메인 모델 (Domain Models)

- Pydantic을 사용하여 각 도메인(recruit, candidate, skill_dic)의 데이터 구조를 정의합니다.
- 모든 도메인 모델은 공통 필드(예: id)를 갖는 BaseData를 상속받습니다.
- 이 모델들은 데이터 로딩, FastAPI의 요청/응답, 데이터 직렬화 등
  애플리케이션 전반에서 데이터의 일관성과 유효성을 보장하는 데 사용됩니다.

Proto 매핑:
- RecruitData → RecruitRow (proto)
- CandidateData → CandidateRow (proto)
- SkillEmbeddingDicData → SkillEmbeddingDicRow (proto)
"""
from typing import List, Optional
from pydantic import BaseModel, Field, field_validator

# ============================================================================
# 기본 모델
# ============================================================================

class BaseData(BaseModel):
    """
    모든 도메인 모델이 상속받는 기본 데이터 모델입니다.
    공통 필드를 정의합니다.
    """
    id: str = Field(..., description="고유 식별자 (UUID v7)")

    class Config:
        # Pydantic 모델이 임의의 클래스 타입(예: pandas row)을 다룰 수 있도록 허용
        arbitrary_types_allowed = True


# ============================================================================
# Recruit 도메인 (채용 공고)
# ============================================================================

class RecruitData(BaseData):
    """
    채용 공고 데이터 모델입니다.

    Proto 매핑: RecruitRow
    - id → id (UUID v7)
    - company_name → company_name
    - exp_years → exp_years
    - english_level → english_level
    - primary_keyword → primary_keyword
    - vector → vector (384d)
    """
    company_name: str = Field(..., description="회사명")
    exp_years: int = Field(..., ge=0, description="요구 경력 (년차)")
    english_level: Optional[str] = Field(None, description="영어 수준")
    primary_keyword: Optional[str] = Field(None, description="핵심 키워드")
    vector: List[float] = Field(..., description="임베딩 벡터 (384차원)")

    @field_validator('vector')
    @classmethod
    def validate_vector_dimension(cls, v):
        if len(v) != 384:
            raise ValueError(f'Recruit vector must be 384 dimensions, got {len(v)}')
        return v


# ============================================================================
# Candidate 도메인 (후보자) - Flat DTO 구조
# ============================================================================

class CandidateData(BaseData):
    """
    후보자 데이터 모델입니다 (Flat DTO 구조).

    Java gRPC DTO 매핑: CandidateRowDto
    - candidate_id → candidate_id (UUID v7)
    - position_category → position_category
    - experience_years → experience_years
    - original_resume → original_resume
    - skills → skills (배열)
    - skills_vector → skills_vector (768d)

    Java 매핑: Batch Writer가 3개 테이블에 분산 저장
    - CandidateEntity (candidate 테이블)
    - CandidateSkillEntity (candidate_skill 테이블, skills 배열 분해)
    - CandidateSkillsEmbeddingEntity (candidate_skills_embedding 테이블)

    Note: BaseData를 상속받지만 id 필드를 candidate_id로 오버라이드
    """
    candidate_id: str = Field(..., description="후보자 UUID")
    position_category: str = Field(..., description="직종 카테고리")
    experience_years: int = Field(..., ge=0, description="경력 (연)")
    original_resume: str = Field(..., description="원문 이력서")
    skills: List[str] = Field(..., min_length=1, description="보유 스킬 배열 (예: ['Java', 'Python'])")
    skills_vector: List[float] = Field(..., description="기술 스택 벡터 (768차원)")

    @field_validator('skills_vector')
    @classmethod
    def validate_vector_dimension(cls, v):
        if len(v) != 768:
            raise ValueError(f'Candidate vector must be 768 dimensions, got {len(v)}')
        return v

    @field_validator('skills')
    @classmethod
    def validate_skills_not_empty(cls, v):
        if not v or len(v) == 0:
            raise ValueError('Candidate must have at least one skill')
        return v

    class Config:
        arbitrary_types_allowed = True

    # BaseData의 id 필드를 숨기기 위한 property
    @property
    def id(self) -> str:
        """id 필드를 candidate_id로 리다이렉트"""
        return self.candidate_id


# ============================================================================
# SkillEmbeddingDic 도메인 (기술 사전)
# ============================================================================

class SkillEmbeddingDicData(BaseModel):
    """
    기술 스택 사전 데이터 모델입니다.

    Java gRPC DTO 매핑: SkillEmbeddingDicRowDto (생성 예정)
    - skill → skill (PK)
    - position_category → position_category
    - skill_vector → skill_vector (768d)

    Java 매핑: SkillEmbeddingDicEntity (skill_embedding_dic 테이블)
    """
    skill: str = Field(..., description="스킬명 (PK)")
    position_category: str = Field(..., description="직종 카테고리")
    skill_vector: List[float] = Field(..., description="스킬 벡터 (768차원)")

    @field_validator('skill_vector')
    @classmethod
    def validate_vector_dimension(cls, v):
        if len(v) != 768:
            raise ValueError(f'Skill vector must be 768 dimensions, got {len(v)}')
        return v

    class Config:
        arbitrary_types_allowed = True
