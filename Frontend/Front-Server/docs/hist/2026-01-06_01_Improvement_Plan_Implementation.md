# 프론트엔드 개선 계획 구현 및 리팩토링

**날짜**: 2026-01-06
**작성자**: Gemini Pro
**목적**: `Frontend_Improvement_Plan.md`에 기반한 3가지 핵심 기능 구현 및 버그 수정

---

## 📋 작업 요약

`Frontend_Improvement_Plan.md`의 내용과 사용자 요청에 따라, 프론트엔드 애플리케이션의 사용자 경험과 상태 관리 로직을 대폭 개선했습니다.

1.  **`CategoryPieChart` 업데이트 버그 수정**: 검색 실행 시에만 차트가 업데이트되도록 수정
2.  **`SkillSelector` UI 개선**: 기술 스택을 카테고리별로 그룹화하여 표시
3.  **Multiple Back Stacks (History) 구현**: 각 사용자 모드(Candidate/Recruiter)별로 탐색 기록을 관리하는 히스토리 스택 기능 구현

---

## ✅ 완료된 작업 상세 내역

### 1. `CategoryPieChart` 업데이트 로직 수정

#### 문제점
- `SearchResultPanel`의 `CategoryPieChart`가 `selectedSkills`(현재 선택 중인 스킬)를 직접 참조하여, 스킬을 선택/해제할 때마다 실시간으로 차트가 변경되는 원치 않는 동작이 발생했습니다.

#### 해결 방안
1.  **`searchSlice`에 `searchedSkills` 상태 추가**:
    -   `ModeSpecificSearchState` 인터페이스에 `searchedSkills: string[]` 필드를 추가했습니다. 이 상태는 실제 검색에 사용된 스킬 목록을 저장합니다.

2.  **`useSearchMatches` 훅 수정**:
    -   검색 쿼리가 성공적으로 완료되면, `setMatches` 액션과 함께 `setSearchedSkills` 액션을 디스패치하여 검색에 사용된 스킬을 Redux 스토어에 저장하도록 로직을 추가했습니다.

3.  **컴포넌트 Prop 전달 구조 변경**:
    -   `HomePage.client.tsx` → `MainContentPanel.tsx` → `SearchResultPanel.tsx`로 이어지는 props 전달 과정에서, `selectedSkills` 대신 `searchedSkills`를 전달하도록 수정했습니다.
    -   `CategoryPieChart`는 이제 `searchedSkills`를 기반으로 렌더링되므로, 검색이 실행된 후에만 차트 내용이 변경됩니다.

### 2. `SkillSelector` 카테고리별 그룹화

#### 문제점
- 기술 스택 목록(`SkillSelector`)이 단일 목록으로 표시되어, 스킬이 많아질 경우 가독성과 사용성이 저하되었습니다.

#### 해결 방안
1.  **`searchSlice` 상태 구조 변경**:
    -   `skillCategories`의 타입을 `string[]`에서 `SkillCategory[]` (`{ category: string, skills: string[] }`)로 변경했습니다.

2.  **`HomePage.client.tsx` 데이터 주입 로직 수정**:
    -   서버에서 받은 `initialSkillCategories` 데이터를 평탄화하지 않고, 원본 구조 그대로 Redux에 저장하도록 수정했습니다.

3.  **`SkillSelector.tsx` 렌더링 로직 개편**:
    -   `skillCategories` 배열을 순회하며 각 카테고리 제목을 먼저 렌더링합니다.
    -   그 아래에 해당 카테고리에 속한 스킬 목록을 렌더링하도록 UI 로직을 전면 수정했습니다.
    -   검색 필터 또한 이 새로운 중첩 구조에서 올바르게 작동하도록 업데이트했습니다.

### 3. Multiple Back Stacks (History) 기능 구현

#### 문제점
- `docs/Frontend_Improvement_Plan.md`에 명시된 대로, 각 사용자 모드 내에서 뒤로 가기 기능이 없어 사용자 경험이 저하되었습니다.

#### 해결 방안
1.  **`uiSlice` 리팩토링 (History Stack 도입)**:
    -   `ModeSpecificUiState`의 구조를 `pageViewMode`와 `selectedMatchId`를 직접 저장하는 방식에서, `history` 배열과 `currentIndex`를 사용하는 방식으로 변경했습니다.
    -   `history` 배열에는 `{ pageViewMode, selectedMatchId }` 객체가 저장됩니다.

2.  **신규 액션 추가**:
    -   `pushHistory`: 새로운 뷰 상태를 히스토리 스택에 추가하고 `currentIndex`를 업데이트합니다.
    -   `navigateBack`: `currentIndex`를 1 감소시켜 이전 뷰 상태로 돌아갑니다.

3.  **`HomePage.client.tsx` 리팩토링**:
    -   기존의 `setPageViewMode`, `setSelectedMatchId` 액션 호출을 모두 새로운 `pushHistory`, `navigateBack` 액션으로 교체했습니다.
    -   `handleSearch`, `handleMatchSelect`는 `pushHistory`를 호출합니다.
    -   `handleBackToList`는 `navigateBack`을 호출하여 뒤로 가기 기능을 수행합니다.
    -   상태 선택 로직 또한 `history[currentIndex]`에서 현재 뷰를 가져오도록 수정했습니다.

---

## 📊 아키텍처 개선 효과

-   **상태 관리의 예측 가능성 증대**: `searchedSkills` 상태를 분리하여, "언제" 차트가 업데이트되어야 하는지에 대한 규칙을 명확히 했습니다.
-   **사용자 경험(UX) 향상**: 기술 스택을 카테고리별로 그룹화하고, 모드별 탐색 기록을 관리하여 사용자가 더 직관적으로 애플리케이션을 사용할 수 있게 되었습니다.
-   **코드 견고성 강화**: `replace` 도구의 반복적인 실패를 통해, 컴포넌트의 상태와 props를 명확히 분리하고, 더 작은 단위의 원자적 변경을 수행하는 것의 중요성을 확인했습니다.

---

## 📝 수정된 파일 목록

-   `src/services/state/features/search/searchSlice.ts`
-   `src/hooks/useSearchMatches.ts`
-   `src/app/_components/HomePage.client.tsx`
-   `src/components/layout/MainContentPanel.tsx`
-   `src/components/search/SearchResultPanel.tsx`
-   `src/components/input-panel/SkillSelector.tsx`
-   `src/services/state/features/ui/uiSlice.ts`
