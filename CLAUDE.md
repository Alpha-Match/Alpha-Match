# Alpha-Match Project - Claude Instructions

**프로젝트명:** Alpha-Match (Headhunter-Recruit Matching System)
**아키텍처:** MSA (Microservices Architecture) + gRPC + GraphQL + Vector DB

---

## 📋 문서 목적

- **CLAUDE.md (이 문서)**: AI 에이전트가 개발 시 참조할 메타 정보, 경로, 규칙
- **README.md**: 사람이 읽을 프로젝트 소개, 설치 및 사용법

---

## 🎯 프로젝트 목표

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

## 🗺️ 핵심 문서 경로 (상세 내용은 해당 문서 참조)

### 🗄️ Backend 공통 문서 (DB 작업 시 필수)
- **DB 스키마 가이드**: `/Backend/docs/DB_스키마_가이드.md`
- **테이블 명세서**: `/Backend/docs/table_specification.md` ⭐ Single Source of Truth
- **ERD 다이어그램**: `/Backend/docs/ERD_다이어그램.md`
- **Flyway 마이그레이션 가이드**: `/Backend/docs/Flyway_마이그레이션_가이드.md`

**DB 작업 체크리스트:**
- ✅ Entity 작성 전 → `table_specification.md` 확인
- ✅ Repository 구현 전 → `DB_스키마_가이드.md` 확인
- ✅ Proto 메시지 정의 전 → `table_specification.md` 확인
- ✅ DB 스키마 변경 시 → `Flyway_마이그레이션_가이드.md` 확인

### 📘 Batch Server
- **아키텍처 및 개발 패턴**: `/Backend/Batch-Server/docs/Spring_Batch_개발_가이드.md`
- **도메인 확장 가이드**: `/Backend/Batch-Server/docs/도메인_확장_가이드.md`
- **동시성 제어 전략**: `/Backend/Batch-Server/docs/동시성_제어.md`
- **전체 구조 설계**: `/Backend/Batch-Server/docs/Entire_Structure.md`

### 🐍 Demo Python
- **서버 개발 가이드**: `/Demo-Python/docs/Python_서버_개발_가이드.md`
- **데이터 처리 가이드**: `/Demo-Python/docs/데이터_처리_가이드.md` (Chunk Loader, 도메인 모델)
- **gRPC 통신 가이드**: `/Demo-Python/docs/gRPC_통신_가이드.md` (Client Streaming)

### 🌐 Frontend
- **Frontend 가이드**: `/Frontend/Front-Server/CLAUDE.md`

### 🏗️ 시스템 아키텍처 (공통)
- **시스템 아키텍처**: `/docs/시스템_아키텍처.md`
- **데이터 플로우**: `/docs/데이터_플로우.md`
- **개발 우선순위**: `/docs/개발_우선순위.md`

---

## 🚀 현재 구현 상태

### ✅ 완료
- **DB 스키마 v2**: Flyway V2 (2025-12-21 스키마 재구조화)
  - 벡터 차원 통일 (384d)
  - 새 테이블 추가 (skill_category_dic, recruit/candidate_description, recruit_skill)
  - TIMESTAMPTZ 적용
- **Batch Server 엔티티 v2**: 11개 엔티티 완료
  - Recruit 도메인: 5개 (RecruitEntity, RecruitDescriptionEntity, RecruitSkillEntity, RecruitSkillId, RecruitSkillsEmbeddingEntity)
  - Candidate 도메인: 5개 (CandidateEntity, CandidateDescriptionEntity, CandidateSkillEntity, CandidateSkillId, CandidateSkillsEmbeddingEntity)
  - Skill Dictionary: 2개 (SkillCategoryDicEntity, SkillEmbeddingDicEntity)
- **Batch Server Repository v2**: 12개 완료
  - Recruit: 4개 Domain + 4개 JPA (Native Upsert, 복합키, 벡터검색)
  - Candidate: 1개 Domain + 1개 JPA (CandidateDescription 신규)
  - Skill Dictionary: 2개 Domain + 2개 JPA (UUID 자동생성)
- **Batch Server 기반**: Factory 패턴 + Quartz Scheduler + gRPC Client/Server
- **Demo Python v2**:
  - Proto v2 (RecruitRow 11필드, 벡터 384d 통일)
  - Domain Models v2 (RecruitData, CandidateData, SkillEmbeddingDicData)
  - 전처리 파이프라인 (컬럼 매핑, Exp Years 변환, 필터링, numpy→list)
  - Skill Embeddings 전용 로더 (synonyms 제외)
  - gRPC Server + Chunk Loader + 도메인별 제네릭 구조
- **Python-Java gRPC 양방향 통신**: Client Streaming (Python → Java)
- **Spring Boot 4.0**: Jackson 3 마이그레이션
- **Frontend**: Apollo Client 4.0, 전역 에러 처리, 동적 TECH_STACKS 연동
- **DB 초기화 및 Batch Server 기동 (2025-12-22)**:
  - PostgreSQL alpha_match DB 초기화 (reset_db.bat)
  - Flyway V1, V2 수동 마이그레이션 실행 (run_migrations.bat)
  - v2 스키마 전체 테이블 생성 완료
  - Quartz 설정 최적화 (auto-startup: false, RAMJobStore)
  - Batch Server 성공적 기동 (gRPC 9090, HTTP 8080)
- **PGvector 직렬화 문제 해결 및 파이프라인 검증 (2025-12-22)**:
  - Repository 3개 수정 (RecruitSkillsEmbedding, CandidateSkillsEmbedding, SkillEmbeddingDic)
  - PGvector → String 변환 (.toString()) 후 PostgreSQL vector 타입으로 CAST
  - bytea → vector 변환 오류 완전 해결
  - End-to-End 파이프라인 검증 성공 (Python → gRPC → Java → PostgreSQL)
  - Recruit 도메인 87,488 레코드 (471MB) 처리 완료
  - 4-table 동시 upsert 검증 (recruit, recruit_skill, recruit_description, recruit_skills_embedding)
  - 384차원 Vector Embedding 저장 완전 검증 ✅

### 🔄 진행 중
- 없음

### ⏳ 예정
- Candidate 도메인 파이프라인 테스트
- API Server 설계 및 구현
- GraphQL 구현 (Resolver → Service → Cache → DB)
- Frontend GraphQL 쿼리 구현, React Query 캐싱
- 성능 최적화 및 모니터링

**상세 일정**: `/docs/개발_우선순위.md` 참조

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

## ⚠️ AI가 반드시 알아야 할 규칙

### 1. 개발 시 금지 사항
- **.pkl 파일 직접 읽기 금지**: 용량이 크므로 메모리 문제 발생 → 반드시 gRPC 스트리밍 사용
- **DB 작업 전 table_specification.md 확인 필수**: 임의로 스키마 추정 금지
- **Virtual Thread 사용 시**: DB Connection Pool 고갈 방지 위해 boundedElastic Scheduler 사용

### 2. 문서 관리 규칙

#### CLAUDE.md는 "현재 상태"만 관리
**❌ 포함하지 말 것:**
- 날짜별 변경 이력 ("📋 최근 업데이트" 섹션)
- "2025-XX-XX에 구현" 같은 시간 기반 정보
- 코드 상세 내용 (참조 경로만 제공)

**✅ 포함할 것:**
- 현재 구현된 기능 (✅ 완료)
- 현재 진행 중인 작업 (🔄 진행 중)
- 앞으로 할 작업 (⏳ 예정)
- 핵심 문서 경로 및 개발 규칙

**시간별 변경사항 추적:**
- Git commit history: `git log --oneline --graph`
- 히스토리 문서: `/docs/hist/YYYY-MM-DD_NN_제목.md`
- Pull Request 설명

#### 문서 계층 구조

**Tier 1 - 고정 문서 (코드 변경 시 즉시 업데이트)**
- `/CLAUDE.md` (루트 프로젝트 개요)
- `/Backend/Batch-Server/CLAUDE.md` 대신 → 고정 문서 3개
  - `Spring_Batch_개발_가이드.md`
  - `도메인_확장_가이드.md`
  - `동시성_제어.md`
- `/Demo-Python/CLAUDE.md` 대신 → 고정 문서 3개
  - `Python_서버_개발_가이드.md`
  - `데이터_처리_가이드.md`
  - `gRPC_통신_가이드.md`

**Tier 2 - 아키텍처 문서 (구조 변경 시 업데이트)**
- `/docs/시스템_아키텍처.md`
- `/Backend/docs/DB_스키마_가이드.md`
- `/Backend/docs/table_specification.md`

**Tier 3 - 히스토리 문서 (Read-Only, 추가만 가능)**
- `/Backend/Batch-Server/docs/hist/YYYY-MM-DD_NN_제목.md`
- `/Demo-Python/docs/hist/YYYY-MM-DD_NN_제목.md`

### 3. 기능 구현 시 워크플로우

```
✅ 코드 작성 및 테스트
  ↓
✅ 히스토리 문서 작성 (선택, 중요한 결정 사항만)
  ↓
✅ 고정 문서 업데이트 (해당 시)
  ↓
✅ CLAUDE.md 업데이트 ("⏳ 예정" → "✅ 완료")
  ↓
✅ Commit
```

**간단한 규칙: "1 Feature = 1 CLAUDE.md Update"**

### 4. 커밋 전 체크리스트
- [ ] 새 기능이 "✅ 완료" 섹션에 추가되었는가?
- [ ] "⏳ 예정" 섹션에서 해당 항목이 삭제되었는가?
- [ ] 고정 문서가 업데이트되었는가? (해당 시)
- [ ] 커밋 메시지에 변경사항이 명확히 기록되었는가?

---

## 📝 개발 가이드

### Git 브랜치 전략
- `main`: 안정 버전
- `develop`: 개발 통합
- `feat/*`: 기능 개발
- `fix/*`: 버그 수정

### 통신 프로토콜
- Backend 간: gRPC (고성능, Streaming)
- Frontend ↔ Backend: GraphQL (유연한 쿼리)

---

## 🛠️ 빠른 시작

### Batch Server 실행
```bash
cd Backend/Batch-Server
./gradlew bootRun
```

### Demo Python Server 실행
```bash
cd Demo-Python
pip install -r requirements.txt
python src/grpc_server.py
```

### 통신 테스트
Batch Server가 자동으로 Python Server에 연결하여 데이터를 수신합니다.

---

**최종 수정일:** 2025-12-22
