"""
skill_embeddings.json을 pkl 형식으로 변환하는 스크립트

table_specification.md 요구사항:
- name → skill
- category → position_category
- vector → skill_vector
- synonyms 필드 제외
"""
import json
import pandas as pd
from pathlib import Path

def convert_skill_json_to_pkl():
    # 파일 경로
    json_path = Path(__file__).parent / 'data' / 'skill_embeddings.json'
    pkl_path = Path(__file__).parent / 'data' / 'skill_embeddings_from_json.pkl'

    print(f"Loading JSON file: {json_path}")
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    print(f"Total skills loaded: {len(data)}")

    # 필드명 매핑 및 synonyms 제외
    processed_data = []
    for item in data:
        processed_data.append({
            'skill': item['name'],
            'position_category': item['category'],
            'skill_vector': item['vector']
            # synonyms는 의도적으로 제외
        })

    # DataFrame 생성
    df = pd.DataFrame(processed_data)

    print(f"DataFrame shape: {df.shape}")
    print(f"Columns: {df.columns.tolist()}")
    print(f"Vector dimension: {len(df['skill_vector'].iloc[0])}")

    # pkl로 저장
    df.to_pickle(pkl_path)
    print(f"Saved to: {pkl_path}")
    print(f"File size: {pkl_path.stat().st_size / 1024:.2f} KB")

if __name__ == '__main__':
    convert_skill_json_to_pkl()
