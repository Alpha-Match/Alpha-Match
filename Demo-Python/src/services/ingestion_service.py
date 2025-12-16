"""
비즈니스 로직 서비스 (Business Logic Services)

- 애플리케이션의 핵심 비즈니스 로직을 처리하는 계층입니다.
- API 계층(FastAPI 엔드포인트)과 인프라 계층(데이터 로더, gRPC 클라이언트)을 연결하는
  중재자 역할을 수행합니다.
"""
from ..proto import embedding_stream_pb2
# 프로젝트 내 다른 모듈 임포트
from ..infrastructure.loaders import get_loader
from ..infrastructure.grpc_clients import stream_data_to_batch_server
from ..config.settings import data_config

# 로깅 설정
import logging
logger = logging.getLogger(__name__)


async def ingest_data_from_file(domain: str, file_path: str) -> embedding_stream_pb2.IngestDataResponse:
    """
    지정된 도메인과 파일 경로에 대해 데이터 수집(ingestion) 프로세스를 수행합니다.

    이 함수는 전체 데이터 처리 흐름을 관장합니다.
    1. 도메인에 맞는 데이터 로더를 가져옵니다.
    2. 파일을 로드하여 데이터 모델 리스트로 변환합니다.
    3. 설정과 실제 데이터의 유효성(예: 벡터 차원)을 검증합니다.
    4. gRPC 클라이언트 스트리밍을 통해 데이터를 배치 서버로 전송합니다.
    5. 배치 서버의 최종 처리 결과를 반환합니다.

    Args:
        domain (str): 처리할 데이터의 도메인 (예: "recruit").
        file_path (str): 로드할 데이터 파일의 전체 경로.

    Returns:
        IngestDataResponse: gRPC 배치 서버의 최종 응답 메시지.
    """
    try:
        logger.info(f"'{domain}' 도메인 데이터 수집 서비스 시작 (파일: {file_path})")

        # 1. 도메인에 맞는 로더 가져오기
        logger.debug(f"도메인 '{domain}'에 대한 로더를 찾는 중...")
        loader = get_loader(domain)
        logger.info(f"사용할 로더: {type(loader).__name__}")

        # 2. 파일에서 데이터 로드
        logger.debug(f"'{file_path}' 파일 로딩 중...")
        data = loader.load(file_path)
        logger.info(f"총 {len(data)}개의 행(row) 데이터 로드 완료")

        if not data:
            raise ValueError("파일에서 로드한 데이터가 없습니다.")

        # 3. 데이터 유효성 검증 (신규 로직)
        # 설정에서 해당 도메인의 설정값 가져오기
        domain_cfg = data_config.DOMAIN_CONFIGS.get(domain)
        if not domain_cfg:
            raise ValueError(f"'{domain}' 도메인에 대한 설정이 config/settings.py에 존재하지 않습니다.")

        # 실제 데이터의 차원 수와 설정된 차원 수 비교
        actual_dimension = len(data[0].vector) if data and hasattr(data[0], 'vector') and data[0].vector else 0
        if actual_dimension != domain_cfg.vector_dimension:
            raise ValueError(
                f"데이터 차원 수 불일치! '{domain}' 도메인의 설정된 차원 수는 "
                f"{domain_cfg.vector_dimension}이지만, 실제 데이터의 차원 수는 {actual_dimension}입니다."
            )
        logger.info(f"데이터 차원 수 검증 완료 (실제: {actual_dimension}, 설정: {domain_cfg.vector_dimension})")

        # 4. gRPC를 통해 배치 서버로 데이터 스트리밍
        logger.debug("gRPC 클라이언트 스트리밍을 통해 데이터 전송 시작...")
        response = await stream_data_to_batch_server(
            domain=domain,
            file_name=file_path.split('/')[-1].split('\\')[-1], # 순수 파일명만 추출
            data=data
        )
        logger.info("데이터 수집 및 전송 프로세스 완료")

        # 5. 최종 결과 반환
        return response

    except (ValueError, FileNotFoundError) as e:
        logger.error(f"데이터 수집 준비 중 오류 발생: {e}")
        # 이런 경우, gRPC 호출 없이 클라이언트에게 바로 에러를 알려야 합니다.
        # FastAPI 핸들러에서 이 예외를 처리하여 적절한 HTTP 응답을 보내야 합니다.
        raise
    except Exception as e:
        logger.error(f"데이터 수집 서비스 실행 중 예기치 않은 오류 발생: {e}")
        raise
