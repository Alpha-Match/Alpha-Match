"""
Protobuf vs JSON Payload 크기 실측 스크립트

- 실제 pkl 데이터 파일에서 청크를 로드하여 Protobuf/JSON 직렬화 크기를 비교합니다.
- 도메인별(recruit, candidate, skill_dic) 전체 데이터 실측
- 결과: 총 바이트, 절감률, 포트폴리오 기재용 수치
"""
import sys
import os
import json
import logging
import time

# Demo-Python/src를 경로에 추가
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from src.infrastructure.loaders import PklChunkLoader, DataFormat, get_loader
from src.domain.models import RecruitData, CandidateData, SkillEmbeddingDicData
from src.proto import embedding_stream_pb2

logging.basicConfig(
    level=logging.WARNING,  # 로더 INFO 로그 억제
    format='%(asctime)s [%(levelname)s] %(message)s'
)
logger = logging.getLogger(__name__)

DATA_DIR = os.path.join(os.path.dirname(__file__), 'data')
CHUNK_SIZE = 100


# ============================================================================
# Pydantic → Protobuf Row 변환 (grpc_clients.py와 동일)
# ============================================================================

def to_recruit_row(r: RecruitData) -> embedding_stream_pb2.RecruitRow:
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


def to_candidate_row(r: CandidateData) -> embedding_stream_pb2.CandidateRow:
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


def to_skill_dic_row(r: SkillEmbeddingDicData) -> embedding_stream_pb2.SkillEmbeddingDicRow:
    return embedding_stream_pb2.SkillEmbeddingDicRow(
        skill=r.skill,
        position_category=r.position_category,
        skill_vector=r.skill_vector,
    )


# ============================================================================
# 도메인별 실측 함수
# ============================================================================

def measure_domain(domain: str, file_path: str, row_converter, chunk_builder):
    """
    단일 도메인 전체 파일에 대해 Protobuf vs JSON 크기를 측정합니다.

    Returns:
        dict: {total_rows, total_proto_bytes, total_json_bytes, chunks, elapsed_sec}
    """
    print(f"\n{'='*60}")
    print(f"  [{domain.upper()}] 실측 시작: {os.path.basename(file_path)}")
    print(f"{'='*60}")

    loader = get_loader(domain, DataFormat.PKL, chunk_size=CHUNK_SIZE)

    total_rows = 0
    total_proto_bytes = 0
    total_json_bytes = 0
    chunk_count = 0
    start = time.time()

    for chunk_data in loader.load_chunks(file_path):
        chunk_count += 1

        # Protobuf 직렬화 크기
        proto_rows = [row_converter(r) for r in chunk_data]
        row_chunk = chunk_builder(proto_rows)
        proto_bytes = row_chunk.ByteSize()

        # JSON 직렬화 크기
        json_bytes = len(json.dumps([r.model_dump() for r in chunk_data]).encode('utf-8'))

        total_rows += len(chunk_data)
        total_proto_bytes += proto_bytes
        total_json_bytes += json_bytes

        reduction = 100 * (1 - proto_bytes / json_bytes) if json_bytes > 0 else 0
        print(f"  chunk {chunk_count:>4} ({len(chunk_data):>4} rows): "
              f"proto={proto_bytes:>10,}B  json={json_bytes:>10,}B  "
              f"reduction={reduction:5.1f}%")

    elapsed = time.time() - start
    return {
        'domain': domain,
        'total_rows': total_rows,
        'total_proto_bytes': total_proto_bytes,
        'total_json_bytes': total_json_bytes,
        'chunks': chunk_count,
        'elapsed_sec': elapsed,
    }


def build_recruit_chunk(proto_rows):
    return embedding_stream_pb2.RowChunk(
        recruit=embedding_stream_pb2.RecruitRowChunk(rows=proto_rows)
    )


def build_candidate_chunk(proto_rows):
    return embedding_stream_pb2.RowChunk(
        candidate=embedding_stream_pb2.CandidateRowChunk(rows=proto_rows)
    )


def build_skill_dic_chunk(proto_rows):
    return embedding_stream_pb2.RowChunk(
        skill_dic=embedding_stream_pb2.SkillEmbeddingDicRowChunk(rows=proto_rows)
    )


# ============================================================================
# 메인 실측
# ============================================================================

def format_gb(byte_count: int) -> str:
    return f"{byte_count / (1024**3):.3f} GB"


def format_mb(byte_count: int) -> str:
    return f"{byte_count / (1024**2):.1f} MB"


def main():
    domains = [
        {
            'domain': 'recruit',
            'file': os.path.join(DATA_DIR, 'recruitment_v3.pkl'),
            'converter': to_recruit_row,
            'builder': build_recruit_chunk,
        },
        {
            'domain': 'candidate',
            'file': os.path.join(DATA_DIR, 'candidate_v2.pkl'),
            'converter': to_candidate_row,
            'builder': build_candidate_chunk,
        },
        {
            'domain': 'skill_dic',
            'file': os.path.join(DATA_DIR, 'skill_embeddings_dict_v2.pkl'),
            'converter': to_skill_dic_row,
            'builder': build_skill_dic_chunk,
        },
    ]

    results = []
    for d in domains:
        if not os.path.exists(d['file']):
            print(f"\n[SKIP] 파일 없음: {d['file']}")
            continue
        result = measure_domain(d['domain'], d['file'], d['converter'], d['builder'])
        results.append(result)

    # ─── 최종 요약 출력 ───
    print(f"\n\n{'='*70}")
    print("  최종 실측 결과 요약 (포트폴리오 기재용)")
    print(f"{'='*70}")
    print(f"{'도메인':<12} {'행 수':>8} {'청크':>6} "
          f"{'Protobuf':>12} {'JSON':>12} {'절감률':>8} {'절감량':>12}")
    print(f"{'-'*70}")

    grand_proto = 0
    grand_json = 0
    grand_rows = 0

    for r in results:
        reduction = 100 * (1 - r['total_proto_bytes'] / r['total_json_bytes'])
        saved = r['total_json_bytes'] - r['total_proto_bytes']
        print(f"{r['domain']:<12} {r['total_rows']:>8,} {r['chunks']:>6} "
              f"{format_mb(r['total_proto_bytes']):>12} "
              f"{format_mb(r['total_json_bytes']):>12} "
              f"{reduction:>7.1f}% "
              f"{format_mb(saved):>12}")
        grand_proto += r['total_proto_bytes']
        grand_json += r['total_json_bytes']
        grand_rows += r['total_rows']

    print(f"{'-'*70}")
    grand_reduction = 100 * (1 - grand_proto / grand_json) if grand_json > 0 else 0
    grand_saved = grand_json - grand_proto
    print(f"{'합계':<12} {grand_rows:>8,} {'':>6} "
          f"{format_gb(grand_proto):>12} "
          f"{format_gb(grand_json):>12} "
          f"{grand_reduction:>7.1f}% "
          f"{format_gb(grand_saved):>12}")
    print(f"{'='*70}")

    print("\n[포트폴리오 수치 (복사용)]")
    for r in results:
        reduction = 100 * (1 - r['total_proto_bytes'] / r['total_json_bytes'])
        print(f"  {r['domain']:12}: {format_mb(r['total_json_bytes'])} → "
              f"{format_mb(r['total_proto_bytes'])}  ({reduction:.1f}% 절감, "
              f"{r['total_rows']:,}건)")
    print(f"  {'전체합계':12}: {format_gb(grand_json)} → "
          f"{format_gb(grand_proto)}  ({grand_reduction:.1f}% 절감)")


if __name__ == '__main__':
    main()
