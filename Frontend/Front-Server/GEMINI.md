# Front-Server - Claude Instructions

**역할:** GraphQL API 소비 → 헤드헌터-구인공고 매칭 UI 제공
**기술 스택:** Next.js 16 + React 19 + Apollo Client 4 + Redux Toolkit

---

## 📋 문서 목적

- **CLAUDE.md (이 문서)**: AI가 참조할 메타정보 + 코드 위치
- **README.md**: 사람이 읽을 아키텍처/컨벤션 상세 설명

---

## 🗺️ 핵심 문서 경로

### 필수 참조
- **아키텍처 및 컨벤션**: `README.md` (이 디렉토리)
- **상세 기술 문서**: `GEMINI.md` (Gemini AI 작성, 상세 아키텍처)
- **아키텍처 가이드**: `docs/ARCHITECTURE.md`
- **캐싱 전략**: `docs/CACHING_STRATEGY.md`
- **데이터 플로우**: `docs/DATA_FLOW.md`

---

## 📂 구현된 코드 위치 (AI가 읽어야 할 경로)

### 🚀 엔트리포인트 (App Router)

- `src/app/layout.tsx` - 루트 레이아웃 (Provider 설정)
- `src/app/page.tsx` - 메인 페이지 (검색 UI)
- `src/app/globals.css` - 전역 CSS (Tailwind)

### ⚙️ Configuration

**Apollo Client:**
- `src/lib/apollo-client.ts` - Apollo Client 설정 (GraphQL 엔드포인트)
- `src/lib/apollo-wrapper.tsx` - Apollo Provider Wrapper

**Redux:**
- `src/store/index.ts` - Redux Store 설정
- `src/store/slices/searchSlice.ts` - 검색 필터 상태
- `src/store/slices/notificationSlice.ts` - 알림 상태

### 🎨 Components

**핵심 컴포넌트:**
- `src/components/SearchBar.tsx` - 검색 바
- `src/components/FilterPanel.tsx` - 필터 패널
- `src/components/ResultCard.tsx` - 결과 카드
- `src/components/AppInitializer.tsx` - 앱 초기화 (동적 데이터 로드)

**공통 컴포넌트:**
- `src/components/common/` - 재사용 가능한 UI 컴포넌트

### 📡 GraphQL

**쿼리:**
- `src/graphql/queries/` - GraphQL 쿼리 정의

**타입:**
- `src/graphql/types/` - GraphQL 타입 정의 (자동 생성 또는 수동)

### 🔧 Utilities

- `src/utils/` - 헬퍼 함수
- `src/hooks/` - 커스텀 React Hooks

### 📋 Types

- `src/types/index.ts` - TypeScript 타입 정의

### 🎨 Styles

- `src/constants/index.ts` - 상수 (TECH_STACKS 등)
- `tailwind.config.ts` - Tailwind 설정

### 📋 설정 파일

- `package.json` - 의존성
- `next.config.mjs` - Next.js 설정
- `tsconfig.json` - TypeScript 설정

---

## 🚀 현재 구현 상태

### ✅ 완료
- Next.js 16 + App Router 마이그레이션
- Apollo Client 4.0 업그레이드
- Redux Toolkit 상태 관리
- 전역 GraphQL 에러 처리 시스템 (Error Link)
- 동적 TECH_STACKS 연동 (AppInitializer)
- 파일 구조 리팩토링 (types, constants)
- Tailwind CSS 스타일링
- 타입스크립트 컴파일 에러 해결 (React 19, Apollo Client 4.0 타입 호환성, GraphQL 응답 데이터 타입 명시 등)
- 전역 에러 알림 시스템 리팩토링 (Custom Event 기반 디커플링, UX 개선)
- 컴포넌트 구조 리팩토링 (useSearchMatches 훅 분리, InputPanel 하위 컴포넌트 분리 및 파일 구조 계층화, Props Drilling 감소)
- Apollo Client 4.0 패턴 문서화

### 🔄 진행 중
- ⏳ Redux를 이용한 상세 UI 상태 관리 로직 구현 (예: 검색 결과 필터링, UI 상태)
- ⏳ 매칭 결과 시각화 컴포넌트 구현 및 데이터 바인딩
- 🔄 네트워크 에러 토스트 알림 문제 디버깅 (후순위로 진행)

### ⏳ 예정
- GraphQL 쿼리 구현 (API Server 연동)
- 벡터 유사도 시각화
- 
---

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

**최종 수정일:** 2025-12-18
