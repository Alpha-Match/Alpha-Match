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
            pkl_data = pd.read_pickle(file_path)

            # pkl 파일이 dict 형태인 경우 data_frame 키로 접근
            if isinstance(pkl_data, dict) and 'data_frame' in pkl_data:
                df = pkl_data['data_frame']
            elif isinstance(pkl_data, list):
                # list 형태인 경우 DataFrame으로 변환
                df = pd.DataFrame(pkl_data)
            else:
                df = pkl_data

            total_rows_before = len(df)
            logger.info(f"Total rows loaded: {total_rows_before}")

            # v2: 도메인별 전처리 적용
            if self.row_class.__name__ == 'RecruitData':
                df = self._preprocess_recruit_data(df)
            elif self.row_class.__name__ == 'CandidateData':
                df = self._preprocess_candidate_data(df)
            elif self.row_class.__name__ == 'SkillEmbeddingDicData':
                df = self._preprocess_skill_dic_data(df)

            total_rows_after = len(df)
            if total_rows_before != total_rows_after:
                logger.info(f"Rows after preprocessing: {total_rows_after} (filtered: {total_rows_before - total_rows_after})")

            # Chunk 단위로 분할
            for i in range(0, total_rows_after, self.chunk_size):
                chunk_df = df.iloc[i:i+self.chunk_size]

                # v2: numpy array를 list로 변환 (skills, skills_vector) + NaN을 None으로 변환
                chunk_records = chunk_df.to_dict('records')
                for record in chunk_records:
                    # skills 변환
                    if 'skills' in record and hasattr(record['skills'], 'tolist'):
                        record['skills'] = record['skills'].tolist()
                    # skills_vector 변환 (v3: string으로 저장된 경우 JSON 파싱)
                    if 'skills_vector' in record:
                        if isinstance(record['skills_vector'], str):
                            import json
                            record['skills_vector'] = json.loads(record['skills_vector'])
                        elif hasattr(record['skills_vector'], 'tolist'):
                            record['skills_vector'] = record['skills_vector'].tolist()
                    # skills_openai_vector → skills_vector 매핑 (v3: pkl 컬럼명)
                    if 'skills_openai_vector' in record:
                        if isinstance(record['skills_openai_vector'], str):
                            import json
                            record['skills_vector'] = json.loads(record['skills_openai_vector'])
                        elif hasattr(record['skills_openai_vector'], 'tolist'):
                            record['skills_vector'] = record['skills_vector'].tolist()
                        else:
                            record['skills_vector'] = record['skills_openai_vector']
                        del record['skills_openai_vector']  # 원본 컬럼 제거
                    # skill_vector 변환 (SkillEmbeddingDic용)
                    if 'skill_vector' in record:
                        if isinstance(record['skill_vector'], str):
                            import json
                            record['skill_vector'] = json.loads(record['skill_vector'])
                        elif hasattr(record['skill_vector'], 'tolist'):
                            record['skill_vector'] = record['skill_vector'].tolist()
                    # NaN을 None으로 변환 (Pydantic 호환성)
                    for key, value in list(record.items()):
                        if isinstance(value, float) and pd.isna(value):
                            record[key] = None

                chunk_data = [self.row_class(**row) for row in chunk_records]

                logger.debug(f"Yielding chunk {i//self.chunk_size + 1}: {len(chunk_data)} rows")
                yield chunk_data

        except FileNotFoundError:
            logger.error(f"File not found: {file_path}")
            raise FileNotFoundError(f"데이터 파일을 찾을 수 없습니다: {file_path}")
        except Exception as e:
            logger.error(f"Error loading pkl file: {e}")
            raise IOError(f"pkl 파일 로딩 중 오류 발생: {e}")

    def _preprocess_recruit_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Recruit 도메인 전처리 (table_specification.md 요구사항)

        1. 컬럼명 매핑 (Title Case → snake_case)
        2. Exp Years 전처리 ('no_exp' → null, 'Ny' → Integer)
        3. 불필요한 컬럼 삭제
        4. skills 빈 배열 제외 (실제로는 0개지만 방어 코드)
        5. 벡터 누락 행 필터링
        """
        logger.info("Applying Recruit domain preprocessing...")

        # 1. 컬럼명 매핑 (Title Case → snake_case, v3: skills_openai_vector 지원)
        column_mapping = {
            'Position': 'position',
            'Company Name': 'company_name',
            'Exp Years': 'experience_years',
            'Primary Keyword': 'primary_keyword',
            'English Level': 'english_level',
            'Published': 'published_at',
            'Long Description': 'long_description',
            'Long Description_lang': 'description_lang',
            'skill_vector': 'skills_vector',  # v2 호환성
            'skills_openai_vector': 'skills_vector',  # v3: OpenAI 1536d
            'skills': 'skills',
            'id': 'id'
        }
        df = df.rename(columns=column_mapping)

        # 2. Exp Years 전처리
        def convert_exp_years(value):
            """'no_exp' → None, 'Ny' → Integer"""
            if pd.isna(value) or value == 'no_exp':
                return None
            if isinstance(value, str) and value.endswith('y'):
                try:
                    return int(value[:-1])
                except ValueError:
                    return None
            if isinstance(value, int):
                return value
            return None

        df['experience_years'] = df['experience_years'].apply(convert_exp_years)
        logger.debug(f"Exp Years converted: no_exp → None, 'Ny' → Integer")

        # 3. 불필요한 컬럼 삭제
        unnecessary_cols = [
            '__index_level_0__',
            'normalized_skills',
            'embedding_input_text',
            'embedding_sample',
            'db_id'  # v2: Remove database ID column if present
        ]
        existing_unnecessary = [col for col in unnecessary_cols if col in df.columns]
        if existing_unnecessary:
            df = df.drop(columns=existing_unnecessary)
            logger.debug(f"Dropped columns: {', '.join(existing_unnecessary)}")

        # 4. skills 빈 배열 제외 (numpy array와 list 모두 지원)
        before_skill_filter = len(df)
        def has_skills(x):
            """skills가 비어있지 않은지 확인 (ndarray 또는 list)"""
            import numpy as np
            # numpy array 또는 list의 길이 확인
            try:
                return len(x) > 0
            except (TypeError, AttributeError):
                # None 또는 길이가 없는 객체
                return False

        df = df[df['skills'].apply(has_skills)]
        after_skill_filter = len(df)
        if before_skill_filter != after_skill_filter:
            logger.info(f"Filtered empty skills: {before_skill_filter - after_skill_filter} rows")

        # 5. 벡터 누락 행 필터링
        before_vector_filter = len(df)
        df = df[df['skills_vector'].notna()]
        after_vector_filter = len(df)
        if before_vector_filter != after_vector_filter:
            logger.info(f"Filtered null vectors: {before_vector_filter - after_vector_filter} rows")

        # 필요한 컬럼만 선택 (v2 스키마 순서대로)
        required_cols = [
            'id', 'position', 'company_name', 'experience_years',
            'primary_keyword', 'english_level', 'published_at',
            'skills', 'long_description', 'description_lang', 'skills_vector'
        ]
        df = df[required_cols]

        logger.info("Recruit preprocessing completed")
        return df

    def _preprocess_candidate_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Candidate 도메인 전처리 (table_specification.md 요구사항)

        1. 컬럼명 매핑 (Title Case → snake_case)
        2. Experience Years 전처리 (float → int, NaN 처리)
        3. 불필요한 컬럼 삭제
        4. skills 빈 배열 제외
        5. 벡터 누락 행 필터링
        6. 벡터 차원 검증 (1536d)
        """
        logger.info("Applying Candidate domain preprocessing...")

        # 1. 컬럼명 매핑 (Title Case → snake_case, v3: skills_openai_vector 지원)
        column_mapping = {
            'id': 'candidate_id',                  # BaseData.id → candidate_id
            'Primary Keyword': 'position_category',
            'Experience Years': 'experience_years',
            'CV': 'original_resume',
            'CV_lang': 'resume_lang',              # v3 신규
            'Moreinfo': 'moreinfo',                # v3 신규
            'Looking For': 'looking_for',          # v3 신규
            'skills': 'skills',
            'skill_vector': 'skills_vector',  # v2 호환성
            'skills_openai_vector': 'skills_vector'  # v3: OpenAI 1536d
        }
        df = df.rename(columns=column_mapping)

        # 2. Position Category 전처리 (NaN → 'Unknown')
        before_position_filter = len(df)
        df['position_category'] = df['position_category'].fillna('Unknown')
        after_position_filter = len(df)
        if before_position_filter != after_position_filter:
            logger.info(f"Filled null position_category: {before_position_filter - after_position_filter} rows")

        # 3. Experience Years 전처리 (float → int)
        def convert_exp_years(value):
            """float64 → int, NaN → None (NULL 저장)"""
            if pd.isna(value):
                return None  # NULL for candidates with unknown experience
            try:
                return int(round(value))  # Round and convert to int
            except (ValueError, TypeError):
                return None

        df['experience_years'] = df['experience_years'].apply(convert_exp_years)
        logger.debug(f"Experience Years converted: float64 → int, NaN → None")

        # 4. 불필요한 컬럼 삭제
        unnecessary_cols = [
            '__index_level_0__',
            'normalized_skills',
            'embedding_input_text',
            'embedding_sample',
            'Position',          # Display field, not needed
            'Highlights',        # Achievements, not needed
            'English Level',     # Language level, not needed
            'db_id'              # v3: Remove database ID column if present
        ]
        existing_unnecessary = [col for col in unnecessary_cols if col in df.columns]
        if existing_unnecessary:
            df = df.drop(columns=existing_unnecessary)
            logger.debug(f"Dropped columns: {', '.join(existing_unnecessary)}")

        # 5. skills 빈 배열 제외 (numpy array와 list 모두 지원)
        before_skill_filter = len(df)
        def has_skills(x):
            """skills가 비어있지 않은지 확인 (ndarray 또는 list)"""
            import numpy as np
            try:
                return len(x) > 0
            except (TypeError, AttributeError):
                # None 또는 길이가 없는 객체
                return False

        df = df[df['skills'].apply(has_skills)]
        after_skill_filter = len(df)
        if before_skill_filter != after_skill_filter:
            logger.info(f"Filtered empty skills: {before_skill_filter - after_skill_filter} rows")

        # 6. 벡터 누락 행 필터링
        before_vector_filter = len(df)
        df = df[df['skills_vector'].notna()]
        after_vector_filter = len(df)
        if before_vector_filter != after_vector_filter:
            logger.info(f"Filtered null vectors: {before_vector_filter - after_vector_filter} rows")

        # 7. 벡터 차원 검증 (384d) - 로깅만, 실제 검증은 Pydantic에서 수행
        if len(df) > 0:
            sample_vector = df['skills_vector'].iloc[0]
            if hasattr(sample_vector, 'shape'):
                logger.debug(f"Vector dimension: {sample_vector.shape[0]}")
            elif isinstance(sample_vector, list):
                logger.debug(f"Vector dimension: {len(sample_vector)}")

        # 필요한 컬럼만 선택 (v3 스키마 순서대로)
        required_cols = [
            'candidate_id', 'position_category', 'experience_years',
            'original_resume', 'resume_lang', 'moreinfo', 'looking_for',
            'skills', 'skills_vector'
        ]
        df = df[required_cols]

        logger.info("Candidate preprocessing completed")
        return df

    def _preprocess_skill_dic_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        SkillEmbeddingDic 도메인 전처리

        1. 컬럼명 매핑 (pkl → models.py)
        2. 불필요한 컬럼 삭제
        3. 벡터 누락 행 필터링
        """
        logger.info("Applying SkillEmbeddingDic domain preprocessing...")

        # 1. 컬럼명 매핑
        column_mapping = {
            'name': 'skill',                              # v3: name → skill
            'category': 'position_category',              # category → position_category
            'skill_set_openai_vector': 'skill_vector',    # v3: OpenAI 1536d
            'id': 'id'                                    # id (사용하지 않지만 유지)
        }
        df = df.rename(columns=column_mapping)

        # 2. 불필요한 컬럼 삭제
        unnecessary_cols = [
            'id',
            '__index_level_0__',
            'synonyms'  # synonyms 컬럼 제외 (SkillEmbeddingDic에서는 불필요)
        ]
        existing_unnecessary = [col for col in unnecessary_cols if col in df.columns]
        if existing_unnecessary:
            df = df.drop(columns=existing_unnecessary)
            logger.debug(f"Dropped columns: {', '.join(existing_unnecessary)}")

        # 3. 벡터 누락 행 필터링
        before_vector_filter = len(df)
        df = df[df['skill_vector'].notna()]
        after_vector_filter = len(df)
        if before_vector_filter != after_vector_filter:
            logger.info(f"Filtered null vectors: {before_vector_filter - after_vector_filter} rows")

        # 필요한 컬럼만 선택
        required_cols = ['skill', 'position_category', 'skill_vector']
        df = df[required_cols]

        logger.info("SkillEmbeddingDic preprocessing completed")
        return df


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
