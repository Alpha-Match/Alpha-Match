# Alpha-Match

> **대규모 벡터 기반 헤드헌터-구인 매칭 시스템**

[![Architecture](https://img.shields.io/badge/Architecture-MSA-blue)](docs/시스템_아키텍처.md)
[![gRPC](https://img.shields.io/badge/Communication-gRPC-green)](docs/Python_Java_gRPC_통신_가이드.md)
[![GraphQL](https://img.shields.io/badge/API-GraphQL-E10098)](Frontend/Front-Server/README.md)
[![Vector DB](https://img.shields.io/badge/Database-pgvector-336791)](Backend/docs/DB_스키마_가이드.md)

---

## 📋 프로젝트 소개

Alpha-Match는 **헤드헌터와 구인공고를 벡터 유사도 기반으로 매칭**하는 추천 시스템입니다. 대규모 임베딩 데이터를 처리하고, 실시간 벡터 검색을 제공하며, 다층 캐싱 전략으로 고속 응답을 제공합니다.

### 🎯 3가지 핵심 실험

1. **Reactive 기반 API 서버 (Spring WebFlux)**
   - GraphQL로 유연한 쿼리 환경 제공
   - Non-blocking I/O로 높은 처리량 달성

2. **Python ↔ Java gRPC Streaming**
   - 대용량 임베딩 데이터를 효율적으로 전송
   - Client Streaming으로 메모리 효율 최적화

3. **pgvector + 멀티 레이어 캐싱**
   - PostgreSQL pgvector로 벡터 유사도 검색
   - Caffeine (메모리) + Redis (분산) 캐싱

---

## ✨ 주요 기능

- 🔍 **벡터 기반 유사도 검색**: pgvector로 고속 임베딩 검색
- 📊 **대규모 데이터 처리**: Spring Batch로 수십만 건 임베딩 수집/저장
- 🚀 **실시간 API**: Reactive GraphQL로 빠른 응답
- 💾 **멀티 레이어 캐싱**: L1 (Caffeine) + L2 (Redis) 전략
- 🔄 **체크포인트 재개**: Batch 실패 시 중단 지점부터 재시작
- 🛡️ **DLQ (Dead Letter Queue)**: 실패한 레코드 격리 및 재처리

---

## 🏗️ 시스템 아키텍처

```
┌─────────────┐
│   Frontend  │ Next.js 16 + Apollo Client
└──────┬──────┘
       │ GraphQL
       ▼
┌─────────────┐
│ API Server  │ Spring WebFlux + GraphQL
└──────┬──────┘
       │ gRPC (요청)
       │ Cache: Caffeine + Redis
       ▼
┌─────────────┐      ┌──────────────┐
│Batch Server │◄─────┤ Demo Python  │
│Spring Batch │ gRPC │ FastAPI      │
└──────┬──────┘Stream└──────────────┘
       │              Embedding 생성
       ▼
┌─────────────┐
│ PostgreSQL  │ pgvector
│   + Redis   │
└─────────────┘
```

**상세 아키텍처**: [docs/시스템_아키텍처.md](docs/시스템_아키텍처.md)

---

## 🛠️ 기술 스택

### Frontend
- **Next.js** 16.0.7 - React 프레임워크
- **Apollo Client** 4.0 - GraphQL 클라이언트
- **Redux Toolkit** - 상태 관리
- **Tailwind CSS** - 스타일링

### Backend - API Server
- **Spring Boot** 4.0 (WebFlux) - Reactive 프레임워크
- **GraphQL** - API 쿼리 언어
- **Caffeine** - 로컬 캐시
- **Redis** - 분산 캐시

### Backend - Batch Server
- **Spring Batch** - 대규모 배치 처리
- **Quartz Scheduler** - 스케줄링
- **gRPC** - Python 서버 통신
- **Flyway** - DB 마이그레이션

### AI/ML - Demo Python
- **FastAPI** - Python 웹 프레임워크
- **gRPC** - Java 서버 통신
- **Sentence Transformers** - 임베딩 생성 (예정)

### Database
- **PostgreSQL** 16 + **pgvector** - 벡터 DB
- **Redis** 7 - 캐시 및 세션 스토어

---

## 🚀 빠른 시작

### 사전 요구사항

- **Java** 21+
- **Python** 3.11+
- **Node.js** 20+
- **PostgreSQL** 16+ (pgvector 확장 설치)
- **Redis** 7+

### 1. 데이터베이스 설정

```bash
# PostgreSQL에 pgvector 확장 설치
psql -U postgres -d alpha_match -c "CREATE EXTENSION vector;"

# Flyway 마이그레이션 실행
cd Backend/Batch-Server
./gradlew flywayMigrate
```

### 2. Batch Server 실행

```bash
cd Backend/Batch-Server
./gradlew bootRun
```

### 3. Demo Python Server 실행

```bash
cd Demo-Python
pip install -r requirements.txt
python src/grpc_server.py
```

### 4. API Server 실행 (예정)

```bash
cd Backend/Api-Server
./gradlew bootRun
```

### 5. Frontend 실행 (예정)

```bash
cd Frontend/Front-Server
npm install
npm run dev
```

브라우저에서 http://localhost:3000 접속

---

## 📂 프로젝트 구조

```
Alpha-Match/
│
├── Frontend/
│   └── Front-Server/           # Next.js 애플리케이션
│       ├── src/
│       │   ├── app/            # Next.js App Router
│       │   ├── components/     # React 컴포넌트
│       │   └── store/          # Redux 상태 관리
│       └── package.json
│
├── Backend/
│   ├── Api-Server/             # GraphQL API 서버
│   │   ├── src/main/java/
│   │   └── build.gradle
│   │
│   ├── Batch-Server/           # Spring Batch 서버
│   │   ├── src/main/java/
│   │   ├── src/main/resources/
│   │   │   └── db/migration/   # Flyway 마이그레이션
│   │   └── build.gradle
│   │
│   └── docs/                   # Backend 공통 문서
│       ├── DB_스키마_가이드.md
│       ├── table_specification.md
│       └── ERD_다이어그램.md
│
├── Demo-Python/                # Python gRPC 서버
│   ├── src/
│   │   ├── grpc_server.py      # gRPC 서버 엔트리포인트
│   │   ├── domain/             # 도메인 모델
│   │   └── infrastructure/     # Chunk Loader
│   └── requirements.txt
│
├── docs/                       # 프로젝트 공통 문서
│   ├── 시스템_아키텍처.md
│   ├── 데이터_플로우.md
│   └── 개발_우선순위.md
│
├── CLAUDE.md                   # AI 개발 가이드
└── README.md                   # 프로젝트 소개 (이 문서)
```

---

## 📚 개발 문서

### 시작하기
- [빠른 시작 가이드](docs/개발_우선순위.md)
- [시스템 아키텍처](docs/시스템_아키텍처.md)
- [데이터 플로우](docs/데이터_플로우.md)

### Backend 개발
- [DB 스키마 가이드](Backend/docs/DB_스키마_가이드.md)
- [테이블 명세서](Backend/docs/table_specification.md) (Single Source of Truth)
- [Spring Batch 개발 가이드](Backend/Batch-Server/docs/Spring_Batch_개발_가이드.md)
- [도메인 확장 가이드](Backend/Batch-Server/docs/도메인_확장_가이드.md)

### Python 개발
- [Python 서버 개발 가이드](Demo-Python/docs/Python_서버_개발_가이드.md)
- [gRPC 통신 가이드](Demo-Python/docs/gRPC_통신_가이드.md)
- [데이터 처리 가이드](Demo-Python/docs/데이터_처리_가이드.md)

### AI 개발자용
- [CLAUDE.md](CLAUDE.md) - AI 에이전트 개발 가이드

---

## 🔄 현재 개발 상태

### ✅ 완료
- **API Server**: GraphQL API 완전 구현 (7개 Query, Dashboard API)
  - Clean Architecture 4-Layer
  - Multi-layer Caching (Caffeine + Redis)
  - 캐시 성능 12.9x 향상
- **Batch Server**: End-to-End 파이프라인 검증 완료
  - Virtual Thread 병렬 쓰기 (33% 성능 향상)
  - 206,334건 처리 (평균 76.8 rps)
- **Demo-Python**: v3 데이터 모델 (1536d OpenAI Embedding)
  - 3개 도메인 전처리 파이프라인
- **Frontend**: 데스크탑 3단 Master-Detail View
  - Redux Toolkit + redux-persist
  - Apollo Client 4.0 + 에러 처리
  - 동적 테마 시스템

### ⏳ 예정
- gRPC 캐시 무효화 연동 (Batch → API)
- Redis L2 캐시 실전 연동
- 통합 테스트 및 성능 최적화

---

## 🤝 기여 방법

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feat/amazing-feature`)
3. Commit your Changes (`git commit -m 'feat: Add some amazing feature'`)
4. Push to the Branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

### 커밋 컨벤션
- `feat:` 새로운 기능 추가
- `fix:` 버그 수정
- `docs:` 문서 변경
- `refactor:` 코드 리팩토링
- `test:` 테스트 코드 추가/수정

---

## 📝 라이선스

이 프로젝트는 개인 학습 목적으로 제작되었습니다.

---

## 👤 작성자

**김태현**

---

## 🙏 감사의 말

이 프로젝트는 대규모 벡터 검색 시스템의 파이프라인을 학습하기 위해 제작되었습니다.

---

**최종 수정일:** 2026-01-14
