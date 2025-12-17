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

### 🗄️ Backend 공통 문서 (DB 작업 시 필수) ⭐
> **중요**: API Server, Batch Server, Demo-Python 작업 시 반드시 아래 문서를 먼저 확인하세요.

- **DB 스키마 가이드**: `/Backend/docs/DB_스키마_가이드.md` - DB 스키마 전체 구조
- **테이블 명세서**: `/Backend/docs/table_specification.md` - 단일 소스 (Single Source of Truth)
- **ERD 다이어그램**: `/Backend/docs/ERD_다이어그램.md` - 시각적 ERD
- **Flyway 마이그레이션 가이드**: `/Backend/docs/Flyway_마이그레이션_가이드.md` - DB 변경 정책

**DB 작업 체크리스트:**
- ✅ Entity 작성 전: `table_specification.md` 확인
- ✅ Repository 구현 전: `DB_스키마_가이드.md` 확인
- ✅ Proto 메시지 정의 전: `table_specification.md` 확인
- ✅ DB 스키마 변경 시: `Flyway_마이그레이션_가이드.md` 확인

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
- DB 스키마 설계 (Flyway V1-V5)
  - V1: Recruit 기본 스키마
  - V2-V5: Candidate, 도메인 범용화, 인덱스, 제약조건 (2025-12-12)
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
  - Client Streaming: Python → Java (데이터 전송, IngestDataStream RPC)
  - 도메인별 제네릭 구조 (recruit 384d, candidate 768d)
  - FastAPI + gRPC Client 하이브리드 아키텍처
- **Spring Boot 4.0 마이그레이션 완료** (2025-12-12)
  - Jackson 3 적용 (ObjectMapper → JsonMapper)
  - JacksonConfig 구성
- **Backend 공통 문서화 완료** (2025-12-12)
  - DB 스키마 가이드, Flyway 마이그레이션 가이드, ERD 다이어그램
  - API Server와 공유 가능한 단일 문서
- **계층별 커밋 완료** (2025-12-12)
  - Batch: 7개 커밋, Python: 7개 커밋
- **Batch Server Factory 패턴 + Quartz Scheduler 구현 완료** (2025-12-16)
  - DomainJobFactory (Factory Method 패턴)
  - BatchJobConfig 리팩토링 (Factory 위임)
  - QuartzConfig (JDBC JobStore)
  - BatchSchedulerConfig (Cron 기반 자동 실행)
  - Recruit Job 스케줄 (매일 새벽 2시)

### 🔄 진행 중
- API Server 설계 및 구현 준비

### ⏳ 예정
- Batch Server: Candidate Job 추가, gRPC Server 구현
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

## 📖 문서 관리 프로세스 (중요!)

**🚨 문서와 코드 간 괴리 방지를 위한 필수 프로세스**

### 1. 코드 변경 시 즉시 문서 업데이트 (강제 규칙)

#### 새 기능 구현 시 체크리스트
```
✅ 코드 작성
✅ 테스트 작성
✅ CLAUDE.md 업데이트 (필수!)
  - "⏳ 예정" → "✅ 완료" 이동
  - 실제 구조 섹션 업데이트
  - "최근 업데이트" 섹션에 날짜 + 변경 사항 기록
✅ 커밋
```

#### 예시: DomainJobFactory 구현 완료 시
**Batch-Server/CLAUDE.md 업데이트:**
```markdown
## ⏳ 구현 예정
- ⏳ DomainJobFactory  ← 삭제

## ✅ 완료된 기능
- ✅ DomainJobFactory - 도메인별 Job/Step 동적 생성  ← 추가

## 📋 최근 업데이트
### 2025-12-17 - DomainJobFactory 구현 완료  ← 추가
- ✅ 도메인별 Job/Step 동적 생성
- ✅ YAML 설정만으로 새 도메인 추가 가능
```

### 2. 커밋 메시지에 문서 업데이트 명시

```bash
# Good ✅
git commit -m "feat(batch): DomainJobFactory 구현

- 도메인별 Job/Step 동적 생성
- YAML 기반 설정 활용
- CLAUDE.md 업데이트 완료 ✅
"

# Bad ❌
git commit -m "feat(batch): DomainJobFactory 구현"
# 문서 업데이트 없음!
```

### 3. 문서 계층 구조 유지

#### Tier 1 문서 (항상 실제 코드와 일치해야 함)
```
🔴 최우선 동기화 필요
├── /CLAUDE.md                           (루트 프로젝트 개요)
├── /Backend/Batch-Server/CLAUDE.md      (Batch Server 상세)
├── /Backend/Api-Server/CLAUDE.md        (API Server 상세)
├── /Demo-Python/CLAUDE.md               (Python Server 상세)
└── /Frontend/Front-Server/CLAUDE.md     (Frontend 상세)
```

#### Tier 2 문서 (아키텍처 변경 시 업데이트)
```
🟡 필요 시 업데이트
├── /docs/시스템_아키텍처.md
├── /docs/데이터_플로우.md
├── /Backend/docs/DB_스키마_가이드.md
└── /Backend/docs/Flyway_마이그레이션_가이드.md
```

#### Tier 3 문서 (히스토리 - 추가만 가능, 수정 불가)
```
🟢 히스토리 (Read-Only)
├── /Backend/Batch-Server/docs/hist/
│   ├── 2025-12-11_01_gRPC_Client_구현_및_통신_검증.md
│   ├── 2025-12-12_01_서비스_레이어_구현.md
│   └── 2025-12-17_01_문서_구조_개선.md
└── /Demo-Python/docs/hist/
    └── 2025-12-12_01_FastAPI_및_클라이언트_스트리밍_구현.md
```

### 4. 고정 문서 vs 히스토리 문서 (2025-12-17 정립)

#### 고정 문서 (Fixed Documents)
각 서버의 핵심 참조 문서로, 항상 최신 상태를 유지해야 합니다:

**Batch-Server 고정 문서 (3개):**
1. `Spring_Batch_개발_가이드.md` - 아키텍처 및 개발 패턴
2. `도메인_확장_가이드.md` - 도메인 추가 절차
3. `동시성_제어.md` - 동시성 제어 전략

**Demo-Python 고정 문서 (3개):**
1. `Python_서버_개발_가이드.md` - 전체 아키텍처
2. `데이터_처리_가이드.md` - Chunk Loader, 도메인 모델
3. `gRPC_통신_가이드.md` - Client Streaming

**특징:**
- 코드 변경 시 즉시 업데이트
- 참조는 고정 문서로만 진행
- 중복 없이 명확한 역할 분리

#### 히스토리 문서 (History Documents)
날짜별 작업 이력을 기록하는 Read-Only 문서:

**파일명 규칙:**
```
/docs/hist/YYYY-MM-DD_NN_간략한_제목.md
예: 2025-12-17_01_문서_구조_개선.md
```

**특징:**
- 작성 후 수정 불가 (Read-Only)
- 중요한 기술 결정 사항 기록
- 문제 해결 과정 문서화
- 고정 문서에 반영할 내용 정리

#### 작업 프로세스
```
코드 작성 및 테스트
  ↓
히스토리 문서 작성 (/docs/hist/YYYY-MM-DD_NN_제목.md)
  ↓
고정 문서 업데이트 (필요 시)
  ↓
CLAUDE.md 업데이트 (구현 상태 반영)
  ↓
Commit
```

### 5. 간단한 규칙: "1 Feature = 1 CLAUDE.md Update"

```
✅ Good:
- DomainJobFactory 구현 → CLAUDE.md 즉시 업데이트
- Quartz Scheduler 추가 → CLAUDE.md 즉시 업데이트
- 각 기능마다 문서 업데이트

❌ Bad:
- 여러 기능 구현 후 문서 한번에 업데이트 (동기화 어려움)
- 문서 업데이트 없이 커밋 (괴리 발생)
```

### 5. AI 에이전트 활용 (권장)

#### 매 작업 세션 시작 시
1. AI 에이전트가 CLAUDE.md를 읽고 실제 코드와 일치하는지 검증
2. 불일치 발견 시 즉시 알림
3. 사용자 확인 후 문서 업데이트

#### 기능 구현 완료 시
1. 사용자: "DomainJobFactory 구현 완료"
2. AI 에이전트: 자동으로 CLAUDE.md 업데이트 제안
   - "⏳ 예정" → "✅ 완료" 이동
   - "최근 업데이트" 섹션 추가
3. 사용자 리뷰 후 커밋

### 6. 문서 검증 체크리스트 (커밋 전)

- [ ] 새 기능이 "✅ 완료" 섹션에 추가되었는가?
- [ ] "⏳ 예정" 섹션에서 해당 항목이 삭제되었는가?
- [ ] "📂 실제 프로젝트 구조"에 새 파일이 추가되었는가?
- [ ] "📋 최근 업데이트" 섹션에 날짜와 내용이 기록되었는가?
- [ ] 커밋 메시지에 "CLAUDE.md 업데이트 완료 ✅" 명시되었는가?

### 7. 문서 우선순위 (중요도 순)

1. **CLAUDE.md** - 각 서버의 실제 구현 상태 (최우선)
2. **코드 주석** - 복잡한 로직 설명
3. **ERD/아키텍처 다이어그램** - 구조 변경 시
4. **히스토리 문서** - 의사결정 기록 (선택)

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

### 2025-12-17 - Candidate 도메인 완전 구현 완료
- ✅ **Proto 파일 확장** - 3개 도메인 지원 (Recruit, Candidate, SkillEmbeddingDic)
  - oneof로 도메인별 분기 처리
  - CandidateRow (Flat DTO), SkillEmbeddingDicRow 추가
- ✅ **Java Entity 5개 생성** - SQL 스키마 정확 매핑
  - CandidateEntity, CandidateSkillEntity (복합 PK)
  - CandidateSkillsEmbeddingEntity (PostgreSQL 배열)
  - SkillEmbeddingDicEntity
- ✅ **Java Repository 8개 구현** - Clean Architecture
  - Domain Interface 4개 (Port)
  - Infrastructure JpaRepository 4개 (Adapter + Upsert Native Query)
- ✅ **Batch Processor/Writer** - Candidate 3개 테이블 분산 저장
  - CandidateItemProcessor (Proto → 3개 Entity 변환)
  - CandidateItemWriter (candidate, candidate_skill, candidate_skills_embedding)
- ✅ **Python Chunk Loader** - 메모리 효율적 대용량 파일 처리
  - BaseChunkLoader + Iterator 패턴
  - 3가지 포맷 지원 (pkl, csv, parquet)
  - 도메인별 + 포맷별 확장 가능 구조
- ✅ **Python 도메인 모델** - Pydantic Validation 강화
  - CandidateData, SkillEmbeddingDicData 추가
  - 벡터 차원 검증 (384d, 768d)

### 2025-12-16 - Batch Server Factory 패턴 + Quartz Scheduler 구현 완료
- ✅ **DomainJobFactory 구현** - Factory Method 패턴으로 도메인별 Job/Step 동적 생성
- ✅ **BatchJobConfig 리팩토링** - 하드코딩된 Job 생성 → Factory 위임
- ✅ **QuartzConfig 구현** - JDBC JobStore, ThreadPool 10개, Misfire 60초
- ✅ **BatchSchedulerConfig 구현** - Quartz + Spring Batch 통합
- ✅ **Recruit Job 스케줄** - Cron 기반 자동 실행 (기본: 매일 새벽 2시)
- ✅ **YAML 설정** - batch.scheduler.jobs.recruit.cron으로 스케줄 관리
- ✅ **문서 동기화** - Batch-Server CLAUDE.md 업데이트 완료

### 2025-12-16 - 문서 관리 프로세스 정립
- ✅ **문서와 코드 간 괴리 방지 프로세스 수립**
  - "1 Feature = 1 CLAUDE.md Update" 규칙
  - Tier 1/2/3 문서 계층 구조 명시
  - 커밋 전 문서 검증 체크리스트
- ✅ **Batch-Server CLAUDE.md 전면 재작성**
  - 실제 코드 기준으로 작성
  - 구현된 기능 vs 예정 기능 명확히 분리
  - AI 에이전트용 필독 사항 추가

### 2025-12-12 - Python-Java gRPC 시스템 완전 통합 완료
- **서비스 레이어 구현**
  - ChunkProcessor: Reactive → Virtual Thread → Blocking JPA 전환
  - EmbeddingStreamingService: 3가지 스트리밍 모드 (전체/Checkpoint/병렬)
  - 상세 로깅: 스레드 번호, 청크 사이즈, 마지막 UUID, 데이터 내용
- **도메인별 제네릭 구조 (Python ↔ Java 매핑)**
  - Python: Protocol + TypeVar(covariant=True) + Factory
  - Java: Generic Interface + Factory + Spring Bean 자동 등록
  - 도메인: recruit (384d), candidate (768d)
- **Jackson 3 마이그레이션**
  - Spring Boot 4.0+ 권장 사항 적용
  - ObjectMapper → JsonMapper 전환
  - JacksonConfig + jackson-datatype-jsr310 추가
- **도메인별 DB 스키마 설계 및 Flyway 마이그레이션**
  - V2: Candidate 스키마 (768d)
  - V3: Domain 컬럼 추가 (DLQ/Checkpoint 범용화)
  - V4: 성능 인덱스
  - V5: 제약조건, 트리거, 헬퍼 함수
  - Base Entity 패턴 (BaseMetadataEntity, BaseEmbeddingEntity)
- **Backend 공통 문서 작성**
  - DB 스키마 가이드, Flyway 마이그레이션 가이드, ERD 다이어그램
  - API Server와 Batch Server 공유 가능한 단일 문서화
- **테스트 코드 정리**
  - Batch: GrpcStreamTestService, GrpcTestRunner 제거 (테스트 전용)
  - Batch: EmbeddingStreamRunner 유지 (@ConditionalOnProperty)
  - Python: test_client.bat 제거
- **계층별 커밋 완료**
  - Batch Server: 7개 커밋 (Config → Database → Domain → Docs)
  - Demo Python: 7개 커밋 (문서 → Config → Domain → Infrastructure → Service → API)
- 상세 내역: `/Backend/Batch-Server/docs/구현_요약_2025-12-12.md`

### 2025-12-11 - gRPC 통신 구현 완료
- Python Server와 gRPC Streaming 통신 성공 (141,897 rows)
- GrpcStreamTestService, GrpcTestRunner 구현
- Checkpoint 재개 기능 검증 완료
- 상세 내역: `/Backend/Batch-Server/hist/2025-12-11_01_gRPC_Client_구현_및_통신_검증.md`

---

**최종 수정일:** 2025-12-17