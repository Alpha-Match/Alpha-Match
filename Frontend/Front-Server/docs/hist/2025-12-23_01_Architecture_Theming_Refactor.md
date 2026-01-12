# 작업 요약: Frontend 아키텍처 리팩토링 및 동적 테마 적용

- **날짜**: 2025-12-23

## 1. 개요

오늘 세션에서는 프론트엔드 애플리케이션의 유지보수성, 확장성, 일관성을 대폭 향상시키기 위한 광범위한 리팩토링을 수행했습니다. 주요 작업은 **컴포넌트 아키텍처 재설계**, **Redux 상태 관리 개선**, 그리고 **동적 테마 시스템 적용**의 세 가지 축으로 진행되었습니다.

## 2. 주요 작업 내용

### 가. 컴포넌트 아키텍처 재설계

- **관심사 분리에 따른 디렉토리 구조 재편성**:
    - `src/components` 하위에 다음과 같은 기능별 디렉토리를 생성하여 모든 컴포넌트를 재배치했습니다.
        - `common/`: `BaseTooltip`, `SkillIcon` 등 앱 전반에서 사용되는 범용 컴포넌트.
        - `layout/`: `Header`, `ThemeManager` 등 페이지 레이아웃을 구성하는 컴포넌트.
        - `dashboard/`: `MainDashboard`, `GenericTreemap`, `SearchedSkillsCategoryDistributionChart` 등 대시보드 화면 관련 컴포넌트.
        - `search/`: `SearchResultPanel`, `ResultList`, `MatchDetailPanel` 등 검색 결과 관련 컴포넌트.
- **컴포넌트 일반화 (Generalization)**:
    - `SkillTooltip` -> `common/BaseTooltip`: `Skill`이라는 특정 도메인에서 분리하여, `icon`, `title`, `value`를 props로 받는 범용 툴팁으로 재작성했습니다.
    - `SkillTreemap` -> `dashboard/GenericTreemap`: `renderCellContent`와 `renderTooltipContent`라는 렌더 프롭(render props)을 사용하여, 데이터 시각화 로직과 콘텐츠 렌더링 로직을 완전히 분리했습니다. 이를 통해 어떤 데이터 구조든 유연하게 렌더링할 수 있는 재사용성 높은 컴포넌트가 되었습니다.

### 나. Redux 상태 관리 개선

- **UI 상태 중앙화**:
    - `searchSlice`에 혼재되어 있던 `userMode` 상태를 `uiSlice`로 이전하여, UI 상태와 검색 데이터 상태의 책임을 명확히 분리했습니다.
    - `page.tsx`에서 `useState`로 관리되던 페이지 뷰 모드(`viewMode`)와 선택된 아이템(`selectedMatch`) 상태를 `uiSlice`에 `pageViewMode`, `selectedMatchId`로 추가하여 전역 상태로 승격시켰습니다.
- **액션 책임 분리**:
    - `Header.tsx`에서 탭 변경 시, 단일 액션(`setActiveTab`) 대신 `setUserMode`(uiSlice)와 `resetSearch`(searchSlice) 두 개의 액션을 명시적으로 디스패치하도록 수정하여, 각 Slice가 자신의 상태에만 책임을 지도록 구조를 개선했습니다.

### 다. 동적 테마 시스템 적용

- **하드코딩된 색상 제거**:
    - `Header`, `InputPanel`, `ResultList` 등 애플리케이션 전반에 걸쳐 하드코딩되어 있던 색상 관련 Tailwind 클래스들을 모두 제거했습니다.
- **테마 상수 기반 동적 스타일링**:
    - `index.ts`에 정의된 `CANDIDATE_THEME_COLORS`와 `RECRUITER_THEME_COLORS`를 기반으로 동적으로 색상을 적용하도록 리팩토링했습니다.
    - Tailwind 클래스로 동적 색상 주입이 어려운 경우, 인라인 `style` 속성과 `chroma.js`를 활용하여 그라데이션, 투명도, 그림자 색상까지 테마에 맞게 동적으로 생성하도록 구현했습니다.
    - 특히, `SearchButton.tsx`에서는 사용자의 피드백을 반영하여, 기본 테마색의 색조(hue)를 변경하는 방식으로 다색상 그라데이션 효과를 복원했습니다.

## 3. 결론

이번 대규모 리팩토링을 통해 프론트엔드 프로젝트는 **SOLID 원칙**에 더 가까운, 유연하고 확장 가능한 아키텍처를 갖추게 되었습니다. 컴포넌트의 재사용성이 극대화되었고, 상태 관리는 더 예측 가능해졌으며, 디자인 시스템은 중앙의 테마 상수를 통해 일관성 있게 제어됩니다. 이는 향후 새로운 기능을 추가하거나 기존 기능을 유지보수할 때의 비용을 크게 절감시킬 것입니다.
