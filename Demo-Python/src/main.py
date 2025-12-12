"""
애플리케이션 진입점 (Application Entrypoint)

- FastAPI 애플리케이션을 생성하고 설정합니다.
- 각 모듈(api, grpc 등)의 라우터를 등록합니다.
- uvicorn을 통해 이 파일을 실행하여 웹 서버를 시작합니다.
"""

from fastapi import FastAPI
import logging

# 프로젝트 내 다른 모듈 임포트
from api import endpoints as ingestion_api

# --- 로깅 기본 설정 ---
# 애플리케이션 시작 시 한 번만 설정합니다.
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s [%(name)s] - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)


# --- FastAPI 앱 생성 ---
app = FastAPI(
    title="Alpha-Match: Demo-Python Server",
    description="""
    gRPC 서버 및 FastAPI 엔드포인트를 통해 데이터 스트리밍을 관리하는
    데모 파이썬 서버입니다.
    """,
    version="1.0.0"
)


# --- API 라우터 등록 ---
# /data 경로로 들어오는 요청을 ingestion_api.router가 처리하도록 등록합니다.
app.include_router(ingestion_api.router)


# --- 기본 엔드포인트 ---
@app.get("/", tags=["Health Check"])
async def read_root():
    """
    서버의 상태를 확인하는 기본 엔드포인트입니다.
    """
    logger.info("Health check endpoint '/' was called.")
    return {"status": "ok", "message": "Welcome to the Demo-Python Server!"}

# --- 서버 실행 ---
# 이 파일이 직접 실행될 때 (예: `python src/main.py`),
# uvicorn을 사용하여 서버를 실행할 수 있습니다.
# 하지만 보통은 CLI에서 `uvicorn src.main:app --reload` 명령어로 실행합니다.
if __name__ == "__main__":
    import uvicorn
    logger.info("애플리케이션을 uvicorn을 통해 직접 실행합니다 (개발용).")
    uvicorn.run(app, host="0.0.0.0", port=8000)
