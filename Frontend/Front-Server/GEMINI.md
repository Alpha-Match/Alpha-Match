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

- `src/app/layout.tsx` - 루트 레이아웃 (Server Component)
- `src/app/page.tsx` - 메인 페이지 (Server Component, async)
- `src/app/_components/HomePage.client.tsx` - 홈페이지 클라이언트 컴포넌트

### ⚙️ Configuration

- `src/lib/server/api.ts` - 서버 사이드 API 함수 (Server Components용)
- `src/services/api/apollo-client.ts` - Apollo Client 설정 (클라이언트 전용)
- `src/services/state/` - Redux 스토어 및 슬라이스
  - `src/services/state/store.ts` - Redux Store
  - `src/services/state/hooks.ts` - Custom Redux Hooks
  - `src/services/state/features/ui/uiSlice.ts` - UI 상태 슬라이스 (도메인별 분리: CANDIDATE/RECRUITER)
  - `src/services/state/features/search/searchSlice.ts` - 검색 조건 슬라이스 (도메인별 분리)
  - `src/services/state/features/notification/notificationSlice.ts` - 전역 알림 슬라이스

### 🎨 Components (기능/화면 단위)

- `src/components/common/` - 범용 컴포넌트
- `src/components/dashboard/` - 대시보드
- `src/components/input-panel/` - 검색 입력 패널
- `src/components/layout/` - 전역 레이아웃
- `src/components/search/` - 검색 결과

### 📡 GraphQL & Hooks

- `src/services/api/queries/` - GraphQL 쿼리 정의
- `src/hooks/` - 커스텀 React Hooks
  - `useSearchMatches` - 검색 실행 및 Redux ViewModel 연동
  - `useMatchDetail` - 상세 정보 조회 (도메인별 분리)

---

## ⚠️ AI가 반드시 알아야 할 규칙

### 1. 코드 컨벤션 참조
**상세 컨벤션은 README.md와 GEMINI.md 참조!** AI는 코드 작성 전에:
1. `README.md` 또는 `GEMINI.md` 읽기 (아키텍처 패턴 이해)
2. 기존 컴포넌트 읽기 (위 경로 참조)
3. 같은 패턴으로 구현

### 2. Next.js App Router 패턴
- `src/app/` - 페이지 및 레이아웃
- **Server Component vs Client Component 구분:**
  - Server Component: 기본값, 서버에서만 실행, async 가능, 초기 데이터 fetch에 활용
  - Client Component: `'use client'` 명시, useState/useEffect/Redux/Event Handler 사용
  - 패턴: Server Component에서 데이터 fetch → Client Component에 props 전달
- `lib/server/` - Server Components 전용 API 함수 (클라이언트 번들에 포함되지 않음)

### 3. 상태 관리 분리 (ViewModel 패턴)

본 프로젝트는 **3-Layer 상태 관리**를 통해 ViewModel 패턴을 구현합니다:

```
┌─────────────────────────────────────┐
│  View Layer (React Components)      │
└───────────────┬─────────────────────┘
                │
┌───────────────▼─────────────────────┐
│  ViewModel Layer (Redux Toolkit)    │ ← UI 상태 + 검색 결과 캐시
│  - searchSlice: {                   │
│      CANDIDATE: {                   │
│        selectedSkills,              │
│        selectedExperience,          │
│        matches ← 영구 보존          │
│      },                             │
│      RECRUITER: { ... }             │
│    }                                │
│  - uiSlice: pageViewMode 등         │
└───────────────┬─────────────────────┘
                │
┌───────────────▼─────────────────────┐
│  Data Layer (Apollo Client)         │ ← 네트워크 캐시
│  InMemoryCache (GraphQL)            │
└─────────────────────────────────────┘
```

**핵심 원칙:**
- **Apollo Client**: GraphQL API 통신 및 네트워크 레벨 캐시 (InMemoryCache)
- **Redux Toolkit**: ViewModel - 도메인별 UI 상태 및 검색 결과 영구 저장
  - `searchSlice.matches`: 검색 결과를 Redux에 저장하여 모드 전환 시에도 보존
  - `uiSlice`: 도메인별 pageViewMode, selectedMatchId 저장
- **Multiple Back Stacks**: 각 UserMode(CANDIDATE/RECRUITER)가 독립적인 상태 스택 유지

**주의사항:**
- Hook의 useState로 matches를 관리하지 말 것 (컴포넌트 재렌더링 시 손실)
- 반드시 `dispatch(setMatches({ userMode, matches }))`로 Redux에 저장
- 뒤로가기 시 Redux 캐시를 먼저 확인: `matches.length === 0` 체크 후 API 호출

### 4. 타입 안정성
- 모든 컴포넌트에 Props 타입 정의
- GraphQL 응답 타입 정의

### 5. 스타일링
- Tailwind CSS 유틸리티 우선
- **중앙 집중형 테마 시스템:**
  - `tailwind.config.ts`에 시맨틱 CSS 변수(예: `background`, `panel-main`, `text-primary`)를 정의하여 컬러 팔레트를 관리합니다.
  - `globals.css`에서 라이트/다크 모드 및 `userMode` (CANDIDATE/RECRUITER)에 따른 이러한 CSS 변수의 실제 값을 정의합니다.
  - 컴포넌트에서는 `bg-panel-main`, `text-text-secondary`, `border-border`와 같은 시맨틱 클래스를 사용하여 테마 변경에 자동으로 반응하도록 합니다.
- **커스텀 스크롤바:** `globals.css`에 정의된 `custom-scrollbar` 클래스를 통해 테마에 맞는 스크롤바를 제공하며, 필요한 스크롤 영역에 적용합니다.

### 6. 에러 처리
- Apollo Error Link로 전역 에러 처리 (`APOLLO_CLIENT_PATTERNS.md` 참조)
- Redux notificationSlice로 사용자 알림
- 컴포넌트 레벨 에러 처리: QueryBoundary 활용

### 7. 트러블슈팅
- **ViewModel & Multiple Back Stacks**: `docs/troubleshooting/ViewModel_Multiple_Back_Stacks.md`
  - Redux useState 사용 시 주의사항
  - 모드 전환 시 상태 손실 문제 해결
  - useEffect 의존성 배열 최적화

---

## 📚 추가 참고 문서

- **히스토리**: `docs/hist/` - 주요 변경 이력 (읽기 전용)
  - `2025-12-30_Server_Components_Migration.md` - Server Components 아키텍처 구축
  - `2025-12-30_ViewModel_Multiple_Back_Stacks.md` - ViewModel 패턴 및 Multiple Back Stacks 구현
- **개선 계획**: `docs/Frontend_Improvement_Plan.md` - 향후 개선 로드맵

---

**최종 수정일:** 2026-01-05
**주요 업데이트:**
- Dashboard 분석 컴포넌트 (CategoryPieChart, SkillCompetencyBadge)
- 무한 스크롤 UX 개선 (NetworkStatus 기반 로딩 구분, Throttle)
- 기술 스택 정렬 (캐시 일관성 향상)
- Server/Client Component 분리 (HomePage.client.tsx)
- 검색 UX 개선 (자동 검색 방지, 캐시 활용)