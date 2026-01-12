# `SkillSelector` 스크롤 관리 및 `SearchedSkillsCategoryDistributionChart` 테마 색상 확인

**날짜**: 2026-01-06
**작성자**: Gemini Pro
**목적**: `SkillSelector.tsx`의 스크롤바 관리를 부모 컴포넌트로 위임하고, `SearchedSkillsCategoryDistributionChart.tsx`의 레이블 폰트 색상이 테마 시스템을 따르는지 확인합니다.

---

## 📋 작업 요약

사용자 피드백에 따라 `SkillSelector` 컴포넌트의 스크롤바 관리 방식을 개선하고, `SearchedSkillsCategoryDistributionChart`의 레이블 색상 결정 방식에 대한 명확한 설명을 추가했습니다.

1.  **`SkillSelector.tsx`**: 컴포넌트 내부 스크롤바를 제거하고 부모 `InputPanel`이 스크롤을 담당하도록 변경했습니다.
2.  **`SearchedSkillsCategoryDistributionChart.tsx`**: `CustomPieLabel`의 폰트 색상이 `ThemeManager`에 의해 동적으로 결정되는 CSS 변수를 사용하고 있음을 명시하는 주석을 추가했습니다.

---

## ✅ 완료된 작업 상세 내역

### 1. `SkillSelector.tsx` 스크롤바 관리 위임

#### 문제점
-   `SkillSelector` 컴포넌트 내부에 고정 높이(`h-[500px]`)와 독립적인 스크롤바(`overflow-y-auto custom-scrollbar`)가 존재했습니다. 이는 부모 `InputPanel`이 전체 영역에 대한 스크롤바를 이미 가지고 있어 이중 스크롤바가 발생하거나 UI의 일관성을 해칠 수 있습니다.

#### 해결 방안
-   `SkillSelector` 내의 스킬 목록을 감싸는 `div`에서 `h-[500px]`, `overflow-y-auto`, `custom-scrollbar` 클래스를 제거했습니다.
-   이제 `SkillSelector` 자체는 스크롤바를 가지지 않으며, 콘텐츠가 길어지면 부모 `InputPanel`의 `overflow-y-auto` 속성에 따라 스크롤이 자연스럽게 발생합니다.

### 2. `SearchedSkillsCategoryDistributionChart.tsx` `CustomPieLabel` 폰트 색상 테마 확인

#### 문제점
-   `SearchedSkillsCategoryDistributionChart`의 `CustomPieLabel` 폰트 색상이 `ThemeManager.tsx`에 의해 결정되는지 여부에 대한 사용자 문의가 있었습니다.

#### 해결 방안
-   `CustomPieLabel` 컴포넌트의 `text` 요소에 사용된 `fill="var(--color-text-primary)"`가 `ThemeManager.tsx`에 의해 `<html>` 태그에 설정되는 테마 클래스(`theme-candidate`, `theme-recruiter`, `dark`)에 따라 `globals.css`에서 정의된 `--color-text-primary` CSS 변수를 참조하고 있음을 명시하는 주석을 추가했습니다.
-   이는 기존 구현이 이미 테마 시스템을 올바르게 따르고 있음을 확인시켜 줍니다. `ThemeManager`는 직접 색상을 설정하는 대신, 전역 CSS 변수의 값을 변경하는 테마 클래스를 제어하는 방식으로 동작합니다.

---

## 📊 UI/UX 개선 효과

-   **일관된 스크롤바 경험**: `InputPanel` 영역 전체에 걸쳐 단일 스크롤바가 관리되어 사용자 경험의 일관성이 향상되었습니다.
-   **명확한 테마 적용**: `CustomPieLabel`의 폰트 색상이 테마 시스템과 연동되어 있음을 명확히 함으로써 코드의 의도를 명확히 하고, 잠재적인 오해를 해소했습니다.

---

## 📝 수정된 파일 목록

-   `src/components/input-panel/SkillSelector.tsx`
-   `src/components/dashboard/SearchedSkillsCategoryDistributionChart.tsx`
