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
- `src/app/page.tsx` - 메인 페이지 (전체 뷰 통합)

### ⚙️ Configuration

- `src/lib/apollo-client.ts` - Apollo Client 설정
- `src/store/` - Redux 스토어 및 슬라이스
  - `features/ui/uiSlice.ts` - UI 상태 (테마, `userMode`, 툴팁, 뷰 모드 등)
  - `features/search/searchSlice.ts` - 검색 조건 상태
  - `features/notification/notificationSlice.ts` - 전역 알림 상태

### 🎨 Components (기능/화면 단위)

- `src/components/common/` - **범용 컴포넌트** (BaseTooltip, SkillIcon, Notification)
- `src/components/dashboard/` - **대시보드** (DefaultDashboard, GenericTreemap, CategoryPieChart)
- `src/components/input-panel/` - **검색 입력 패널** (InputPanel, SkillSelector 등)
- `src/components/layout/` - **전역 레이아웃** (Header, AppInitializer, ThemeManager)
- `src/components/search/` - **검색 결과** (SearchResultPanel, ResultCard, MatchDetailPanel)

### 📡 GraphQL & Hooks

- `src/graphql/queries/` - GraphQL 쿼리
- `src/hooks/` - 커스텀 React Hooks (e.g., `useSearchMatches`)

---

## 🚀 현재 구현 상태

### ✅ 완료
- **컴포넌트 아키텍처 리팩토링 (2025-12-23)**:
  - `components` 디렉토리를 `common`, `layout`, `dashboard`, `search`, `input-panel` 등 기능/화면 단위 구조로 재편성.
  - `SkillTooltip` -> `BaseTooltip`, `SkillTreemap` -> `GenericTreemap`으로 일반화하여 재사용성 극대화.
- **동적 테마 시스템 적용 (2025-12-23)**:
  - `appConstants.ts`의 테마 색상을 기반으로 UI 전반의 하드코딩된 색상을 동적으로 변경.
  - `Header`, `InputPanel`, `SearchButton`, `ResultCard` 등 주요 컴포넌트에 테마 색상 적용 완료.
- **Redux 상태 관리 개선 (2025-12-23)**:
  - `userMode` 상태를 `searchSlice`에서 `uiSlice`로 이전하여 관심사 분리.
  - `page.tsx`의 뷰 상태(`viewMode`, `selectedMatch`)를 Redux(`uiSlice`)에서 전역으로 관리하도록 변경.
  - 툴팁의 `visible` 상태를 Redux(`activeTooltipId`)로 제어하여 동시 활성화 문제 해결.
- Next.js 16 + App Router 마이그레이션
- Apollo Client 4.0 업그레이드 및 최신 패턴 적용
- 전역 GraphQL 에러 처리 시스템 (Error Link + Custom Event)
- 동적 TECH_STACKS 연동 (AppInitializer)

### 🔄 진행 중
- 없음. 모든 요청된 리팩토링 완료.

### ⏳ 예정
- GraphQL 쿼리 구현 (현재는 Mock 데이터 사용)
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

**최종 수정일:** 2025-12-22
