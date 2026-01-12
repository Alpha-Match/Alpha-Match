# Front-Server - Claude Instructions

**역할:** GraphQL API 소비 → 헤드헌터-구인공고 매칭 UI 제공
**기술 스택:** Next.js 16 + React 19 + Apollo Client 4 + Redux Toolkit

---

## 📋 문서 목적

- **GEMINI.md (이 문서)**: AI가 참조할 메타정보 + 코드 위치
- **README.md**: 사람이 읽을 아키텍처/컨벤션 상세 설명

---

## 🗺️ 핵심 문서 경로
- **아키텍처 및 컨벤션**: `README.md` (이 디렉토리)
- **아키텍처 가이드**: `docs/ARCHITECTURE.md`
- **캐싱 전략**: `docs/CACHING_STRATEGY.md`
- **Apollo Client 패턴**: `docs/APOLLO_CLIENT_PATTERNS.md`

---

## 📂 구현된 코드 위치 (AI가 읽어야 할 경로)

### 📡 GraphQL & Hooks

- `src/services/api/queries/` - GraphQL 쿼리 정의
- `src/hooks/` - 커스텀 React Hooks
  - `useSearchMatches` - 검색 실행 및 Redux ViewModel 연동
  - `useMatchDetail` - 상세 정보 조회 (도메인별 분리)
  - `useAppNavigation` - 앱 네비게이션 로직 및 히스토리 관리
  - `useIntersectionObserver` - 요소 가시성 감지 (무한 스크롤 등)
  - `useHydrated` - 클라이언트 하이드레이션 상태 추적

---

## 🎯 UI/UX 아키텍처

### 1. 데스크탑 레이아웃 재구성: 3단 Master-Detail View

사용자 피드백을 반영하여 데스크탑 검색 결과 화면을 **3단 레이아웃**으로 재구성합니다 (`pageViewMode`가 'dashboard'가 아닐 때). 이를 통해 각 패널의 책임이 명확해지고, 정보의 밀도를 적절히 분배하여 화면의 압박감을 해소하며, 사용자가 검색 조건 입력, 분석 결과 확인, 목록 탐색, 상세 정보 확인이라는 흐름을 자연스럽게 따라갈 수 있도록 돕습니다.

-   **좌측 (1단, `w-[380px]`): 검색 조건 입력 패널**
    -   `InputPanel` (항상 표시되어 검색 조건 변경 가능)
-   **중앙 (2단, `w-[450px]`): 검색 결과 분석 패널**
    -   `SearchResultAnalysisPanel` (검색된 스킬에 대한 통계, 차트 등 분석 정보 표시)
-   **우측 (3단, `flex-1`): 결과 목록 및 상세 정보 영역**
    -   **초기 상태:** `SearchResultPanel` (검색 결과 리스트만 표시)
    -   **항목 클릭 시:** `MatchDetailPanel` (선택된 항목의 상세 정보 표시)
        - `MatchDetailPanel` 내의 '뒤로가기' 버튼을 클릭하면 다시 `SearchResultPanel` (목록)로 돌아갑니다.
    
    ### 2. Header에 전역 '대시보드로 돌아가기' 버튼 추가
    
    데스크탑 모드에서 `InputPanel`이나 검색 결과 화면에서 초기 대시보드 화면으로 돌아가는 명확한 버튼을 제공하기 위해 상단 헤더(`Header`) 컴포넌트에 '🏠 대시보드' 버튼을 추가합니다.
    
    -   `Header` 컴포넌트에는 `onNavigateToDashboard` 콜백 함수와 `showDashboardButton` 플래그를 prop으로 전달합니다.
    -   `Header` 내부에서는 `showDashboardButton`이 `true`일 때 이 버튼을 렌더링하고, 클릭 시 `onNavigateToDashboard`를 호출하여 `DefaultDashboard` 화면으로 전환합니다.
    -   이 버튼은 `pageViewMode`가 'dashboard'가 아닐 때 항상 표시되어, 사용자가 어떤 화면에 있든 한 번의 클릭으로 초기 대시보드로 돌아갈 수 있도록 접근성을 높입니다.
    
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
│        searchedSkills, ← 검색된 스킬 │
│        matches ← 영구 보존          │
│      },                             │
│      RECRUITER: { ... }             │
│    }                                │
│  - uiSlice: {                       │
│      CANDIDATE: {                   │
│        history: [{...}, ...],       │
│        currentIndex: number         │
│      }                              │
│    }                                │
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
  - `searchSlice.searchedSkills`: 검색에 실제 사용된 스킬을 저장하여, 의도치 않은 UI 업데이트를 방지 (예: `CategoryPieChart`)
  - `uiSlice.history`: 페이지 뷰(`pageViewMode`, `selectedMatchId`)의 배열을 저장하여, 모드별 탐색 기록(뒤로 가기)을 관리합니다.
- **Multiple Back Stacks**: 각 UserMode(CANDIDATE/RECRUITER)가 독립적인 상태 스택(검색 조건, 검색 결과, 탐색 기록)을 유지합니다.

**데이터 페칭 책임 (Data Fetching Responsibilities):**
- **페이지 레벨 데이터 (Page-level Data):** `HomePage.client.tsx`와 같은 최상위 컨테이너 컴포넌트가 Redux와 상호작용하며 페이지의 핵심 데이터(`matches`, `searchedSkills` 등)를 관리하고 하위 컴포넌트에 props로 전달합니다.
- **컴포넌트 레벨 통계/분석 데이터 (Component-level Stats/Analysis Data):** `SearchResultPanel.tsx` 이나 그 하위의 `TopSkills`, `CategoryPieChart`와 같은 분석 컴포넌트는 **자체적으로 필요한 데이터를 GraphQL 쿼리(`useQuery`)를 통해 직접 가져올 수 있습니다.** 이는 컴포넌트의 독립성과 재사용성을 높이고, props drilling을 방지하는 유효한 패턴입니다. 예를 들어, 검색 결과의 전체 개수(`totalCount`)는 `SearchResultPanel`이 직접 `GET_SEARCH_STATISTICS` 쿼리로 가져옵니다.

**주의사항:**
- Hook의 useState로 matches를 관리하지 말 것 (컴포넌트 재렌더링 시 손실)
- 반드시 `dispatch(setMatches({ userMode, matches }))`로 Redux에 저장
- `pushHistory`와 `navigateBack` 액션을 사용하여 탐색 상태를 변경해야 합니다.

### 4. 타입 안정성
- 모든 컴포넌트에 Props 타입 정의
- GraphQL 응답 타입 정의

### 5. 스타일링
- Tailwind CSS 유틸리티 우선
- **중앙 집중형 테마 시스템:**
  - `tailwind.config.ts`에 시맨틱 CSS 변수(예: `background`, `panel-main`, `text-primary`)를 정의하여 컬러 팔레트를 관리합니다.
  - `globals.css`에서 라이트/다크 모드 및 `userMode` (CANDIDATE/RECRUITER)에 따른 이러한 CSS 변수의 실제 값을 정의합니다.
  - 컴포넌트에서는 `bg-panel-main`, `text-text-secondary`, `border-border`와 같은 시맨틱 클래스를 사용하여 테마 변경에 자동으로 반응하도록 합니다.
- **`TwoLevelPieChart` 색상 일관성 확보**: `TwoLevelPieChart.tsx` 컴포넌트 내 `skillColor` 계산 로직에서 `chroma(...).brighten(0.8)` 부분을 제거하여, 하위 기술 스택도 해당 카테고리와 동일한 색상을 사용하도록 수정합니다. 이를 통해 차트 내에서 카테고리와 하위 스킬 간의 시각적 연결성이 강화되고, 전체적인 테마 일관성이 향상됩니다.
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
- **Hydration 오류**: `docs/troubleshooting/Hydration_Error_and_SSR.md`
  - 서버-클라이언트 렌더링 불일치 문제 해결

---

## 📚 추가 참고 문서

- **히스토리**: `docs/hist/` - 주요 변경 이력 (읽기 전용)
  - `2025-12-30_Server_Components_Migration.md` - Server Components 아키텍처 구축
  - `2025-12-30_ViewModel_Multiple_Back_Stacks.md` - ViewModel 패턴 및 Multiple Back Stacks 구현
  - `2026-01-06_01_Improvement_Plan_Implementation.md` - 개선 계획 구현 (히스토리 스택, UI 개선 등)
  - `2026-01-06_02_SkillSelector_BugFix.md` - `SkillSelector.tsx`의 `undefined` 오류 수정 및 문서 업데이트
  - `2026-01-06_03_Refactor_CustomHooks.md` - 리팩토링: 커스텀 훅 분리를 통한 클린 아키텍처 강화
  - `2026-01-06_04_SkillSelector_PieChart_Improvements.md` - 프론트엔드 UI 개선: SkillSelector 토글 및 CategoryPieChart 레이블 표시
  - `2026-01-06_05_CategoryPieChart_Label_Visibility_Fix.md` - `CategoryPieChart` 레이블 가시성 개선
  - `2026-01-06_06_SkillSelector_DynamicHeight_Sorting_Fix.md` - `SkillSelector.tsx` 동적 높이, 정렬 및 전체 스킬 가시성 개선
- **개선 계획**: `docs/Frontend_Improvement_Plan.md` - 향후 개선 로드맵
- **Apollo Client 및 SSR 데이터 페칭**: `docs/Apollo_Client_and_SSR_Fetching.md`

---

**최종 수정일:** 2026-01-12
**주요 업데이트:**
- **`TwoLevelPieChart` TypeScript 오류 수정 및 레이블 테마 색상 적용**: Legend Payload 타입 오류 수정 및 파이 차트 레이블이 테마에 따라 색상이 변경되도록 개선.
- **데스크탑 UI 재구성 (3단 Master-Detail View)**: 검색 결과 화면을 `InputPanel`, `SearchResultAnalysisPanel`, `SearchResultPanel`/`MatchDetailPanel`로 구성된 3단 레이아웃으로 변경하여 정보 밀도를 분배하고 UX 흐름을 개선.
- **Header에 전역 '대시보드로 돌아가기' 버튼 추가**: 사용자가 어떤 화면에 있든 초기 대시보드로 쉽게 돌아갈 수 있도록 `Header` 컴포넌트에 버튼 추가.
- **`TwoLevelPieChart` 색상 일관성 확보**: `chroma(...).brighten(0.8)` 제거하여 하위 기술 스택의 색상이 카테고리 색상과 동일하게 유지되도록 수정.
- **검색 결과 통계 UX 개선**: `SearchResultPanel`에서 전체 검색 결과 수를 표시하도록 수정. `GET_TOP_SKILLS_IN_SEARCH` 쿼리를 `GET_SEARCH_STATISTICS`로 리팩토링하고 `totalCount`를 포함하도록 백엔드 요구사항 정의.
- **`SkillSelector.tsx` 동적 높이, 정렬 및 전체 스킬 가시성 개선**: 토글 시 스킬 목록 잘림 문제 해결, 카테고리별 정렬, 모든 스킬 선택 가능하도록 개선
- **`CategoryPieChart` 레이블 가시성 개선**: 점유율과 관계없이 모든 카테고리의 레이블이 차트 내에 표시되도록 수정
- **`SkillSelector` 카테고리 목록 토글 기능**: 각 기술 스택 카테고리 목록을 확장/축소할 수 있도록 UI 개선
- **`CategoryPieChart` 그래프 내 레이블 표시**: 파이 차트의 각 조각에 카테고리 이름과 백분율을 직접 표시하여 직관성 향상
- **클린 아키텍처 리팩토링**: `useAppNavigation`과 `useIntersectionObserver` 커스텀 훅을 통해 컴포넌트 책임 분리 및 재사용성 강화
- **`SkillSelector.tsx` 오류 수정**: `category.skills`가 `undefined`일 경우 `filter` 호출 시 발생하는 런타임 오류 해결
- **히스토리 스택 구현**: `uiSlice`를 리팩토링하여 각 사용자 모드별 탐색 기록(뒤로 가기) 관리
- **`CategoryPieChart` 업데이트 로직 수정**: 검색이 실행된 후에만 차트가 업데이트되도록 `searchedSkills` 상태 분리
- **`SkillSelector` UI 개선**: 기술 스택을 카테고리별로 그룹화하여 표시
- **Hydration 오류 수정**: `useHydrated` 훅을 도입하여 서버-클라이언트 렌더링 불일치 문제 해결 및 `DefaultDashboard.tsx` 안정성 강화
- Dashboard 분석 컴포넌트 (CategoryPieChart, SkillCompetencyBadge)
- 무한 스크롤 UX 개선 (NetworkStatus 기반 로딩 구분, Throttle)
- 기술 스택 정렬 (캐시 일관성 향상)
- Server/Client Component 분리 (HomePage.client.tsx)
- 검색 UX 개선 (자동 검색 방지, 캐시 활용)