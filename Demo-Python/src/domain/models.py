"""
도메인 모델 (Domain Models)

- Pydantic을 사용하여 각 도메인(recruit, candidate)의 데이터 구조를 정의합니다.
- 모든 도메인 모델은 공통 필드(예: id)를 갖는 BaseData를 상속받습니다.
- 이 모델들은 데이터 로딩, FastAPI의 요청/응답, 데이터 직렬화 등
  애플리케이션 전반에서 데이터의 일관성과 유효성을 보장하는 데 사용됩니다.
"""
from typing import List, Optional
from pydantic import BaseModel, Field

# --- 기본 모델 ---

class BaseData(BaseModel):
    """
    모든 도메인 모델이 상속받는 기본 데이터 모델입니다.
    공통 필드를 정의합니다.
    """
    id: str = Field(..., description="고유 식별자 (UUID v7)")


# --- 채용(Recruit) 도메인 모델 ---

class RecruitData(BaseData):
    """
    채용 공고 데이터 모델입니다.
    .pkl 파일의 행(row)이 이 구조로 파싱됩니다.
    """
    company_name: str = Field(..., description="회사명")
    exp_years: int = Field(..., description="요구 경력(년차)")
    english_level: Optional[str] = Field(None, description="영어 수준")
    primary_keyword: Optional[str] = Field(None, description="핵심 키워드")
    vector: List[float] = Field(..., description="임베딩 벡터 (384차원)")

    class Config:
        # Pydantic 모델이 임의의 클래스 타입(예: pandas row)을 다룰 수 있도록 허용
        arbitrary_types_allowed = True


# --- 후보자(Candidate) 도메인 모델 ---

class CandidateData(BaseData):
    """
    후보자 데이터 모델입니다. (예시)
    아직 구체적인 필드가 정의되지 않았으며, 추후 확장을 위해 구조만 마련합니다.
    """
    name: str = Field(..., description="후보자 이름")
    skills: List[str] = Field(default_factory=list, description="보유 기술 스택")
    # ... 후보자 관련 다른 필드들 추가 가능

    class Config:
        arbitrary_types_allowed = True
