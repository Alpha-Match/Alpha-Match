"""
skills 컬럼 타입 확인
"""
import pandas as pd

pkl_data = pd.read_pickle('data/recruitment_v1.pkl')
df = pkl_data['data_frame']

print("=" * 80)
print("skills 컬럼 타입 분석")
print("=" * 80)

# 첫 10개 샘플
print("\n첫 10개 샘플:")
for i, skills_val in enumerate(df['skills'].head(10), 1):
    print(f"{i}. Type: {type(skills_val).__name__}, Value: {skills_val}")

# 타입별 통계
print("\n타입별 통계:")
type_counts = df['skills'].apply(lambda x: type(x).__name__).value_counts()
print(type_counts)

# None 또는 NaN 개수
print(f"\nNone 개수: {df['skills'].isna().sum()}")
print(f"빈 문자열 개수: {(df['skills'] == '').sum()}")

# 실제 리스트인지 확인
is_list = df['skills'].apply(lambda x: isinstance(x, list))
print(f"\nlist 타입 개수: {is_list.sum()} / {len(df)}")

# 빈 리스트 확인
if is_list.any():
    empty_lists = df[is_list]['skills'].apply(lambda x: len(x) == 0)
    print(f"빈 리스트 개수: {empty_lists.sum()}")
