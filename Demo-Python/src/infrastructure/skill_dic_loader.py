"""
Skill Embedding Dictionary Loader

skill_embeddings.json 전용 로더
table_specification.md 요구사항: synonyms 필드 제외
"""
import json
import logging
from typing import List
from ..domain.models import SkillEmbeddingDicData

logger = logging.getLogger(__name__)


def load_skill_embeddings_json(file_path: str) -> List[SkillEmbeddingDicData]:
    """
    skill_embeddings.json 전용 로더

    전처리 요구사항 (table_specification.md):
    - synonyms 필드 제외
    - name → skill
    - category → position_category
    - vector → skill_vector

    Args:
        file_path: JSON 파일 경로

    Returns:
        List[SkillEmbeddingDicData]: 변환된 스킬 임베딩 리스트
    """
    try:
        logger.info(f"Loading skill embeddings JSON: {file_path}")

        with open(file_path, 'r', encoding='utf-8') as f:
            raw_data = json.load(f)

        logger.info(f"Total skills loaded: {len(raw_data)}")

        # 필드명 매핑 및 synonyms 제외
        skill_list = [
            SkillEmbeddingDicData(
                skill=item['name'],
                position_category=item['category'],
                skill_vector=item['vector']
                # synonyms 필드는 의도적으로 제외 (table_specification.md 요구사항)
            )
            for item in raw_data
        ]

        logger.info(f"Skill embeddings loaded successfully: {len(skill_list)} skills")
        return skill_list

    except FileNotFoundError:
        logger.error(f"File not found: {file_path}")
        raise FileNotFoundError(f"Skill embeddings 파일을 찾을 수 없습니다: {file_path}")
    except KeyError as e:
        logger.error(f"Missing required field in JSON: {e}")
        raise ValueError(f"JSON 필드 누락: {e}")
    except Exception as e:
        logger.error(f"Error loading skill embeddings: {e}")
        raise IOError(f"Skill embeddings 로딩 중 오류 발생: {e}")
