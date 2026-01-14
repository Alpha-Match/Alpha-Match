"""
API 엔드포인트 (API Endpoints)

- FastAPI를 사용하여 외부 요청을 받는 HTTP 엔드포인트를 정의합니다.
- 각 엔드포인트는 적절한 서비스(비즈니스 로직)를 호출하고,
  그 결과를 estful API 응답 형식으로 변환하여 반환합니다.
"""
from fastapi import APIRouter, Query, HTTPException, status
from pydantic import BaseModel

# 프로젝트 내 다른 모듈 임포트
from ..services.ingestion_service import ingest_data_from_file
from ..proto import embedding_stream_pb2
from ..config.settings import data_config

# 로깅 설정
import logging
logger = logging.getLogger(__name__)


# FastAPI 라우터를 생성합니다.
# 라우터를 사용하면 엔드포인트를 모듈화하여 관리할 수 있습니다.
router = APIRouter(
    prefix="/data",  # 이 라우터의 모든 경로는 "/data"로 시작합니다.
    tags=["Data Ingestion"], # FastAPI 문서에서 엔드포인트를 그룹화하는 태그입니다.
)


# --- API 응답 모델 ---
class IngestionResponseModel(BaseModel):
    """
    데이터 수집 API의 최종 응답을 위한 Pydantic 모델입니다.
    gRPC 응답을 JSON 형식으로 변환하는 데 사용됩니다.
    """
    success: bool
    received_chunks: int
    message: str


# --- API 엔드포인트 정의 ---

@router.post(
    "/ingest/{domain}",
    response_model=IngestionResponseModel,
    summary="도메인 데이터 수집 및 gRPC 스트리밍 트리거",
    description="""
    지정된 도메인의 .pkl 파일을 로드하여, gRPC 클라이언트 스트리밍을 통해
    배치 서버로 데이터 전송을 시작합니다.
    """
)
async def trigger_ingestion(
    domain: str,
    file_name: str = Query(
        ...,
        description="data 폴더에 위치한 .pkl 파일의 이름",
        examples=["processed_recruitment_data.pkl"]
    ),
    chunk_size: int = Query(
        default=100,
        ge=50,
        le=1000,
        description="gRPC 스트리밍 청크 크기 (기본: 100, 범위: 50-1000)"
    ),
):
    """
    데이터 수집(Ingestion) 파이프라인을 시작하는 엔드포인트입니다.

    - **domain**: 처리할 데이터의 종류 (예: "recruit", "candidate").
    - **file_name**: 처리할 파일의 이름.
    - **chunk_size**: gRPC 스트리밍 청크 크기 (기본 100, Batch Writer는 300 고정).
    """
    full_file_path = f"{data_config.DATA_DIR}/{file_name}"
    logger.info(f"API 요청 수신: POST /data/ingest/{domain}?file_name={file_name}&chunk_size={chunk_size}")

    try:
        # 서비스 계층의 핵심 로직을 호출합니다.
        grpc_response: embedding_stream_pb2.IngestDataResponse = await ingest_data_from_file(
            domain=domain,
            file_path=full_file_path,
            chunk_size=chunk_size
        )

        # gRPC 응답 객체를 Pydantic 모델(JSON으로 변환 가능)로 변환하여 반환합니다.
        return IngestionResponseModel(
            success=grpc_response.success,
            received_chunks=grpc_response.received_chunks,
            message=grpc_response.message
        )

    except FileNotFoundError as e:
        logger.error(f"파일을 찾을 수 없음: {full_file_path}")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )
    except ValueError as e:
        logger.error(f"잘못된 요청 값: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        logger.error(f"내부 서버 오류 발생: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"내부 서버 오류가 발생했습니다: {e}"
        )
