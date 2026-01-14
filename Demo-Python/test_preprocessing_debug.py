"""
전처리 디버깅 스크립트 - 각 단계별로 행 수 확인
"""
import pandas as pd

print("=" * 80)
print("전처리 디버깅 - 단계별 행 수 추적")
print("=" * 80)

# 1. 파일 로드
print("\n[1] 파일 로드")
pkl_data = pd.read_pickle('data/recruitment_v1.pkl')

if isinstance(pkl_data, dict) and 'data_frame' in pkl_data:
    df = pkl_data['data_frame']
else:
    df = pkl_data

print(f"초기 행 수: {len(df):,}")
print(f"초기 컬럼: {list(df.columns)}")

# 2. 컬럼명 매핑
print("\n[2] 컬럼명 매핑")
column_mapping = {
    'Position': 'position',
    'Company Name': 'company_name',
    'Exp Years': 'experience_years',
    'Primary Keyword': 'primary_keyword',
    'English Level': 'english_level',
    'Published': 'published_at',
    'Long Description': 'long_description',
    'Long Description_lang': 'description_lang',
    'skill_vector': 'skills_vector',
    'skills': 'skills',
    'id': 'id'
}
df = df.rename(columns=column_mapping)
print(f"매핑 후 행 수: {len(df):,}")
print(f"매핑 후 컬럼: {list(df.columns)}")

# 3. Exp Years 전처리
print("\n[3] Exp Years 전처리")
def convert_exp_years(value):
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
print(f"변환 후 행 수: {len(df):,}")

# 4. 불필요한 컬럼 삭제
print("\n[4] 불필요한 컬럼 삭제")
unnecessary_cols = ['__index_level_0__', 'normalized_skills', 'embedding_input_text', 'embedding_sample']
existing_unnecessary = [col for col in unnecessary_cols if col in df.columns]
if existing_unnecessary:
    df = df.drop(columns=existing_unnecessary)
    print(f"삭제된 컬럼: {', '.join(existing_unnecessary)}")
else:
    print("삭제할 컬럼 없음")
print(f"삭제 후 행 수: {len(df):,}")

# 5. skills 빈 배열 제외
print("\n[5] skills 빈 배열 필터링")
before = len(df)
df = df[df['skills'].apply(lambda x: isinstance(x, list) and len(x) > 0)]
after = len(df)
print(f"필터링 전: {before:,}")
print(f"필터링 후: {after:,}")
print(f"제거된 행: {before - after:,}")

# 6. 벡터 누락 행 필터링
print("\n[6] 벡터 누락 필터링")
before = len(df)
print(f"skills_vector 컬럼 존재: {'skills_vector' in df.columns}")
if 'skills_vector' in df.columns:
    print(f"skills_vector null 개수: {df['skills_vector'].isna().sum():,}")
    df = df[df['skills_vector'].notna()]
    after = len(df)
    print(f"필터링 전: {before:,}")
    print(f"필터링 후: {after:,}")
    print(f"제거된 행: {before - after:,}")
else:
    print("[ERROR] skills_vector 컬럼이 없음!")

# 7. 필요한 컬럼만 선택
print("\n[7] 필요한 컬럼만 선택")
required_cols = [
    'id', 'position', 'company_name', 'experience_years',
    'primary_keyword', 'english_level', 'published_at',
    'skills', 'long_description', 'description_lang', 'skills_vector'
]
print(f"필요한 컬럼: {required_cols}")
print(f"현재 컬럼: {list(df.columns)}")

missing_cols = [col for col in required_cols if col not in df.columns]
if missing_cols:
    print(f"[ERROR] 누락된 컬럼: {missing_cols}")
else:
    df = df[required_cols]
    print(f"최종 행 수: {len(df):,}")
    print(f"최종 컬럼: {list(df.columns)}")

print("\n" + "=" * 80)
print("디버깅 완료")
print("=" * 80)
