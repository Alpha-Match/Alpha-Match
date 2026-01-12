# Frontend 개선 계획: 데스크탑 UI 재구성 및 UX 향상

**날짜**: 2026-01-11
**작성자**: Claude Sonnet 4.5
**목적**: 데스크탑 환경에서의 검색 UI를 재구성하고, 사용자 피드백을 반영하여 전반적인 UX를 개선합니다.

---

## 📋 작업 요약

사용자의 피드백을 바탕으로 데스크탑 환경에서의 검색 결과 화면 구성을 재조정하고, 대시보드 접근성을 향상시키며, 차트의 시각적 일관성을 확보하는 작업을 진행합니다.

---

## ✅ 개선 계획 상세 내역

### 1. 데스크탑 레이아웃 재구성: 3단 Master-Detail View

현재 데스크탑 검색 결과 화면이 너무 많은 정보를 한 번에 보여주어 압박감을 준다는 점을 해결하고, `img_6.png`에서 제시된 구조를 기반으로 다음과 같이 **3단 레이아웃**으로 재구성합니다.

#### 변경 후 구조 (`pageViewMode`가 'dashboard'가 아닐 때)

-   **좌측 (1단, `w-[380px]`): 검색 조건 입력 패널**
    -   `InputPanel` (항상 표시되어 검색 조건 변경 가능)
-   **중앙 (2단, `w-[450px]`): 검색 결과 분석 패널**
    -   `SearchResultAnalysisPanel` (검색된 스킬에 대한 통계, 차트 등 분석 정보 표시)
-   **우측 (3단, `flex-1`): 결과 목록 및 상세 정보 영역**
    -   **초기 상태:** `SearchResultPanel` (검색 결과 리스트만 표시)
    -   **항목 클릭 시:** `MatchDetailPanel` (선택된 항목의 상세 정보 표시)
    -   `MatchDetailPanel` 내의 '뒤로가기' 버튼을 클릭하면 다시 `SearchResultPanel` (목록)로 돌아갑니다.

#### 기대 효과
-   각 패널의 책임이 명확해지고, 정보의 밀도를 적절히 분배하여 화면의 압박감을 해소합니다.
-   사용자가 검색 조건 입력, 분석 결과 확인, 목록 탐색, 상세 정보 확인이라는 흐름을 자연스럽게 따라갈 수 있도록 돕습니다.

### 2. Header에 전역 '대시보드로 돌아가기' 버튼 추가

#### 문제점
-   데스크탑 모드에서 `InputPanel`이나 검색 결과 화면에서 초기 대시보드 화면으로 돌아가는 명확한 버튼이 부재합니다.

#### 해결 방안
-   **상단 헤더(Header)에 '🏠 대시보드' 버튼 추가:**
    -   `Header` 컴포넌트에 `onNavigateToDashboard` 콜백 함수와 `showDashboardButton` 플래그를 prop으로 전달합니다.
    -   `Header` 내부에서는 `showDashboardButton`이 `true`일 때 이 버튼을 렌더링하고, 클릭 시 `onNavigateToDashboard`를 호출하여 `MainDashboard` 화면으로 전환합니다.
-   이 버튼은 `pageViewMode`가 'dashboard'가 아닐 때 항상 표시되어, 사용자가 어떤 화면에 있든 한 번의 클릭으로 초기 대시보드로 돌아갈 수 있도록 접근성을 높입니다.

### 3. `TwoLevelPieChart` 색상 일관성 확보

#### 문제점
-   `TwoLevelPieChart.tsx`에서 바깥쪽 링(하위 기술 스택)의 색상이 안쪽 링(카테고리)의 색상보다 밝게 처리되어 시각적 일관성이 저해됩니다.

#### 해결 방안
-   `TwoLevelPieChart.tsx` 컴포넌트 내 `skillColor` 계산 로직에서 `chroma(...).brighten(0.8)` 부분을 제거하여, 하위 기술 스택도 해당 카테고리와 동일한 색상을 사용하도록 수정합니다.
-   이를 통해 차트 내에서 카테고리와 하위 스킬 간의 시각적 연결성이 강화되고, 전체적인 테마 일관성이 향상됩니다.

---

## 📝 수정될 파일 목록

-   `Frontend/Front-Server/src/app/_components/HomePage.client.tsx`
-   `Frontend/Front-Server/src/components/layout/Header.tsx`
-   `Frontend/Front-Server/src/components/search/SearchResultPanel.tsx`
-   `Frontend/Front-Server/src/components/search/SearchResultAnalysisPanel.tsx` (기존 생성됨)
-   `Frontend/Front-Server/src/components/common/TwoLevelPieChart.tsx`

---

**작업 완료일**: 2026-01-11
**작성자**: Claude Sonnet 4.5
**테스트 상태**: (변경 후 수동 테스트 필요)
