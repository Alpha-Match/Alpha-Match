# 프론트엔드 UI 개선: SkillSelector 토글 및 CategoryPieChart 레이블 표시

**날짜**: 2026-01-06
**작성자**: Gemini Pro
**목적**: 사용자 경험 향상을 위해 `SkillSelector`의 카테고리 목록 토글 기능과 `CategoryPieChart`의 그래프 내 레이블 표시 기능을 구현합니다.

---

## 📋 작업 요약

사용자 피드백과 UI/UX 개선 목표에 따라 `SkillSelector`와 `CategoryPieChart` 컴포넌트에 새로운 기능을 추가했습니다.

1.  **`SkillSelector.tsx`**: 기술 스택 카테고리를 토글하여 확장/축소할 수 있는 기능을 추가했습니다.
2.  **`CategoryPieChart.tsx`**: 파이 차트의 각 조각에 해당 카테고리 이름과 백분율을 직접 표시하도록 개선했습니다.

---

## ✅ 완료된 작업 상세 내역

### 1. `SkillSelector.tsx` 카테고리 목록 토글 기능 구현

#### 문제점
-   `SkillSelector`에 기술 스택 카테고리별로 그룹화된 목록이 추가되었지만, 모든 스킬이 항상 표시되어 목록이 길어질 경우 스크롤이 많아지고 가독성이 저하되었습니다.

#### 해결 방안
1.  **토글 상태 관리**: `useState` 훅을 사용하여 `openCategories`라는 로컬 상태를 추가했습니다. 이는 각 카테고리의 열림/닫힘 상태를 `Set<string>` 형태로 저장합니다. 초기에는 모든 카테고리를 열린 상태로 설정했습니다.
2.  **토글 UI 및 핸들러 추가**:
    -   각 카테고리 헤더 (`<h3>`였던 부분)를 `button` 요소로 변경하고 `handleCategoryToggle` 함수를 연결했습니다.
    -   버튼 내부에 `ChevronDown` 아이콘을 추가하고, `isOpen` 상태에 따라 아이콘이 회전하도록 CSS (`rotate-0`, `-rotate-90`)를 적용했습니다.
3.  **조건부 렌더링 및 애니메이션**:
    -   카테고리 내의 스킬 목록을 감싸는 `div`에 `overflow-hidden`, `transition-all`, `duration-300`, `ease-in-out` 클래스를 적용했습니다.
    -   `isOpen` 상태에 따라 `max-h-96 opacity-100` 또는 `max-h-0 opacity-0` 클래스를 동적으로 적용하여 부드러운 확장/축소 애니메이션을 구현했습니다.
4.  **검색 필터 개선**: `filteredCategories` 계산 시 카테고리 이름 자체도 검색되도록 `category.category.toLowerCase().includes(searchTerm.toLowerCase())` 조건을 추가했습니다.

### 2. `CategoryPieChart.tsx` 그래프 내 레이블 표시 기능 구현

#### 문제점
-   `CategoryPieChart`는 각 파이 조각의 백분율만 외부에 표시하고 있어, 사용자가 각 조각이 어떤 카테고리인지 파악하려면 범례(Legend)를 참조해야 했습니다. 이는 직관성이 떨어집니다.

#### 해결 방안
1.  **`CustomPieLabel` 컴포넌트 수정**:
    -   `recharts` 라이브러리가 `label` prop에 전달하는 `payload` 객체에서 `name`(카테고리 이름)을 추출하여 사용하도록 `CustomPieLabel` 함수의 시그니처를 변경했습니다.
    -   레이블 텍스트를 `{카테고리 이름} {백분율}%` 형식으로 변경했습니다.
    -   레이블의 `fill` 색상을 `white`로 설정하여 가독성을 높였습니다.
    -   `textAnchor`와 `dominantBaseline` 속성을 사용하여 텍스트가 파이 조각 내에서 중앙 정렬되도록 했습니다.
    -   **조건부 렌더링**: `percent < 0.05` (5% 미만)인 작은 조각의 경우 레이블을 생략하여 텍스트가 겹치거나 지저분해지는 것을 방지했습니다.
2.  **도넛 차트 형태로 변경**:
    -   `Pie` 컴포넌트의 `innerRadius`를 `60`으로 설정하여 도넛(Donut) 차트 형태로 변경했습니다. 이는 파이 조각 내부에 레이블을 표시할 수 있는 충분한 공간을 확보하기 위함입니다.

---

## 📊 UI/UX 개선 효과

-   **`SkillSelector`**: 길어진 기술 스택 목록을 효율적으로 관리하고, 원하는 카테고리에 쉽게 집중할 수 있게 되어 사용성이 크게 향상되었습니다.
-   **`CategoryPieChart`**: 그래프 자체에서 모든 정보(카테고리 이름 및 백분율)를 직관적으로 파악할 수 있게 되어 데이터 해석의 용이성과 시각적 효율성이 높아졌습니다.

---

## 📝 수정된 파일 목록

-   `src/components/input-panel/SkillSelector.tsx`
-   `src/components/dashboard/CategoryPieChart.tsx`
