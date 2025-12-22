"""
전처리 요구사항 검증 스크립트
"""
import pandas as pd
import json
import numpy as np

print("=" * 80)
print("전처리 요구사항 검증")
print("=" * 80)

# 1. Recruit 데이터 검증
print("\n[1] Recruit 데이터 (recruitment_v1.pkl) 검증\n")

pkl_data = pd.read_pickle('data/recruitment_v1.pkl')
df = pkl_data['data_frame']

print(f"총 행 수: {len(df):,}")

# 1-1. skills 빈 배열 확인
print("\n--- [1-1] skills 빈 배열 확인 ---")
empty_skills = df[df['skills'].apply(lambda x: isinstance(x, list) and len(x) == 0)]
print(f"빈 배열 개수: {len(empty_skills):,} ({len(empty_skills)/len(df)*100:.2f}%)")
if len(empty_skills) > 0:
    print("샘플:")
    print(empty_skills[['id', 'skills', 'Position']].head(1))

# 1-2. Exp Years 값 분석
print("\n--- [1-2] Exp Years 값 분석 ---")
exp_years_col = df['Exp Years']
print(f"총 행 수: {len(exp_years_col):,}")
print(f"Null 개수: {exp_years_col.isna().sum():,}")

# no_exp 확인
no_exp_count = (exp_years_col == 'no_exp').sum()
print(f"'no_exp' 값 개수: {no_exp_count:,}")

# "2y" 형식 확인 (문자열이면서 'y'로 끝나는 것)
year_format = exp_years_col[exp_years_col.astype(str).str.endswith('y', na=False)]
print(f"'Ny' 형식 개수: {len(year_format):,} ({len(year_format)/len(df)*100:.2f}%)")

# 고유값 상위 10개
print("\n고유값 상위 10개:")
print(exp_years_col.value_counts().head(10))

print("\n다양한 형식 샘플 (5개):")
sample_values = exp_years_col.dropna().sample(min(5, len(exp_years_col.dropna())))
for idx, val in enumerate(sample_values, 1):
    print(f"  {idx}. {repr(val)} (type: {type(val).__name__})")

# 1-3. 불필요한 컬럼 확인
print("\n--- [1-3] 불필요한 컬럼 확인 ---")
unnecessary_cols = ['__index_level_0__', 'normalized_skills', 'embedding_input_text', 'embedding_sample']
for col in unnecessary_cols:
    if col in df.columns:
        sample = str(df[col].iloc[0])[:50]  # 첫 50자만
        print(f"[O] {col}: 존재 (샘플: {sample})")
    else:
        print(f"[X] {col}: 존재하지 않음")

# 2. Skill Embeddings 데이터 검증
print("\n" + "=" * 80)
print("[2] Skill Embeddings (skill_embeddings.json) 검증")
print("=" * 80 + "\n")

with open('data/skill_embeddings.json', 'r', encoding='utf-8') as f:
    skills_data = json.load(f)

print(f"총 스킬 개수: {len(skills_data)}")

# 2-1. synonyms 필드 확인
print("\n--- [2-1] synonyms 필드 확인 ---")
has_synonyms = sum(1 for skill in skills_data if 'synonyms' in skill)
print(f"synonyms 필드 존재: {has_synonyms}/{len(skills_data)}")

print("\nsynonyms 샘플 (5개):")
for i, skill in enumerate(skills_data[:5], 1):
    synonyms = skill.get('synonyms', [])
    print(f"  {i}. {skill['name']}: {synonyms}")

# 3. 전처리 로직 제안
print("\n" + "=" * 80)
print("[3] 전처리 로직 요약")
print("=" * 80 + "\n")

print("1. skills 빈 배열 제외:")
print(f"   - 필터링 전: {len(df):,} 행")
print(f"   - 필터링 후: {len(df) - len(empty_skills):,} 행")
print(f"   - 제거될 행: {len(empty_skills):,} ({len(empty_skills)/len(df)*100:.2f}%)")

print("\n2. Exp Years 전처리:")
print(f"   - 'no_exp' → null 변환: {no_exp_count:,} 행")
print(f"   - 'Ny' → Integer 변환: {len(year_format):,} 행")

print("\n3. 컬럼 삭제:")
existing_unnecessary = [col for col in unnecessary_cols if col in df.columns]
print(f"   - 삭제할 컬럼: {', '.join(existing_unnecessary)}")

print("\n4. synonyms 필드 제외:")
print(f"   - JSON 로드 시 제외 (106개 항목 모두 적용)")

print("\n" + "=" * 80)
print("검증 완료")
print("=" * 80)
