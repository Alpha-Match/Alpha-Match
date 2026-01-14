# `SearchedSkillsCategoryDistributionChart` 범례 테마 색상 수정

**날짜**: 2026-01-06
**작성자**: Gemini Pro
**목적**: `SearchedSkillsCategoryDistributionChart`의 범례(Legend) 텍스트 색상이 다크/라이트 모드에 따라 올바르게 변경되도록 수정합니다.

---

## 📋 작업 요약

`SearchedSkillsCategoryDistributionChart`의 범례 텍스트 색상이 테마 관리자(`ThemeManager.tsx`)에 의해 동적으로 결정되는 `--color-text-secondary` CSS 변수를 따르지 않고 항상 어둡게 표시되는 문제가 있었습니다. `Recharts`의 `Legend` 컴포넌트 스타일링 방식을 조정하여 이 문제를 해결했습니다.

---

## ✅ 완료된 작업 상세 내역

### 1. `SearchedSkillsCategoryDistributionChart.tsx` 범례 텍스트 색상 수정

#### 문제점
-   `Legend` 컴포넌트의 `wrapperStyle={{ fill: 'var(--color-text-secondary)' }}` 설정이 범례 텍스트(SVG `<text>` 요소)의 `fill` 속성에 올바르게 적용되지 않았습니다. 이는 `wrapperStyle`이 범례를 감싸는 상위 HTML 요소에 적용되고, SVG 텍스트 요소에는 직접적으로 영향을 주지 않았기 때문입니다. 그 결과, 범례 텍스트가 테마와 관계없이 항상 어둡게 표시되어 가독성이 떨어졌습니다.

#### 해결 방안
-   `Recharts` `Legend` 컴포넌트의 `itemStyle` prop을 사용하여 각 범례 항목의 텍스트 색상을 직접 설정하도록 변경했습니다.
-   `<Legend itemStyle={{ color: 'var(--color-text-secondary)' }}/>`와 같이 `itemStyle`에 `color` 속성을 `var(--color-text-secondary)`로 지정했습니다. `Recharts`는 `itemStyle`의 `color` 속성을 내부적으로 SVG `fill` 속성으로 변환하여 텍스트 요소에 적용합니다.

---

## 📊 UI/UX 개선 효과

-   **향상된 가독성**: 범례 텍스트 색상이 테마(다크/라이트 모드)에 따라 동적으로 변경되어 어떤 테마에서도 명확하게 읽을 수 있게 되었습니다.
-   **일관된 테마 적용**: 애플리케이션 전체의 테마 시스템이 범례 텍스트에도 올바르게 적용되어 UI의 일관성이 향상되었습니다.

---

## 📝 수정된 파일 목록

-   `src/components/dashboard/SearchedSkillsCategoryDistributionChart.tsx`
