# gRPC Client Quick Start Guide

**빠른 시작:** Python 서버와 gRPC 통신 테스트

---

## 🚀 1분 안에 시작하기

### Step 1: Python 서버 실행
```bash
cd Demo-Python
python src/grpc_server.py
```

### Step 2: Batch 서버 실행
```bash
cd Backend/Batch-Server
./gradlew bootRun
```

### Step 3: 로그 확인
터미널에서 다음과 같은 로그가 출력되면 성공:
```
================================================================================
Starting gRPC Connection and Streaming Test
================================================================================

[STEP 1] Testing gRPC Connection...
Connection successful! Received X rows

[STEP 2] Testing Full Streaming...
Chunk #1: Received 300 rows
Sample Row - ID: xxx
Sample Row - Company: xxx
...

================================================================================
All gRPC Tests Completed Successfully!
Total Chunks Received: X
Total Rows Received: XXXX
================================================================================
```

---

## ⚙️ 설정

### 테스트 활성화/비활성화

**활성화 (개발 모드):**
```yaml
# application.yml
grpc:
  test:
    enabled: true  # 애플리케이션 시작 시 자동 테스트
```

**비활성화 (운영 모드):**
```yaml
grpc:
  test:
    enabled: false  # 테스트 건너뛰기
```

### Chunk 크기 조정
```yaml
batch:
  embedding:
    chunk-size: 300  # 한 번에 받을 row 수 (기본값: 300)
```

---

## 🔍 주요 로그 설명

| 로그 | 의미 |
|-----|------|
| `Creating gRPC channel for Python Embedding Server: localhost:50051` | 채널 생성 성공 |
| `Starting embedding stream - chunkSize: 300` | 스트리밍 시작 |
| `Received chunk #X with Y rows` | Chunk 수신 성공 |
| `Embedding stream completed. Total chunks received: X` | 전체 스트리밍 완료 |
| `Connection successful! Received X rows` | 연결 테스트 성공 |

---

## ⚠️ 문제 해결

### Python 서버 연결 실패
```
UNAVAILABLE: io exception
Python gRPC Server is not available!
```

**원인:**
- Python 서버가 실행되지 않음
- 포트 50051이 사용 중

**해결:**
1. Python 서버 실행 확인: `python src/grpc_server.py`
2. 포트 확인: `netstat -ano | findstr :50051` (Windows)
3. 방화벽 확인

### Proto 파일 생성 실패
```
package com.alpha.backend.grpc.proto does not exist
```

**해결:**
```bash
./gradlew clean generateProto
./gradlew compileJava
```

### Chunk 크기 조정 필요
Vector 데이터가 크면 chunk 크기를 줄여보세요:
```yaml
batch:
  embedding:
    chunk-size: 100  # 300 → 100
```

---

## 📁 관련 파일

| 파일 | 역할 |
|-----|------|
| `EmbeddingGrpcClient.java` | gRPC 클라이언트 (스트림 수신) |
| `GrpcStreamTestService.java` | 테스트 서비스 |
| `GrpcTestRunner.java` | 자동 실행 Runner |
| `embedding_stream.proto` | gRPC 프로토콜 정의 |
| `application.yml` | 설정 파일 |

---

## 📚 상세 문서

더 자세한 내용은 다음 문서를 참고하세요:
- **gRPC 클라이언트 구현 가이드**: `/docs/gRPC_클라이언트_구현.md`
- **gRPC 통신 가이드**: `/docs/gRPC_통신_가이드.md`
- **Batch 설계서**: `/docs/Batch설계서.md`

---

**작성일:** 2025-12-11
