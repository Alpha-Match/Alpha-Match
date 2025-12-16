"""
Demo-Python gRPC 서버를 위한 설정 모듈입니다.

다음 항목들에 대한 모든 설정 변수를 중앙에서 관리합니다:
- gRPC 서버 설정
- 데이터 로딩 및 스트리밍 설정
- 파일 경로 및 성능 튜닝
"""

from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict


@dataclass
class ServerConfig:
    """gRPC 및 FastAPI 서버 설정"""
    # FastAPI 서버가 리슨할 주소
    HOST: str = '0.0.0.0'
    PORT: int = 8000

    # gRPC 클라이언트가 접속할 배치 서버 주소
    BATCH_SERVER_ADDRESS: str = 'localhost:50051'

    # (구) gRPC 서버 설정
    GRPC_HOST: str = '[::]:50051'  # 모든 인터페이스의 50051 포트에서 리슨
    MAX_WORKERS: int = 10      # ThreadPoolExecutor의 최대 워커 수
    MAX_MESSAGE_LENGTH: int = 100 * 1024 * 1024  # 최대 메시지 크기: 100MB


@dataclass
class DomainConfig:
    """도메인별 개별 설정"""
    vector_dimension: int


@dataclass
class DataConfig:
    """데이터 로딩 및 처리 설정"""
    # 파일 경로
    BASE_DIR: Path = Path(__file__).parent.parent.parent # Demo-Python 루트 경로
    DATA_DIR: Path = BASE_DIR / 'data'
    
    # 스트리밍 설정
    DEFAULT_CHUNK_SIZE: int = 300  # 청크당 기본 행 수
    MIN_CHUNK_SIZE: int = 100      # 최소 청크 크기
    MAX_CHUNK_SIZE: int = 1000     # 최대 청크 크기

    # 데이터 최적화
    OPTIMIZE_DTYPES: bool = True   # 데이터 타입 최적화 활성화
    USE_CATEGORY: bool = True      # 문자열에 category 타입 사용

    # 로깅
    LOG_CHUNK_INTERVAL: int = 10   # N개 청크마다 로그 기록

    # 도메인별 설정을 관리하는 딕셔너리
    DOMAIN_CONFIGS: Dict[str, DomainConfig] = field(default_factory=lambda: {
        "recruit": DomainConfig(vector_dimension=384),
        "candidate": DomainConfig(vector_dimension=768), # 예시: 후보자 도메인은 768차원
    })


@dataclass
class PerformanceConfig:
    """성능 튜닝 설정"""
    # 메모리 관리
    CHUNK_READ_SIZE: int = 1000    # DataFrame에서 한 번에 읽을 행 수

    # 재시도 설정
    MAX_RETRY_ATTEMPTS: int = 3
    RETRY_DELAY_SECONDS: float = 1.0

    # 타임아웃 설정
    REQUEST_TIMEOUT_SECONDS: int = 300  # 5분


# 전역 설정 인스턴스
server_config = ServerConfig()
data_config = DataConfig()
performance_config = PerformanceConfig()


def validate_config() -> bool:
    """
    설정 값들을 검증합니다. (파일 경로 등)

    Returns:
        bool: 모든 설정이 유효하면 True를 반환합니다.
    """
    # 데이터 디렉토리 존재 여부 확인
    if not data_config.DATA_DIR.exists():
        raise FileNotFoundError(
            f"데이터 디렉토리를 찾을 수 없습니다: {data_config.DATA_DIR}\n"
            f"예상 경로: {data_config.DATA_DIR.absolute()}"
        )

    # 청크 크기 검증
    if not (data_config.MIN_CHUNK_SIZE <= data_config.DEFAULT_CHUNK_SIZE <= data_config.MAX_CHUNK_SIZE):
        raise ValueError(
            f"잘못된 청크 크기 설정입니다: "
            f"최소({data_config.MIN_CHUNK_SIZE}) <= 기본({data_config.DEFAULT_CHUNK_SIZE}) <= 최대({data_config.MAX_CHUNK_SIZE})"
        )

    print(f"[Config] 설정 검증 완료")
    print(f"[Config] 데이터 디렉토리: {data_config.DATA_DIR}")
    print(f"[Config] 서버 주소: {server_config.HOST}:{server_config.PORT}")
    print(f"[Config] 기본 청크 크기: {data_config.DEFAULT_CHUNK_SIZE}")

    return True


if __name__ == '__main__':
    # 설정 테스트
    try:
        validate_config()
        print("\n모든 설정이 유효합니다!")
    except Exception as e:
        print(f"설정 오류: {e}")
        raise
