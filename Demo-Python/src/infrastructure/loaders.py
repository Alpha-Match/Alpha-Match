"""
데이터 로더 (Data Loaders)

- 제네릭과 프로토콜을 사용하여 다양한 데이터 소스와 도메인을 처리하는
  유연하고 확장 가능한 로딩 시스템을 구현합니다.
- TypeVar의 공변성을 활용하여, 구체적인 로더(예: DataLoader[RecruitData])를
  더 일반적인 로더(예: DataLoader[BaseData])가 필요한 곳에 안전하게 사용할 수 있습니다.
"""
import pandas as pd
from typing import List, Protocol, Type, TypeVar, Dict

# 프로젝트 내 다른 모듈 임포트
from domain.models import BaseData, RecruitData, CandidateData

# 공변성을 갖는 제네릭 타입 T_Row를 정의합니다.
# BaseData를 상속하는 모든 타입을 허용합니다.
# covariant=True 설정은 DataLoader[RecruitData]가 DataLoader[BaseData]의 하위 타입임을 의미합니다.
# 이를 통해 타입 계층 구조를 안전하게 구성할 수 있습니다.
T_Row = TypeVar('T_Row', bound=BaseData, covariant=True)


class DataLoader(Protocol[T_Row]):
    """
    데이터 로더를 위한 프로토콜(인터페이스)입니다.
    이 프로토콜을 구현하는 모든 클래스는 `load` 메소드를 반드시 가져야 합니다.
    제네릭 타입 `T_Row`를 사용하여 어떤 종류의 데이터를 로드할지 지정합니다.
    """
    def load(self, file_path: str) -> List[T_Row]:
        """
        지정된 경로의 파일에서 데이터를 로드하고,
        정해진 모델(T_Row)의 리스트로 변환하여 반환합니다.

        Args:
            file_path (str): 로드할 파일의 경로.

        Returns:
            List[T_Row]: 파싱된 데이터 모델의 리스트.
        """
        ...


class PklRecruitLoader(DataLoader[RecruitData]):
    """
    Pandas Pickle(.pkl) 파일에서 '채용 공고(Recruit)' 데이터를 로드하는 구체적인 로더입니다.
    """
    def load(self, file_path: str) -> List[RecruitData]:
        """
        .pkl 파일을 읽어 Pandas DataFrame으로 변환한 후,
        각 행을 RecruitData 모델 객체로 파싱하여 리스트로 반환합니다.

        Args:
            file_path (str): .pkl 파일 경로.

        Returns:
            List[RecruitData]: RecruitData 모델 객체의 리스트.
        """
        try:
            df = pd.read_pickle(file_path)
            # DataFrame의 각 행을 순회하며 Pydantic 모델로 변환
            # to_dict('records')는 각 행을 {'컬럼명': 값} 형태의 딕셔너리로 만듭니다.
            return [RecruitData(**row) for row in df.to_dict('records')]
        except FileNotFoundError:
            raise FileNotFoundError(f"데이터 파일을 찾을 수 없습니다: {file_path}")
        except Exception as e:
            raise IOError(f"데이터 파일 로딩 중 오류 발생: {e}")


class PklCandidateLoader(DataLoader[CandidateData]):
    """
    .pkl 파일에서 '후보자(Candidate)' 데이터를 로드하는 로더입니다. (구현 예시)
    """
    def load(self, file_path: str) -> List[CandidateData]:
        # 현재는 예시이므로 실제 구현은 생략하고 빈 리스트를 반환합니다.
        # 추후 후보자 데이터 형식에 맞게 `PklRecruitLoader`처럼 구현이 필요합니다.
        print(f"주의: {file_path}에 대한 후보자 로더는 아직 구현되지 않았습니다.")
        return []


# --- 로더 팩토리 ---

# 도메인 이름(문자열)과 실제 로더 클래스를 매핑합니다.
_loader_registry: Dict[str, DataLoader] = {
    "recruit": PklRecruitLoader(),
    "candidate": PklCandidateLoader(),
}


def get_loader(domain: str) -> DataLoader:
    """
    도메인 이름에 해당하는 데이터 로더 인스턴스를 반환하는 팩토리 함수입니다.

    Args:
        domain (str): 데이터를 식별하는 도메인 이름 (예: "recruit").

    Returns:
        DataLoader: 해당 도메인에 맞는 데이터 로더 인스턴스.

    Raises:
        ValueError: 지원하지 않는 도메인일 경우 발생합니다.
    """
    loader = _loader_registry.get(domain)
    if loader is None:
        raise ValueError(f"지원하지 않는 도메인입니다: '{domain}'. "
                         f"사용 가능한 도메인: {list(_loader_registry.keys())}")
    return loader
