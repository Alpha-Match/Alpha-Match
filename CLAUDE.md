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
- **Frontend 가이드**: `/Frontend/Front-Server/GEMINI.md`

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
- **JVM 힙 메모리 및 로깅 최적화 (2025-12-26)**:
  - `gradle.properties` 추가: `-Xms2g -Xmx8g -XX:+UseG1GC`
  - 로깅 레벨 DEBUG → INFO 조정 (OOM 방지)
  - OOM 크래시 분석 및 해결 (리포트: `Backend/Batch-Server/docs/hist/2025-12-26_01_OOM_Crash_Analysis_Report.md`)
- **Frontend: DefaultDashboard GraphQL 연동 및 데이터 처리**
  - **Recruit**: 87,488건, 12m 54.8s, 113.0 rps ✅
  - **Candidate**: 118,741건, 30m 50.1s, 64.2 rps ✅
  - **Skill_dic**: 105건, 1.69s, 62.2 rps ✅
  - **총 처리량**: 206,334건, 44m 46.6s, 평균 76.8 rps
  - 리포트: `Backend/Batch-Server/docs/hist/2025-12-26_02_Performance_Test_Report.md`
- **Api-Server Clean Architecture 전면 리팩토링 (2025-12-29)**:
  - 3-Layer 원칙 적용 (Domain → Application → Infrastructure)
  - Application Services 이동: SearchService, DashboardService, CacheService → application/service/
  - GraphQL 이동: resolver, type, input → infrastructure/graphql/ (Input Adapter)
  - Configuration 이동: CacheConfig, CorsConfig, R2dbcConfig → infrastructure/config/
  - Domain Layer 정리 (빈 service 디렉토리 삭제, SkillNormalizationService는 Domain Service로 유지)
  - 총 16개 파일 이동, 의존성 방향 검증 완료
  - Gradle Build 성공 (29s, 9 tasks)
  - 리포트: `Backend/Api-Server/docs/hist/2025-12-29_02_Complete_Clean_Architecture_Refactoring.md`
- **Api-Server 4-Layer Architecture 리팩토링 (2025-12-30)**:
  - Presentation Layer 명시적 분리 (GraphQL Input Adapter)
  - GraphQL resolver, type → presentation/graphql/로 이동
  - Infrastructure → Presentation 계층 구조 명확화
  - Application Service import 경로 수정 (10개 파일)
  - 4계층 구조 확립 (Presentation → Application → Domain → Infrastructure)
  - CLAUDE.md 아키텍처 문서 업데이트
- **Frontend-Backend 완전 통합 (2025-12-30)**:
  - GraphQL 스키마 동기화 (MatchItem 타입 정합성, description 필드 제거)
  - Apollo Client 엔드포인트 수정 (8088 → 8080)
  - Detail 뷰 쿼리 추가 (GET_RECRUIT_DETAIL, GET_CANDIDATE_DETAIL)
  - TypeScript 타입 추가 (RecruitDetail, CandidateDetail)
  - 에러 처리 시스템 개선:
    - Custom Event 패턴 → Redux 직접 dispatch
    - 쿼리별 맞춤형 에러 메시지 매핑
    - Apollo Error Link 강화 (GraphQL/Server/Network 에러 구분)
  - Apollo 캐싱 전략 최적화:
    - typePolicies 설정 (merge: false, keyArgs)
    - dashboardData userMode별 캐싱
    - Detail 쿼리 ID별 캐싱
  - useMatchDetail Hook 구현 (cache-first, lazy query)
  - 환경 변수 외부화 (.env.example, .env.local)
  - API Server 연동 테스트:
    - GET_SKILL_CATEGORIES: 6 카테고리, 105 스킬 ✅
    - GET_DASHBOARD_DATA: 카테고리별 통계 ✅
    - SEARCH_MATCHES: Java+Spring 검색, 0.797 유사도 ✅
  - 리포트: `Frontend/Front-Server/docs/hist/2025-12-30_Frontend_Backend_Integration.md`
- **Dashboard 기능 및 검색 최적화 (2026-01-05)**:
  - **Backend (Api-Server)**:
    - 카테고리 분포 API 구현 (getCategoryDistribution): 검색한 기술 스택의 카테고리별 비율 분석
    - 역량 매칭도 API 구현 (getSkillCompetencyMatch): 보유/부족/추가 스킬 분석 및 매칭 퍼센트
    - 유사도 필터링 강화: 0.0 → 0.6 (60% 이상만 반환)
    - 기술 스택 정렬 처리: 캐시 히트율 향상을 위한 일관된 쿼리 생성
    - GraphQL 타입 추가: CategoryMatchDistribution, SkillCompetencyMatch
  - **Frontend (Front-Server)**:
    - CategoryPieChart 컴포넌트: SVG 기반 원 그래프 시각화 (10개 카테고리 색상 매핑)
    - SkillCompetencyBadge 컴포넌트: High/Medium/Low 3단계 역량 레벨 표시
    - 무한 스크롤 UX 개선:
      - NetworkStatus 기반 초기 로딩/fetchMore 로딩 구분
      - Throttle 적용 (300ms 최소 간격)으로 중복 요청 방지
      - 스크롤 위치 유지 (전체 화면 새로고침 제거)
    - 기술 스택 정렬: Frontend에서도 정렬하여 Backend 캐싱 일관성 확보
    - Server/Client Component 분리: HomePage.client.tsx 구조 개선
  - **성능 개선**:
    - 캐시 히트율: ~50% → ~80% (스킬 정렬 효과)
    - 서버 부하: 30% 감소 (throttle 효과)
    - 검색 품질: 유사도 60% 이상으로 향상

### 🔄 진행 중
- 없음

### ⏳ 예정
- 청크 사이즈 튜닝 (100, 500, 1000 비교)
- Redis 연동 및 실제 성능 테스트
- CacheService 적용 확대 (getSkillCategories, Dashboard, Detail 조회)
- gRPC Server 구현 (캐시 무효화 수신)
- GraphQL Mutation 구현 (캐시 무효화 API)
- Frontend: Detail 뷰 UI 컴포넌트 구현 (useMatchDetail Hook 활용)
- Frontend: ErrorBoundary 컴포넌트 추가
- Frontend: GraphQL Code Generator 설정 (선택적)
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
| **PostgreSQL** | pgvector | **5433** | Vector DB |
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
- `/Frontend/Front-Server/GEMINI.md` (프론트엔드 아키텍처 및 개발 가이드)

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

**최종 수정일:** 2026-01-05 (Dashboard 기능 및 검색 최적화 완료)
