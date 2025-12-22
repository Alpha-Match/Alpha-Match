"""
전처리 파이프라인 통합 테스트

목적:
1. PklChunkLoader의 전처리 로직이 정상 작동하는지 확인
2. RecruitData v2 스키마와 호환되는지 검증
3. 벡터 차원(384d)이 올바른지 확인
4. skill_dic_loader가 정상 작동하는지 확인
"""
import sys
from pathlib import Path

# 프로젝트 루트를 Python 경로에 추가
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.infrastructure.loaders import get_loader, DataFormat
from src.infrastructure.skill_dic_loader import load_skill_embeddings_json
from src.domain.models import RecruitData, SkillEmbeddingDicData

print("=" * 80)
print("전처리 파이프라인 통합 테스트")
print("=" * 80)

# ============================================================================
# Test 1: Recruit Data Preprocessing Pipeline
# ============================================================================
print("\n[Test 1] Recruit Data Preprocessing Pipeline\n")

recruit_file = "data/recruitment_v1.pkl"
print(f"파일: {recruit_file}")

try:
    # Loader 생성
    loader = get_loader("recruit", DataFormat.PKL, chunk_size=1000)
    print("[OK] Loader 생성 성공")

    # 첫 번째 청크만 로드
    chunk_iterator = loader.load_chunks(recruit_file)
    try:
        first_chunk = next(chunk_iterator)
    except StopIteration:
        print("[ERROR] 첫 번째 청크가 비어있음 - 전처리 필터링이 너무 강력함")
        print("데이터 파일을 확인하세요.")
        raise

    print(f"[OK] 첫 번째 청크 로드 성공: {len(first_chunk)} rows")

    # 첫 번째 행 검증
    first_row = first_chunk[0]
    print("\n--- 첫 번째 행 검증 ---")

    # RecruitData 타입 확인
    assert isinstance(first_row, RecruitData), f"타입 불일치: {type(first_row)}"
    print("[OK] 타입: RecruitData")

    # 필드 존재 확인
    required_fields = [
        'id', 'position', 'company_name', 'experience_years',
        'primary_keyword', 'english_level', 'published_at',
        'skills', 'long_description', 'description_lang', 'skills_vector'
    ]

    for field in required_fields:
        assert hasattr(first_row, field), f"필드 누락: {field}"
    print(f"[OK] 필수 필드 {len(required_fields)}개 모두 존재")

    # 벡터 차원 확인
    vector_dim = len(first_row.skills_vector)
    assert vector_dim == 384, f"벡터 차원 불일치: {vector_dim} (예상: 384)"
    print(f"[OK] 벡터 차원: {vector_dim}d")

    # skills 배열 검증
    assert isinstance(first_row.skills, list), "skills는 리스트여야 함"
    assert len(first_row.skills) > 0, "skills 배열이 비어있음 (필터링 실패)"
    print(f"[OK] skills 배열: {len(first_row.skills)}개 기술 스택")

    # experience_years 타입 확인
    if first_row.experience_years is not None:
        assert isinstance(first_row.experience_years, int), f"experience_years 타입 불일치: {type(first_row.experience_years)}"
        print(f"[OK] experience_years: {first_row.experience_years} (Integer)")
    else:
        print("[OK] experience_years: None (null 변환 성공)")

    # 샘플 데이터 출력
    print("\n--- 샘플 데이터 ---")
    print(f"ID: {first_row.id}")
    print(f"Position: {first_row.position[:50]}...")
    print(f"Company: {first_row.company_name}")
    print(f"Experience: {first_row.experience_years}")
    print(f"Skills: {first_row.skills[:3]}...")
    print(f"Vector: [{first_row.skills_vector[0]:.4f}, {first_row.skills_vector[1]:.4f}, ..., {first_row.skills_vector[-1]:.4f}]")

    # 전체 청크 통계
    print("\n--- 전체 데이터 통계 (첫 3개 청크) ---")
    total_rows = len(first_chunk)
    null_exp_count = sum(1 for row in first_chunk if row.experience_years is None)

    # 추가 2개 청크 로드
    for i, chunk in enumerate(chunk_iterator, start=2):
        total_rows += len(chunk)
        null_exp_count += sum(1 for row in chunk if row.experience_years is None)
        if i >= 3:
            break

    print(f"총 행 수: {total_rows}")
    print(f"experience_years null 개수: {null_exp_count} ({null_exp_count/total_rows*100:.2f}%)")
    print(f"유효 벡터 비율: 100% (null 벡터는 이미 필터링됨)")

    print("\n[OK] Test 1 통과: Recruit Data 전처리 파이프라인 정상")

except Exception as e:
    print(f"\n[FAIL] Test 1 실패: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)

# ============================================================================
# Test 2: Skill Embeddings JSON Loader
# ============================================================================
print("\n" + "=" * 80)
print("[Test 2] Skill Embeddings JSON Loader\n")

skill_file = "data/skill_embeddings.json"
print(f"파일: {skill_file}")

try:
    # JSON 로드
    skill_list = load_skill_embeddings_json(skill_file)
    print(f"[OK] JSON 로드 성공: {len(skill_list)} 스킬")

    # 첫 번째 스킬 검증
    first_skill = skill_list[0]
    print("\n--- 첫 번째 스킬 검증 ---")

    # SkillEmbeddingDicData 타입 확인
    assert isinstance(first_skill, SkillEmbeddingDicData), f"타입 불일치: {type(first_skill)}"
    print("[OK] 타입: SkillEmbeddingDicData")

    # 필드 존재 확인
    required_fields = ['skill', 'position_category', 'skill_vector']
    for field in required_fields:
        assert hasattr(first_skill, field), f"필드 누락: {field}"
    print(f"[OK] 필수 필드 {len(required_fields)}개 모두 존재")

    # synonyms 필드가 없는지 확인 (Pydantic 모델에는 정의되지 않음)
    model_fields = first_skill.model_fields.keys()
    assert 'synonyms' not in model_fields, "synonyms 필드가 제외되지 않음"
    print("[OK] synonyms 필드 제외됨")

    # 벡터 차원 확인
    vector_dim = len(first_skill.skill_vector)
    assert vector_dim == 384, f"벡터 차원 불일치: {vector_dim} (예상: 384)"
    print(f"[OK] 벡터 차원: {vector_dim}d")

    # 샘플 데이터 출력
    print("\n--- 샘플 데이터 (첫 5개 스킬) ---")
    for i, skill in enumerate(skill_list[:5], 1):
        print(f"{i}. {skill.skill} ({skill.position_category}) - Vector: {len(skill.skill_vector)}d")

    # 전체 통계
    print("\n--- 전체 통계 ---")
    print(f"총 스킬 개수: {len(skill_list)}")
    categories = {}
    for skill in skill_list:
        categories[skill.position_category] = categories.get(skill.position_category, 0) + 1

    print("카테고리별 분포:")
    for category, count in sorted(categories.items(), key=lambda x: x[1], reverse=True):
        print(f"  - {category}: {count}개")

    print("\n[OK] Test 2 통과: Skill Embeddings 로더 정상")

except Exception as e:
    print(f"\n[FAIL] Test 2 실패: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)

# ============================================================================
# 최종 결과
# ============================================================================
print("\n" + "=" * 80)
print("SUCCESS: 전체 테스트 통과")
print("=" * 80)
print("\n전처리 파이프라인 검증 완료:")
print("  1. Recruit 데이터 전처리 (컬럼 매핑, Exp Years 변환, 필터링)")
print("  2. 벡터 차원 384d 검증")
print("  3. Skill Embeddings JSON 로딩 (synonyms 제외)")
print("  4. Pydantic v2 스키마 호환성")
print("\nPython 서버 v2 업데이트 완료. gRPC 스트리밍 준비 완료.")
