"""
비즈니스 로직 서비스 (Business Logic Services)

- 애플리케이션의 핵심 비즈니스 로직을 처리하는 계층입니다.
- API 계층(FastAPI 엔드포인트)과 인프라 계층(데이터 로더, gRPC 클라이언트)을 연결하는
  중재자 역할을 수행합니다.
- Chunk 기반 스트리밍 처리로 메모리 효율성 확보
"""
from typing import List
from ..proto import embedding_stream_pb2
# 프로젝트 내 다른 모듈 임포트
from ..infrastructure.loaders import get_loader_auto
from ..infrastructure.grpc_clients import stream_data_to_batch_server
from ..config.settings import data_config
from ..domain.models import BaseData

# 로깅 설정
import logging
logger = logging.getLogger(__name__)


async def ingest_data_from_file(domain: str, file_path: str, chunk_size: int = 1000) -> embedding_stream_pb2.IngestDataResponse:
    """
    지정된 도메인과 파일 경로에 대해 데이터 수집(ingestion) 프로세스를 수행합니다.

    Chunk 기반 스트리밍 처리:
    1. 파일을 Chunk 단위로 읽어서 메모리 효율성 확보
    2. 각 Chunk를 Pydantic 모델로 변환하여 Validation 수행
    3. gRPC 클라이언트 스트리밍을 통해 Batch Server로 전송

    Args:
        domain (str): 처리할 데이터의 도메인 (예: "recruit", "candidate", "skill_dic")
        file_path (str): 로드할 데이터 파일의 전체 경로
        chunk_size (int): Chunk 크기 (기본 1000 rows)

    Returns:
        IngestDataResponse: gRPC 배치 서버의 최종 응답 메시지

    Raises:
        ValueError: 지원하지 않는 도메인, 포맷 또는 데이터 검증 실패
        FileNotFoundError: 파일을 찾을 수 없음
        IOError: 파일 로딩 중 오류
    """
    try:
        logger.info(f"[{domain}] 데이터 수집 시작: {file_path} (chunk_size={chunk_size})")

        # 1. 도메인 설정 확인
        domain_cfg = data_config.DOMAIN_CONFIGS.get(domain)
        if not domain_cfg:
            raise ValueError(f"'{domain}' 도메인에 대한 설정이 config/settings.py에 존재하지 않습니다.")

        # 2. 파일 확장자 자동 감지 + Loader 생성
        logger.debug(f"파일 확장자 자동 감지 중...")
        loader = get_loader_auto(domain, file_path, chunk_size=chunk_size)
        logger.info(f"사용할 로더: {type(loader).__name__} (format={loader.__class__.__name__.replace('ChunkLoader', '')})")

        # 3. Chunk 단위로 데이터 로드 및 스트리밍
        logger.debug(f"Chunk 단위 데이터 로딩 시작...")

        all_chunks: List[List[BaseData]] = []
        total_rows = 0

        for chunk_idx, chunk_data in enumerate(loader.load_chunks(file_path)):
            # 첫 번째 Chunk에서 벡터 차원 검증 (v2: skills_vector, skill_vector 지원)
            if chunk_idx == 0 and chunk_data:
                # v2: 도메인별 벡터 필드명 확인
                first_row = chunk_data[0]
                vector_field = None
                if hasattr(first_row, 'skills_vector'):
                    vector_field = first_row.skills_vector
                elif hasattr(first_row, 'skill_vector'):
                    vector_field = first_row.skill_vector
                elif hasattr(first_row, 'vector'):  # legacy v1 지원
                    vector_field = first_row.vector

                actual_dimension = len(vector_field) if vector_field else 0
                if actual_dimension != domain_cfg.vector_dimension:
                    raise ValueError(
                        f"데이터 차원 수 불일치! '{domain}' 도메인의 설정된 차원 수는 "
                        f"{domain_cfg.vector_dimension}이지만, 실제 데이터의 차원 수는 {actual_dimension}입니다."
                    )
                logger.info(f"벡터 차원 검증 완료: {actual_dimension}d")

            all_chunks.append(chunk_data)
            total_rows += len(chunk_data)
            logger.debug(f"Chunk {chunk_idx + 1} 로드 완료: {len(chunk_data)} rows (누적: {total_rows})")

        if total_rows == 0:
            raise ValueError("파일에서 로드한 데이터가 없습니다.")

        logger.info(f"총 {total_rows}개 행(row) 로드 완료 ({len(all_chunks)} chunks)")

        # 4. gRPC를 통해 배치 서버로 데이터 스트리밍
        # 전체 데이터를 평탄화하여 전송 (향후 Chunk 단위 스트리밍으로 개선 가능)
        flattened_data = [row for chunk in all_chunks for row in chunk]

        logger.debug(f"gRPC 클라이언트 스트리밍 시작 (chunk_size={chunk_size})...")
        response = await stream_data_to_batch_server(
            domain=domain,
            file_name=file_path.split('/')[-1].split('\\')[-1],  # 순수 파일명만 추출
            data=flattened_data,
            chunk_size=chunk_size  # API에서 전달받은 청크 크기 사용
        )
        logger.info(f"[{domain}] 데이터 수집 완료: {total_rows} rows 전송")

        return response

    except (ValueError, FileNotFoundError, IOError) as e:
        logger.error(f"[{domain}] 데이터 수집 실패: {e}")
        raise
    except Exception as e:
        logger.error(f"[{domain}] 예기치 않은 오류: {e}", exc_info=True)
        raise
