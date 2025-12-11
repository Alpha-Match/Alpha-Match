# gRPC 통합 테스트 가이드

## 📋 구현 완료 사항

### ✅ Batch-Server (Java)
- **위치**: `Backend/Batch-Server/`
- **gRPC Client**: `EmbeddingGrpcClient.java`
- **테스트 서비스**: `GrpcStreamTestService.java`
- **자동 테스트**: `GrpcTestRunner.java`
- **포트**: Python 서버(50051)에 연결

### ✅ Demo-Python (Python)
- **위치**: `Demo-Python/`
- **gRPC Server**: `src/grpc_server.py`
- **테스트 Client**: `src/grpc_client.py`
- **데이터**: `processed_recruitment_data.pkl` (141,897 rows)
- **포트**: 50051에서 listening

---

## 🚀 통합 테스트 실행

### 단계 1: Python 서버 시작

**터미널 1** (Demo-Python)
```bash
cd C:\Final_2025-12-09\Alpha-Match\Demo-Python
python src/grpc_server.py
```

**예상 출력**:
```
============================================================
          Demo Python gRPC Embedding Server
============================================================
Configuration:
  Server Host: [::]:50051
  Max Workers: 10
  Max Message Size: 100 MB
  Data File: processed_recruitment_data.pkl
  Default Chunk Size: 300 rows
============================================================
Loading data from: processed_recruitment_data.pkl
Data loaded successfully: 141,897 rows
Memory optimized - dtype conversions saved 5.3%
============================================================
gRPC Server Started Successfully
Listening on: [::]:50051
Press Ctrl+C to stop
============================================================
```

---

### 단계 2: Batch-Server 시작

**터미널 2** (Batch-Server)
```bash
cd C:\Final_2025-12-09\Alpha-Match\Backend\Batch-Server
./gradlew bootRun
```

또는 Windows에서:
```bash
gradlew.bat bootRun
```

**예상 출력**:
```
================================================================================
Starting gRPC Connection and Streaming Test
================================================================================
[STEP 1] Testing gRPC Connection...
[Connection Test] Sending request with chunk_size: 100
[Connection Test] First chunk received: 100 rows
Connection successful! Received 100 rows
Sample Row - UUID: c0ca96e7-85df-50df-a64e-d934cd02a170
Sample Row - Company: MyCointainer
Sample Row - Vector Dimension: 384

[STEP 2] Testing Full Streaming...
[Full Stream] Sending request with chunk_size: 300
Chunk #1: Received 300 rows
  Sample Row - ID: c0ca96e7-85df-50df-a64e-d934cd02a170
  Sample Row - Company: MyCointainer
  Sample Row - Vector Dimension: 384
Chunk #2: Received 300 rows
...
Total chunks received: 474
Total rows received: 141,897

[STEP 3] Testing Checkpoint Resumption...
[Checkpoint Test] Resuming from UUID: c0ca96e7-85df-50df-a64e-d934cd02a170
Chunk #1: Received 300 rows (starting from checkpoint)
...

All gRPC Tests Completed Successfully!
================================================================================
```

---

## 🔍 검증 포인트

### Python 서버 로그 확인
```
[2025-12-11 15:30:22] INFO: New streaming request received
[2025-12-11 15:30:22] INFO: Checkpoint: None, Chunk size: 100
[2025-12-11 15:30:22] INFO: Starting data stream...
[2025-12-11 15:30:23] INFO: Progress: 10000/141897 rows (7.0%)
[2025-12-11 15:30:24] INFO: Progress: 20000/141897 rows (14.1%)
...
[2025-12-11 15:30:30] INFO: Stream completed: 141897 rows in 474 chunks
```

### Batch-Server 로그 확인
```
[Connection Test] Successfully received data from Python server
[Full Stream] Processing chunk 1/474
[Full Stream] Processing chunk 10/474
...
[Full Stream] All chunks processed successfully
```

---

## 🧪 추가 테스트 옵션

### Python 독립 테스트 (Batch-Server 없이)
```bash
cd Demo-Python
python src/grpc_client.py
```

**테스트 옵션**:
```bash
# 기본 테스트 (300 rows/chunk)
python src/grpc_client.py

# 작은 chunk로 테스트 (100 rows/chunk)
python src/grpc_client.py --chunk-size 100

# 큰 chunk로 테스트 (1000 rows/chunk)
python src/grpc_client.py --chunk-size 1000

# Checkpoint 테스트
python src/grpc_client.py --checkpoint c0ca96e7-85df-50df-a64e-d934cd02a170
```

---

## ⚙️ 설정 변경

### Batch-Server 설정 (application.yml)

**테스트 활성화/비활성화**:
```yaml
grpc:
  test:
    enabled: true  # false로 변경하면 자동 테스트 비활성화
```

**Chunk Size 변경**:
```yaml
grpc:
  embedding:
    chunk-size: 300  # 원하는 값으로 변경 (100-1000)
```

### Python 서버 설정 (src/config.py)

```python
DEFAULT_CHUNK_SIZE = 300  # 원하는 값으로 변경
```

---

## 🐛 문제 해결

### Python 서버 연결 실패
```
Error: Connection refused to localhost:50051
```

**해결책**:
1. Python 서버가 실행 중인지 확인
2. 방화벽에서 50051 포트 허용 확인
3. Python 서버 로그에서 에러 메시지 확인

### 데이터 파일 없음
```
Error: File not found: processed_recruitment_data.pkl
```

**해결책**:
1. `Demo-Python/` 디렉토리에 .pkl 파일이 있는지 확인
2. 파일 경로가 올바른지 확인
3. `src/config.py`에서 파일 경로 수정

### Proto 버전 불일치
```
Error: Method not found or signature mismatch
```

**해결책**:
1. Proto 파일이 양쪽에서 동일한지 확인
2. Python proto 재생성: `python -m grpc_tools.protoc ...`
3. Java proto 재컴파일: `./gradlew build`

---

## 📊 성능 지표

### 예상 성능
- **Throughput**: ~10,000 rows/sec
- **Latency**: <100ms per chunk
- **Memory**: Python ~500MB, Java ~300MB
- **Duration**: 전체 데이터(141,897 rows) 약 14초

### 실제 측정 방법
- Python 서버: 로그에서 duration 확인
- Batch-Server: `GrpcStreamTestService` 로그에서 시간 측정

---

## 📈 다음 단계

### 1. DB 저장 구현 (Batch-Server)
- MetadataRepository.save()
- EmbeddingRepository.save()
- Transaction 관리

### 2. Spring Batch Job 구성
- Job/Step 정의
- Chunk-oriented processing
- Checkpoint restart

### 3. Scheduler 설정
- Quartz 또는 @Scheduled
- Cron expression 설정

### 4. API Server 연동
- GraphQL Resolver
- Cache 전략 (Caffeine + Redis)

---

## 📝 참고 문서

- **Batch-Server**: `Backend/Batch-Server/GRPC_QUICKSTART.md`
- **Python Server**: `Demo-Python/README.md`
- **전체 설계**: `Backend/Batch-Server/docs/Entire_Structure.md`
- **gRPC 가이드**: `Backend/Batch-Server/docs/gRPC_통신_가이드.md`

---

**작성일**: 2025-12-11
**상태**: Ready for Integration Testing ✅
