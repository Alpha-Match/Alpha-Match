"""
데이터 로더 (Chunk-based Data Loaders)

- Chunk 단위로 데이터를 로드하여 메모리 효율성 확보
- 다양한 포맷 지원: pkl, csv, parquet
- 도메인별 + 포맷별 확장 가능한 구조
- Iterator 패턴으로 대용량 파일 처리
"""
import pandas as pd
import logging
from abc import ABC, abstractmethod
from typing import Iterator, List, TypeVar, Generic, Dict, Tuple, Type
from enum import Enum

# 프로젝트 내 다른 모듈 임포트
from ..domain.models import BaseData, RecruitData, CandidateData, SkillEmbeddingDicData

# 로깅 설정
logger = logging.getLogger(__name__)

# 제네릭 타입 정의
T_Row = TypeVar('T_Row', bound=BaseData)

# ============================================================================
# 데이터 포맷 Enum
# ============================================================================

class DataFormat(str, Enum):
    """지원하는 데이터 포맷"""
    PKL = "pkl"
    CSV = "csv"
    PARQUET = "parquet"


# ============================================================================
# Base Chunk Loader (추상 클래스)
# ============================================================================

class BaseChunkLoader(ABC, Generic[T_Row]):
    """
    Chunk 단위로 데이터를 로드하는 추상 베이스 클래스

    모든 Loader는 이 클래스를 상속받아 load_chunks() 메서드를 구현해야 합니다.
    Iterator 패턴을 사용하여 메모리 효율적으로 대용량 데이터를 처리합니다.
    """

    def __init__(self, row_class: Type[T_Row], chunk_size: int = 1000):
        """
        Args:
            row_class: Pydantic 모델 클래스 (RecruitData, CandidateData 등)
            chunk_size: Chunk 크기 (기본 1000 rows)
        """
        self.row_class = row_class
        self.chunk_size = chunk_size

    @abstractmethod
    def load_chunks(self, file_path: str) -> Iterator[List[T_Row]]:
        """
        파일에서 Chunk 단위로 데이터를 로드하는 메서드 (추상 메서드)

        Args:
            file_path: 로드할 파일 경로

        Yields:
            List[T_Row]: Chunk 단위로 파싱된 데이터 모델 리스트
        """
        pass


# ============================================================================
# Pkl Chunk Loader
# ============================================================================

class PklChunkLoader(BaseChunkLoader[T_Row]):
    """
    Pickle(.pkl) 파일을 Chunk 단위로 로드하는 Loader
    """

    def load_chunks(self, file_path: str) -> Iterator[List[T_Row]]:
        """
        .pkl 파일을 Chunk 단위로 로드

        전체 파일을 메모리에 로드한 후, Chunk 단위로 분할하여 yield
        (Pandas는 pkl에 대해 chunksize를 지원하지 않음)
        """
        try:
            logger.info(f"Loading pkl file: {file_path}")
            df = pd.read_pickle(file_path)
            total_rows = len(df)
            logger.info(f"Total rows loaded: {total_rows}")

            # Chunk 단위로 분할
            for i in range(0, total_rows, self.chunk_size):
                chunk_df = df.iloc[i:i+self.chunk_size]
                chunk_data = [self.row_class(**row) for row in chunk_df.to_dict('records')]

                logger.debug(f"Yielding chunk {i//self.chunk_size + 1}: {len(chunk_data)} rows")
                yield chunk_data

        except FileNotFoundError:
            logger.error(f"File not found: {file_path}")
            raise FileNotFoundError(f"데이터 파일을 찾을 수 없습니다: {file_path}")
        except Exception as e:
            logger.error(f"Error loading pkl file: {e}")
            raise IOError(f"pkl 파일 로딩 중 오류 발생: {e}")


# ============================================================================
# CSV Chunk Loader
# ============================================================================

class CsvChunkLoader(BaseChunkLoader[T_Row]):
    """
    CSV 파일을 Chunk 단위로 로드하는 Loader

    Pandas의 read_csv(chunksize=...)를 활용하여 메모리 효율적으로 로드
    """

    def load_chunks(self, file_path: str) -> Iterator[List[T_Row]]:
        """
        .csv 파일을 Chunk 단위로 로드

        Pandas의 chunksize 파라미터를 사용하여 스트리밍 방식으로 로드
        """
        try:
            logger.info(f"Loading csv file (chunk mode): {file_path}")

            # CSV를 Chunk 단위로 읽기
            chunk_iterator = pd.read_csv(file_path, chunksize=self.chunk_size)

            for i, chunk_df in enumerate(chunk_iterator):
                # vector 컬럼이 문자열로 저장된 경우 파싱
                if 'vector' in chunk_df.columns:
                    chunk_df['vector'] = chunk_df['vector'].apply(self._parse_vector)

                if 'skills' in chunk_df.columns and isinstance(chunk_df['skills'].iloc[0], str):
                    chunk_df['skills'] = chunk_df['skills'].apply(self._parse_array)

                chunk_data = [self.row_class(**row) for row in chunk_df.to_dict('records')]

                logger.debug(f"Yielding chunk {i + 1}: {len(chunk_data)} rows")
                yield chunk_data

        except FileNotFoundError:
            logger.error(f"File not found: {file_path}")
            raise FileNotFoundError(f"데이터 파일을 찾을 수 없습니다: {file_path}")
        except Exception as e:
            logger.error(f"Error loading csv file: {e}")
            raise IOError(f"csv 파일 로딩 중 오류 발생: {e}")

    @staticmethod
    def _parse_vector(value) -> List[float]:
        """문자열로 저장된 벡터를 파싱"""
        import json
        if isinstance(value, str):
            return json.loads(value)
        return value

    @staticmethod
    def _parse_array(value) -> List[str]:
        """문자열로 저장된 배열을 파싱"""
        import json
        if isinstance(value, str):
            return json.loads(value)
        return value


# ============================================================================
# Parquet Chunk Loader
# ============================================================================

class ParquetChunkLoader(BaseChunkLoader[T_Row]):
    """
    Parquet 파일을 Chunk 단위로 로드하는 Loader

    PyArrow를 사용하여 효율적으로 로드
    """

    def load_chunks(self, file_path: str) -> Iterator[List[T_Row]]:
        """
        .parquet 파일을 Chunk 단위로 로드

        PyArrow의 ParquetFile을 사용하여 배치 단위로 읽기
        """
        try:
            logger.info(f"Loading parquet file (chunk mode): {file_path}")

            # PyArrow 사용
            try:
                import pyarrow.parquet as pq
            except ImportError:
                raise ImportError("pyarrow가 설치되어 있지 않습니다. 'pip install pyarrow'로 설치하세요.")

            parquet_file = pq.ParquetFile(file_path)

            # Batch 단위로 읽기
            for i, batch in enumerate(parquet_file.iter_batches(batch_size=self.chunk_size)):
                chunk_df = batch.to_pandas()
                chunk_data = [self.row_class(**row) for row in chunk_df.to_dict('records')]

                logger.debug(f"Yielding chunk {i + 1}: {len(chunk_data)} rows")
                yield chunk_data

        except FileNotFoundError:
            logger.error(f"File not found: {file_path}")
            raise FileNotFoundError(f"데이터 파일을 찾을 수 없습니다: {file_path}")
        except Exception as e:
            logger.error(f"Error loading parquet file: {e}")
            raise IOError(f"parquet 파일 로딩 중 오류 발생: {e}")


# ============================================================================
# Loader Registry (Factory Pattern)
# ============================================================================

# 도메인별 + 포맷별 Loader 클래스 매핑
_loader_class_registry: Dict[Tuple[str, DataFormat], Type[BaseChunkLoader]] = {
    # Recruit 도메인
    ("recruit", DataFormat.PKL): PklChunkLoader,
    ("recruit", DataFormat.CSV): CsvChunkLoader,
    ("recruit", DataFormat.PARQUET): ParquetChunkLoader,

    # Candidate 도메인
    ("candidate", DataFormat.PKL): PklChunkLoader,
    ("candidate", DataFormat.CSV): CsvChunkLoader,
    ("candidate", DataFormat.PARQUET): ParquetChunkLoader,

    # SkillEmbeddingDic 도메인
    ("skill_dic", DataFormat.PKL): PklChunkLoader,
    ("skill_dic", DataFormat.CSV): CsvChunkLoader,
    ("skill_dic", DataFormat.PARQUET): ParquetChunkLoader,
}

# 도메인별 Row 클래스 매핑
_domain_row_class_map: Dict[str, Type[BaseData]] = {
    "recruit": RecruitData,
    "candidate": CandidateData,
    "skill_dic": SkillEmbeddingDicData,
}


def get_loader(domain: str, format: DataFormat, chunk_size: int = 1000) -> BaseChunkLoader:
    """
    도메인과 포맷에 맞는 Chunk Loader 인스턴스를 반환하는 팩토리 함수

    Args:
        domain: 도메인 이름 ("recruit", "candidate", "skill_dic")
        format: 데이터 포맷 (DataFormat.PKL, CSV, PARQUET)
        chunk_size: Chunk 크기 (기본 1000)

    Returns:
        BaseChunkLoader: 해당 도메인/포맷에 맞는 Loader 인스턴스

    Raises:
        ValueError: 지원하지 않는 도메인 또는 포맷

    Example:
        >>> loader = get_loader("recruit", DataFormat.CSV, chunk_size=500)
        >>> for chunk in loader.load_chunks("data.csv"):
        ...     process(chunk)
    """
    # Loader 클래스 찾기
    loader_class = _loader_class_registry.get((domain, format))
    if loader_class is None:
        raise ValueError(
            f"지원하지 않는 조합: domain={domain}, format={format}\n"
            f"사용 가능한 조합: {list(_loader_class_registry.keys())}"
        )

    # Row 클래스 찾기
    row_class = _domain_row_class_map.get(domain)
    if row_class is None:
        raise ValueError(
            f"지원하지 않는 도메인: {domain}\n"
            f"사용 가능한 도메인: {list(_domain_row_class_map.keys())}"
        )

    # Loader 인스턴스 생성
    return loader_class(row_class=row_class, chunk_size=chunk_size)


def get_loader_auto(domain: str, file_path: str, chunk_size: int = 1000) -> BaseChunkLoader:
    """
    파일 확장자로 포맷을 자동 감지하여 Loader 반환 (편의 함수)

    Args:
        domain: 도메인 이름
        file_path: 파일 경로 (확장자로 포맷 자동 감지)
        chunk_size: Chunk 크기

    Returns:
        BaseChunkLoader: 해당 도메인/포맷에 맞는 Loader 인스턴스

    Example:
        >>> loader = get_loader_auto("recruit", "data/recruit.csv")
        >>> for chunk in loader.load_chunks("data/recruit.csv"):
        ...     process(chunk)
    """
    ext = file_path.split('.')[-1].lower()

    format_map = {
        "pkl": DataFormat.PKL,
        "csv": DataFormat.CSV,
        "parquet": DataFormat.PARQUET,
    }

    format = format_map.get(ext)
    if not format:
        raise ValueError(
            f"지원하지 않는 파일 확장자: {ext}\n"
            f"지원 가능한 확장자: {list(format_map.keys())}"
        )

    return get_loader(domain, format, chunk_size)
