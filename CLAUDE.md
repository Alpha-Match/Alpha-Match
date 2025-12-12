# Alpha-Match Project - Claude Instructions

**프로젝트명:** Alpha-Match (Headhunter-Recruit Matching System)
**작성일:** 2025-12-10
**소유자:** 김태현
**아키텍처:** MSA (Microservices Architecture) + gRPC + GraphQL + Vector DB

---

## 📋 프로젝트 목표

이 프로젝트는 **대규모 벡터 기반 추천 시스템의 파이프라인을 작은 단위로 직접 구축**하는 것을 목표로 합니다.

### 3가지 핵심 실험
1. **Reactive 기반 API 서버(WebFlux)로 유연한 GraphQL 조회 환경 구축**
2. **Python Embedding 서버 ↔ Java Batch 서버 간 gRPC Streaming 설계**
3. **Embedding 데이터를 PostgreSQL(pgvector)에 저장하고 캐싱(메모리/Redis)으로 고속화**

### 주요 학습 목표
- Reactive Programming (WebFlux) 실전 적용
- gRPC Streaming 대용량 데이터 전송
- pgvector를 활용한 Vector Similarity Search
- 멀티 레이어 캐싱 전략 (Caffeine + Redis)
- 동시성 제어 및 Race Condition 해결

---

## 🗺️ 핵심 문서 참조

### 🚨 먼저 읽어야 할 문서
- **시스템 아키텍처**: `/docs/시스템_아키텍처.md` 🏗️
- **데이터 플로우**: `/docs/데이터_플로우.md` 🔄
- **개발 우선순위**: `/docs/개발_우선순위.md` 🚀
- **전체 구조 설계**: `/Backend/Batch-Server/docs/Entire_Structure.md` 📘

### 서버별 상세 문서
- **Frontend**: `/Frontend/Front-Server/CLAUDE.md`
- **API Server**: `/Backend/Api-Server/CLAUDE.md`
- **Batch Server**: `/Backend/Batch-Server/CLAUDE.md`
- **Demo Python**: `/Demo-Python/CLAUDE.md`

---

## 📂 프로젝트 구조

```
C:/Final_2025-12-09/Alpha-Match/
│
├── CLAUDE.md                  # 🚨 전체 프로젝트 가이드 (현재 문서)
├── docs/                      # 📚 공통 문서
│   ├── 시스템_아키텍처.md
│   ├── 데이터_플로우.md
│   └── 개발_우선순위.md
│
├── Frontend/
│   └── Front-Server/          # Next.js 16.0.7 + React Query
│
├── Backend/
│   ├── Api-Server/            # Spring WebFlux + GraphQL
│   └── Batch-Server/          # Spring Batch + gRPC Client
│
├── Demo-Python/               # Python gRPC Streaming Server
│
└── deploy/                    # 배포 설정
```

---

## 🔧 시스템 구성 요소

| 서버 | 기술 스택 | 포트 | 역할 |
|-----|---------|-----|-----|
| **Front-Server** | Next.js 16.0.7 | 3000 | GraphQL API 소비, 캐싱 |
| **Api-Server** | Spring WebFlux | 8080, 50052 | GraphQL API, 캐싱, gRPC |
| **Batch-Server** | Spring Batch | N/A | Embedding 수신/저장 |
| **Demo-Python** | Python + gRPC | 50051 | Embedding 스트리밍 |
| **PostgreSQL** | pgvector | 5432 | Vector DB |
| **Redis** | - | 6379 | 분산 캐싱 |

---

## 🚀 현재 진행 상황

### ✅ 완료
- gRPC proto 파일 작성
- DB 스키마 설계 (Flyway)
- Batch Server 기본 구조 (Entity, Repository, Config, gRPC Client)
- 전체 프로젝트 문서화 구조 완성
- **Batch Server gRPC 통신 구현 및 검증 완료** (2025-12-11)
  - Python Server와 통신 성공 (141,897 rows)
  - GrpcStreamTestService, GrpcTestRunner 구현
  - Checkpoint 재개 기능 검증
- **Batch Server 서비스 레이어 구현 완료** (2025-12-12)
  - ChunkProcessor (Reactive → Virtual Thread → Blocking JPA)
  - EmbeddingStreamingService (gRPC Stream → DB 파이프라인)
  - 도메인별 프로세서 패턴 (Factory + Generic)
  - 상세 로깅 (스레드 번호, 청크 사이즈, UUID, 데이터 내용)
- **Python-Java 양방향 gRPC 구현 완료** (2025-12-12)
  - Server Streaming: Python → Java (데이터 전송)
  - Client Streaming: Java ← Python (데이터 수신)
  - 도메인별 제네릭 구조 (recruit, candidate)

### 🔄 진행 중
- Batch Server: Job/Step/Scheduler 구현
- API Server 설계 및 구현 준비

### ⏳ 예정
- API Server GraphQL 구현
- Frontend 구현
- 통합 테스트 및 성능 최적화

**상세 일정**: `/docs/개발_우선순위.md` 참조

---

## 📚 CRITICAL DOCUMENTATION PATTERN

**🚨 중요한 문서 작성 시 반드시 여기에 추가하세요!**

작성하거나 발견한 문서는 즉시 이 섹션에 추가하여 컨텍스트 손실을 방지합니다.

- 아키텍처 다이어그램 → 참조 경로 추가
- 데이터베이스 스키마 → 참조 경로 추가
- 문제 해결 방법 → 참조 경로 추가
- 설정 가이드 → 참조 경로 추가

### Backend 공통 문서 (2025-12-12 추가)
- **DB 스키마 가이드** → `/Backend/docs/DB_스키마_가이드.md`
- **Flyway 마이그레이션 가이드** → `/Backend/docs/Flyway_마이그레이션_가이드.md`
- **ERD 다이어그램** → `/Backend/docs/ERD_다이어그램.md`

### Batch-Server 문서
- **도메인 확장 가이드** → `/Backend/Batch-Server/docs/도메인_확장_가이드.md`

### 예시
- 새로운 gRPC 통신 패턴 → `/docs/gRPC_통신_가이드.md`
- 성능 최적화 결과 → `/docs/성능_최적화_결과.md`

---

## 🛠️ 빠른 시작

### 1. Batch Server 실행
```bash
cd Backend/Batch-Server
./gradlew bootRun
```

### 2. Demo Python Server 실행
```bash
cd Demo-Python
pip install -r requirements.txt
python src/grpc_server.py
```

### 3. 통신 테스트
Batch Server가 자동으로 Python Server에 연결하여 데이터를 수신합니다.

---

## 📝 개발 가이드

### Git 브랜치 전략
- `main`: 안정 버전
- `develop`: 개발 통합
- `feat/*`: 기능 개발
- `fix/*`: 버그 수정

### 문서화 규칙
- 각 서버의 CLAUDE.md: 서버별 상세 설명
- docs/: 공통 기술 설계 문서
- docs/hist/: 작업 히스토리 (날짜별)

### 통신 프로토콜
- Backend 간: gRPC (고성능, Streaming 지원)
- Frontend ↔ Backend: GraphQL (유연한 쿼리)

---

## ⚠️ 주의사항

1. **Demo-Python의 .pkl 파일 직접 조회 금지**
   - 용량이 크므로 메모리 문제 발생 가능
   - 반드시 gRPC 스트리밍을 통해서만 접근

2. **Virtual Thread 사용 시 주의**
   - DB Connection Pool 고갈 방지
   - boundedElastic Scheduler 사용

3. **Race Condition 주의**
   - 캐시 무효화 시 AtomicBoolean 사용
   - Upsert 순서 (metadata → embedding)

---

## 🔗 팀별 액션 포인트

| 팀 | 해야 할 일 |
|-----|----------|
| **Frontend** | GraphQL 스키마 기반 데이터 소비 / React Query 캐싱 전략 |
| **API Backend** | Resolver → Service → Cache → DB 구조 구축 / gRPC 클라이언트 작성 |
| **AI 팀** | pkl → chunk stream 서버 구현 / Embedding 생성·추론 모델 관리 |
| **Batch 팀** | Embedding stream 소비 및 upsert / checkpoint 및 재시작 처리 |
| **Infra 팀** | Postgres(pgvector) + Redis + 서비스 네트워크 구성 / gRPC 설정 |

---

---

## 📋 최근 업데이트

### 2025-12-12 - 서비스 레이어 및 도메인별 제네릭 구조 구현 완료
- **서비스 레이어 구현**
  - ChunkProcessor: Reactive → Virtual Thread → Blocking JPA 전환
  - EmbeddingStreamingService: 3가지 스트리밍 모드 (전체/Checkpoint/병렬)
  - 상세 로깅: 스레드 번호, 청크 사이즈, 마지막 UUID, 데이터 내용
- **도메인별 제네릭 구조**
  - Python의 Protocol 패턴 → Java의 Generic 인터페이스로 매핑
  - DataProcessor\<T\> + DataProcessorFactory (Factory 패턴)
  - RecruitDataProcessor, CandidateDataProcessor 구현
- **양방향 gRPC 구현**
  - Server Streaming: Batch → Python 데이터 요청
  - Client Streaming: Python → Batch 데이터 수신
  - 메타데이터 기반 도메인 라우팅
- 상세 내역: `/Backend/Batch-Server/docs/구현_요약_2025-12-12.md`

### 2025-12-11 - gRPC 통신 구현 완료
- Python Server와 gRPC Streaming 통신 성공 (141,897 rows)
- GrpcStreamTestService, GrpcTestRunner 구현
- Checkpoint 재개 기능 검증 완료
- 상세 내역: `/Backend/Batch-Server/hist/2025-12-11_01_gRPC_Client_구현_및_통신_검증.md`

---

**최종 수정일:** 2025-12-12