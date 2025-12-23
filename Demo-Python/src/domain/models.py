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
# Recruit 도메인 (채용 공고) - v2
# ============================================================================

class RecruitData(BaseData):
    """
    채용 공고 데이터 모델 (v2)

    v2 변경사항:
    - 추가: position, published_at, skills[], long_description, description_lang
    - 필드명 변경: exp_years → experience_years, vector → skills_vector

    Proto 매핑: RecruitRow (v2)
    - id → id (UUID v7)
    - position → position (포지션 원문)
    - company_name → company_name
    - experience_years → experience_years
    - primary_keyword → primary_keyword
    - english_level → english_level
    - published_at → published_at (ISO 8601)
    - skills → skills (요구 기술 스택 배열)
    - long_description → long_description (채용 공고 원문)
    - description_lang → description_lang (원문 언어)
    - skills_vector → skills_vector (384d)
    """
    position: str = Field(..., description="포지션 원문")
    company_name: str = Field(..., description="회사명")
    experience_years: Optional[int] = Field(None, ge=0, description="요구 경력 (연)")
    primary_keyword: Optional[str] = Field(None, description="주요 키워드")
    english_level: Optional[str] = Field(None, description="영어 레벨")
    published_at: Optional[str] = Field(None, description="게시 날짜 (ISO 8601)")
    skills: List[str] = Field(..., min_length=1, description="요구 기술 스택 배열")
    long_description: Optional[str] = Field(None, description="채용 공고 원문 (Markdown)")
    description_lang: Optional[str] = Field(None, description="원문 언어 (e.g., 'en', 'ko')")
    skills_vector: List[float] = Field(..., description="기술 스택 벡터 (384d)")

    @field_validator('skills_vector')
    @classmethod
    def validate_vector_dimension(cls, v):
        if len(v) != 384:
            raise ValueError(f'Recruit skills_vector must be 384 dimensions, got {len(v)}')
        return v

    @field_validator('skills')
    @classmethod
    def validate_skills_not_empty(cls, v):
        if not v or len(v) == 0:
            raise ValueError('Recruit must have at least one skill')
        return v


# ============================================================================
# Candidate 도메인 (후보자) - Flat DTO 구조
# ============================================================================

class CandidateData(BaseData):
    """
    후보자 데이터 모델 (v2 - Flat DTO 구조)

    Java gRPC DTO 매핑: CandidateRowDto
    - candidate_id → candidate_id (UUID v7)
    - position_category → position_category
    - experience_years → experience_years
    - original_resume → original_resume
    - skills → skills (배열)
    - skills_vector → skills_vector (384d)

    Java 매핑: Batch Writer가 4개 테이블에 분산 저장 (v2)
    - CandidateEntity (candidate 테이블 - 기본 정보)
    - CandidateSkillEntity (candidate_skill 테이블 - skills 배열 분해, 1:N)
    - CandidateDescriptionEntity (candidate_description 테이블 - 이력서 원문)
    - CandidateSkillsEmbeddingEntity (candidate_skills_embedding 테이블 - 벡터)

    Note: BaseData를 상속받지만 id 필드를 candidate_id로 오버라이드
    """
    candidate_id: str = Field(..., description="후보자 UUID")
    position_category: str = Field(..., description="직종 카테고리")
    experience_years: int = Field(..., ge=0, description="경력 (연)")
    original_resume: str = Field(..., description="원문 이력서")
    skills: List[str] = Field(..., min_length=1, description="보유 스킬 배열 (예: ['Java', 'Python'])")
    skills_vector: List[float] = Field(..., description="기술 스택 벡터 (384차원)")

    @field_validator('skills_vector')
    @classmethod
    def validate_vector_dimension(cls, v):
        if len(v) != 384:
            raise ValueError(f'Candidate skills_vector must be 384 dimensions, got {len(v)}')
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
    기술 스택 사전 데이터 모델 (v2)

    v2 변경사항:
    - 벡터 차원 통일: 768d → 384d

    Java gRPC DTO 매핑: SkillEmbeddingDicRowDto
    - skill → skill (PK)
    - position_category → position_category
    - skill_vector → skill_vector (384d)

    Java 매핑: SkillEmbeddingDicEntity (skill_embedding_dic 테이블)
    """
    skill: str = Field(..., description="스킬명 (PK)")
    position_category: str = Field(..., description="직종 카테고리")
    skill_vector: List[float] = Field(..., description="스킬 벡터 (384차원)")

    @field_validator('skill_vector')
    @classmethod
    def validate_vector_dimension(cls, v):
        if len(v) != 384:
            raise ValueError(f'Skill vector must be 384 dimensions, got {len(v)}')
        return v

    class Config:
        arbitrary_types_allowed = True
