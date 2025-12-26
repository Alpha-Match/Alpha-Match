# Front-Server - Claude Instructions

**역할:** GraphQL API 소비 → 헤드헌터-구인공고 매칭 UI 제공
**기술 스택:** Next.js 16 + React 19 + Apollo Client 4 + Redux Toolkit

---

## 📋 문서 목적

- **GEMINI.md (이 문서)**: AI가 참조할 메타정보 + 코드 위치
- **README.md**: 사람이 읽을 아키텍처/컨벤션 상세 설명

---

## 🗺️ 핵심 문서 경로

## 🗺️ 핵심 문서 경로
- **아키텍처 및 컨벤션**: `README.md` (이 디렉토리)
- **아키텍처 가이드**: `docs/ARCHITECTURE.md`
- **캐싱 전략**: `docs/CACHING_STRATEGY.md`
- **Apollo Client 패턴**: `docs/APOLLO_CLIENT_PATTERNS.md`

---

## 📂 구현된 코드 위치 (AI가 읽어야 할 경로)

### 🚀 엔트리포인트 (App Router)

- `src/app/layout.tsx` - 루트 레이아웃
- `src/app/page.tsx` - 메인 페이지

### ⚙️ Configuration

- `src/services/api/apollo-client.ts` - Apollo Client 설정
- `src/services/state/` - Redux 스토어 및 슬라이스
  - `src/services/state/store.ts` - Redux Store
  - `src/services/state/hooks.ts` - Custom Redux Hooks
  - `src/services/state/features/ui/uiSlice.ts` - UI 상태 슬라이스
  - `src/services/state/features/search/searchSlice.ts` - 검색 조건 슬라이스
  - `src/services/state/features/notification/notificationSlice.ts` - 전역 알림 슬라이스

### 🎨 Components (기능/화면 단위)

- `src/components/common/` - 범용 컴포넌트
- `src/components/dashboard/` - 대시보드
- `src/components/input-panel/` - 검색 입력 패널
- `src/components/layout/` - 전역 레이아웃
- `src/components/search/` - 검색 결과

### 📡 GraphQL & Hooks

- `src/graphql/queries/` - GraphQL 쿼리
- `src/hooks/` - 커스텀 React Hooks (e.g., `useSearchMatches`)

---

## 🚀 현재 구현 상태

### 🔄 진행 중
- 없음.

### ⏳ 예정
- GraphQL 쿼리 구현 (API Server 연동)
- 벡터 유사도 시각화 상세 구현
- 단위/E2E 테스트 코드 작성

## ⚠️ AI가 반드시 알아야 할 규칙

### 1. 코드 컨벤션 참조
**상세 컨벤션은 README.md와 GEMINI.md 참조!** AI는 코드 작성 전에:
1. `README.md` 또는 `GEMINI.md` 읽기 (아키텍처 패턴 이해)
2. 기존 컴포넌트 읽기 (위 경로 참조)
3. 같은 패턴으로 구현

### 2. Next.js App Router 패턴
- `src/app/` - 페이지 및 레이아웃
- Server Component vs Client Component 구분
- `'use client'` 지시어 사용 시점 명확히

### 3. 상태 관리 분리
- **서버 상태**: Apollo Client (GraphQL 캐시)
- **클라이언트 상태**: Redux Toolkit (UI 상태, 필터 등)

### 4. 타입 안정성
- 모든 컴포넌트에 Props 타입 정의
- GraphQL 응답 타입 정의

### 5. 스타일링
- Tailwind CSS 유틸리티 우선
- 커스텀 CSS는 `globals.css`에 최소화

### 6. 에러 처리
- Apollo Error Link로 전역 에러 처리
- Redux notificationSlice로 사용자 알림

---

**최종 수정일:** 2025-12-26