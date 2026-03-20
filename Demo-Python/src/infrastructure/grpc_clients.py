"""
gRPC 클라이언트 (gRPC Clients)

- 배치 서버(Java)와 통신하는 gRPC 클라이언트 로직을 포함합니다.
- 비동기 방식으로 gRPC 서버의 RPC를 호출합니다.
"""
import json
# 로깅 설정
import logging
from typing import List, AsyncGenerator

import grpc
from ..config.settings import server_config
from ..domain.models import BaseData, RecruitData, CandidateData, SkillEmbeddingDicData
# 프로젝트 내 다른 모듈 및 생성된 proto 파일 임포트
from ..proto import embedding_stream_pb2
from ..proto import embedding_stream_pb2_grpc

logger = logging.getLogger(__name__)


def _to_recruit_row(r: RecruitData) -> embedding_stream_pb2.RecruitRow:
    return embedding_stream_pb2.RecruitRow(
        id=r.id,
        position=r.position,
        company_name=r.company_name,
        experience_years=r.experience_years or 0,
        primary_keyword=r.primary_keyword or "",
        english_level=r.english_level or "",
        published_at=r.published_at or "",
        skills=r.skills,
        long_description=r.long_description or "",
        description_lang=r.description_lang or "",
        skills_vector=r.skills_vector,
    )


def _to_candidate_row(r: CandidateData) -> embedding_stream_pb2.CandidateRow:
    return embedding_stream_pb2.CandidateRow(
        candidate_id=r.candidate_id,
        position_category=r.position_category,
        experience_years=r.experience_years,
        original_resume=r.original_resume,
        skills=r.skills,
        skills_vector=r.skills_vector,
        resume_lang=r.resume_lang or "",
        moreinfo=r.moreinfo or "",
        looking_for=r.looking_for or "",
    )


def _to_skill_dic_row(r: SkillEmbeddingDicData) -> embedding_stream_pb2.SkillEmbeddingDicRow:
    return embedding_stream_pb2.SkillEmbeddingDicRow(
        skill=r.skill,
        position_category=r.position_category,
        skill_vector=r.skill_vector,
    )


async def _prepare_ingest_requests(
    domain: str,
    file_name: str,
    vector_dimension: int,
    data: List[BaseData],
    chunk_size: int
) -> AsyncGenerator[embedding_stream_pb2.IngestDataRequest, None]:
    """
    gRPC 클라이언트 스트리밍을 위한 요청 생성기(비동기 제너레이터)입니다.

    첫 번째 요청으로 메타데이터를 전송하고, 이후 Protobuf 이진 인코딩된 RowChunk를 순차적으로 전송합니다.

    Args:
        domain (str): 데이터 도메인.
        file_name (str): 원본 파일 이름.
        vector_dimension (int): 벡터 차원.
        data (List[BaseData]): 전송할 전체 데이터 리스트 (RecruitData, CandidateData, etc.).
        chunk_size (int): 한 번에 전송할 데이터 청크의 크기.

    Yields:
        IngestDataRequest: gRPC 서버로 보낼 요청 메시지.
    """
    # 1. 메타데이터 전송
    total_chunks = (len(data) + chunk_size - 1) // chunk_size
    metadata = embedding_stream_pb2.IngestMetadata(
        domain=domain,
        file_name=file_name,
        vector_dimension=vector_dimension
    )
    yield embedding_stream_pb2.IngestDataRequest(metadata=metadata)
    logger.info(f"스트림 시작: 메타데이터 전송 (도메인: {domain}, 파일: {file_name})")

    # 2. 데이터 청크 순차적 전송 (Protobuf 이진 인코딩)
    for i in range(0, len(data), chunk_size):
        chunk_data = data[i:i + chunk_size]
        chunk_index = i // chunk_size + 1

        # Pydantic 모델 → Protobuf Row → RowChunk 변환
        if domain == "recruit":
            proto_rows = [_to_recruit_row(r) for r in chunk_data]
            row_chunk = embedding_stream_pb2.RowChunk(
                recruit=embedding_stream_pb2.RecruitRowChunk(rows=proto_rows)
            )
        elif domain == "candidate":
            proto_rows = [_to_candidate_row(r) for r in chunk_data]
            row_chunk = embedding_stream_pb2.RowChunk(
                candidate=embedding_stream_pb2.CandidateRowChunk(rows=proto_rows)
            )
        elif domain == "skill_dic":
            proto_rows = [_to_skill_dic_row(r) for r in chunk_data]
            row_chunk = embedding_stream_pb2.RowChunk(
                skill_dic=embedding_stream_pb2.SkillEmbeddingDicRowChunk(rows=proto_rows)
            )
        else:
            raise ValueError(f"지원하지 않는 도메인: {domain}")

        # [MEASURE] Protobuf vs JSON 크기 비교 (측정 완료 후 json_bytes 라인 제거 예정)
        proto_bytes = row_chunk.ByteSize()
        json_bytes = len(json.dumps([r.model_dump() for r in chunk_data]).encode('utf-8'))
        logger.info(
            f"[MEASURE] Chunk {chunk_index}/{total_chunks}: "
            f"proto={proto_bytes:,}B  json={json_bytes:,}B  "
            f"reduction={100*(1-proto_bytes/json_bytes):.1f}%"
        )

        yield embedding_stream_pb2.IngestDataRequest(data_chunk=row_chunk)
        logger.debug(f"데이터 청크 {chunk_index}/{total_chunks} 전송 ({len(chunk_data)} rows)")

    logger.info("모든 데이터 청크 전송 완료")


async def stream_data_to_batch_server(
    domain: str,
    file_name: str,
    data: List[BaseData],
    chunk_size: int = 100  # v3: baseline 복원 (Virtual Thread 병렬 쓰기 비교용)
) -> embedding_stream_pb2.IngestDataResponse:
    """
    클라이언트 스트리밍을 통해 배치 서버로 데이터를 전송합니다.

    Args:
        domain (str): 데이터 도메인.
        file_name (str): 원본 파일 이름.
        data (List[BaseData]): 전송할 Pydantic 모델 객체 리스트 (RecruitData, CandidateData, etc.).
        chunk_size (int): 한 번에 묶어서 보낼 데이터 행의 수.

    Returns:
        IngestDataResponse: 배치 서버로부터의 최종 응답.
    """
    if not data:
        logger.warning("전송할 데이터가 없습니다.")
        return embedding_stream_pb2.IngestDataResponse(
            success=False, received_chunks=0, message="전송할 데이터가 없습니다."
        )

    # v2: 도메인별 벡터 필드명 확인 (skills_vector, skill_vector, vector)
    first_row = data[0]
    vector_field = None
    if hasattr(first_row, 'skills_vector'):
        vector_field = first_row.skills_vector
    elif hasattr(first_row, 'skill_vector'):
        vector_field = first_row.skill_vector
    elif hasattr(first_row, 'vector'):  # legacy v1 지원
        vector_field = first_row.vector

    vector_dimension = len(vector_field) if vector_field else 0

    try:
        async with grpc.aio.insecure_channel(server_config.BATCH_SERVER_ADDRESS) as channel:
            stub = embedding_stream_pb2_grpc.EmbeddingStreamServiceStub(channel)

            # 비동기 제너레이터를 생성하여 stub에 전달
            request_generator = _prepare_ingest_requests(
                domain, file_name, vector_dimension, data, chunk_size
            )

            logger.info(f"배치 서버({server_config.BATCH_SERVER_ADDRESS})로 데이터 스트리밍을 시작합니다...")
            response = await stub.IngestDataStream(request_generator)
            logger.info("배치 서버로부터 최종 응답을 수신했습니다.")
            return response

    except grpc.aio.AioRpcError as e:
        logger.error(f"gRPC 통신 실패: {e.code()} - {e.details()}")
        raise
    except Exception as e:
        logger.error(f"데이터 스트리밍 중 예기치 않은 오류 발생: {e}")
        raise
