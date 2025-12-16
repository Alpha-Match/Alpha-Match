"""
PostgreSQL 호환성을 위한 UUID v7 생성기.

UUID v7의 장점:
- 시간 순 정렬 (데이터베이스 인덱싱에 유리)
- PostgreSQL의 UUID 타입과 호환
- 분산 시스템 환경에서 고유성 보장

참고: 현재 .pkl 파일은 내용 기반의 UUID v5를 이미 포함하고 있으므로,
이 모듈은 주로 문서화 및 향후 새로운 임베딩 생성 시 사용을 위해 존재합니다.
"""

import uuid
from typing import List


def generate_uuid_v7() -> str:
    """
    UUID v7 (시간 순 정렬 UUID)을 생성합니다.

    UUID v7 형식:
    - 48 비트: 밀리초 단위의 유닉스 타임스탬프
    - 12 비트: 랜덤 데이터
    - 2 비트: 버전 (0b111)
    - 62 비트: 랜덤 데이터

    Returns:
        str: UUID v7 문자열 (예: "01896e3f-5a5e-7b4c-9f3e-d1234567890a")

    참고:
        Python 3.11 기준, `uuid` 모듈은 아직 UUID v7을 정식 지원하지 않습니다.
        프로덕션 환경에서는 'uuid6' 패키지 사용을 고려해볼 수 있습니다.
        이 데모에서는 원본 데이터로부터 결정론적으로 생성된 UUID v5를 사용합니다.
    """
    # 임시 방편: 현재는 UUID v4 (랜덤)를 사용합니다.
    # 프로덕션에서는: from uuid6 import uuid7; return str(uuid7())
    return str(uuid.uuid4())


def generate_batch_uuids(count: int) -> List[str]:
    """
    배치 작업을 위해 여러 개의 UUID를 생성합니다.

    Args:
        count: 생성할 UUID의 개수

    Returns:
        List[str]: UUID 문자열의 리스트
    """
    return [generate_uuid_v7() for _ in range(count)]


def is_valid_uuid(uuid_string: str) -> bool:
    """
    UUID 문자열 형식이 유효한지 검증합니다.

    Args:
        uuid_string: 검증할 UUID 문자열

    Returns:
        bool: UUID 형식이 유효하면 True
    """
    try:
        uuid.UUID(uuid_string)
        return True
    except (ValueError, AttributeError, TypeError):
        return False


def generate_deterministic_uuid(data: str) -> str:
    """
    내용 기반으로 결정론적인 UUID v5를 생성합니다.

    이 방식은 다음과 같은 경우에 유용합니다:
    - 동일한 내용이 항상 동일한 UUID를 갖도록 보장
    - 중복 방지
    - 재현 가능한 ID 생성

    Args:
        data: 해싱할 문자열 데이터 (예: 회사명 + 직무 + 날짜)

    Returns:
        str: UUID v5 문자열

    예시:
        >>> generate_deterministic_uuid("Company_A_Backend_Engineer_2024-01-01")
        'c0ca96e7-85df-50df-a64e-d934cd02a170'
    """
    # 결정론적 생성을 위해 DNS 네임스페이스를 사용합니다.
    namespace = uuid.NAMESPACE_DNS
    return str(uuid.uuid5(namespace, data))


if __name__ == '__main__':
    # UUID 생성 테스트
    print("UUID v7 (v4로 대체):")
    for i in range(5):
        print(f"  {i+1}. {generate_uuid_v7()}")

    print("\n결정론적 UUID v5:")
    data = "MyCointainer_Sysadmin_2024-01-01"
    for i in range(3):
        # 동일한 데이터는 항상 동일한 UUID를 생성해야 합니다.
        print(f"  {i+1}. {generate_deterministic_uuid(data)}")

    print("\n형식 검증:")
    valid_uuid = "c0ca96e7-85df-50df-a64e-d934cd02a170"
    invalid_uuid = "not-a-uuid"
    print(f"  '{valid_uuid}'는 유효한 형식인가? {is_valid_uuid(valid_uuid)}")
    print(f"  '{invalid_uuid}'는 유효한 형식인가? {is_valid_uuid(invalid_uuid)}")
