# 리팩토링: 커스텀 훅 분리를 통한 클린 아키텍처 강화

**날짜**: 2026-01-06
**작성자**: Gemini Pro
**목적**: 컴포넌트의 책임을 분리하고 재사용성을 높이기 위해 `useAppNavigation`과 `useIntersectionObserver` 커스텀 훅을 생성하고 적용합니다.

---

## 📋 작업 요약

기존에 컴포넌트 내부에 복잡하게 얽혀 있던 로직들을 별도의 커스텀 훅으로 분리하여, 단일 책임 원칙과 클린 아키텍처를 강화하는 리팩토링을 진행했습니다.

1.  **네비게이션 로직 분리**: `HomePage.client.tsx`에 있던 Redux 기반의 페이지 이동 로직을 `useAppNavigation` 훅으로 분리했습니다.
2.  **무한 스크롤 로직 분리**: `SearchResultPanel.tsx`에 있던 `IntersectionObserver` 관련 로직을 재사용 가능한 `useIntersectionObserver` 훅으로 분리했습니다.

---

## ✅ 완료된 작업 상세 내역

### 1. `useAppNavigation` 훅 생성 및 적용

#### 문제점
-   `HomePage.client.tsx` 컴포넌트가 Redux 상태를 직접 조회하고, 여러 이벤트 핸들러(`handleSearch`, `handleMatchSelect` 등)에서 네비게이션 관련 액션을 직접 디스패치하는 등 너무 많은 책임을 가지고 있었습니다.

#### 해결 방안
1.  **`useAppNavigation` 훅 생성**:
    -   **파일**: `src/hooks/useAppNavigation.ts`
    -   **역할**: `uiSlice`의 `history` 상태를 구독하고, `navigateToDetail`, `goBack` 등과 같이 의미가 명확한 네비게이션 함수를 제공합니다. 모든 Redux 관련 로직은 이 훅 내부에 캡슐화됩니다.

2.  **`HomePage.client.tsx` 리팩토링**:
    -   기존의 `useSelector`와 `useDispatch`를 사용한 복잡한 네비게이션 코드들을 모두 제거했습니다.
    -   대신 `const { pageViewMode, navigateToDetail, goBack, ... } = useAppNavigation();`과 같이 훅을 호출하여 상태와 함수를 간결하게 가져와 사용하도록 수정했습니다.
    -   이를 통해 `HomePage.client.tsx`는 네비게이션의 '방법'이 아닌 '의도'만 표현하게 되어 코드가 훨씬 깔끔해지고 유지보수가 용이해졌습니다.

### 2. `useIntersectionObserver` 훅 생성 및 적용

#### 문제점
-   `SearchResultPanel.tsx` 컴포넌트 내에 `IntersectionObserver`를 설정하고 관리하는 `useEffect` 로직이 직접 포함되어 있어, 컴포넌트가 UI 렌더링 외에 부가적인 책임을 가지고 있었습니다. 또한 이 로직은 다른 곳에서 재사용하기 어려웠습니다.

#### 해결 방안
1.  **`useIntersectionObserver` 훅 생성**:
    -   **파일**: `src/hooks/useIntersectionObserver.ts`
    -   **역할**: `IntersectionObserver`의 생성, 관찰, 해제 로직을 모두 캡슐화합니다. 콜백 함수와 옵션을 인자로 받아, 관찰할 대상에 부착할 `ref`를 반환합니다.

2.  **`SearchResultPanel.tsx` 리팩토링**:
    -   컴포넌트 내의 복잡한 `useEffect`와 `useRef` 로직을 제거했습니다.
    -   `const sentinelRef = useIntersectionObserver(loadMore, { ... });` 와 같이 단 한 줄의 코드로 무한 스크롤 로직을 적용하도록 수정했습니다.
    -   이를 통해 `SearchResultPanel`은 데이터 목록을 보여주는 순수 UI 컴포넌트로서의 책임에 더 집중할 수 있게 되었습니다.

---

## 📊 아키텍처 개선 효과

-   **단일 책임 원칙 (SRP) 강화**: 각 컴포넌트와 훅이 하나의 명확한 책임만 갖게 되었습니다.
    -   `HomePage.client`: 앱의 최상위 상태 조율
    -   `useAppNavigation`: 네비게이션 상태 및 액션 관리
    -   `SearchResultPanel`: 검색 결과 목록 표시
    -   `useIntersectionObserver`: 요소 가시성 감지
-   **재사용성 증가**: `useIntersectionObserver` 훅은 향후 이미지 지연 로딩 등 다른 기능에도 쉽게 재사용할 수 있습니다.
-   **가독성 및 유지보수성 향상**: 복잡한 로직이 추상화되어 컴포넌트 코드가 더 간결하고 이해하기 쉬워졌습니다.

---

## 📝 수정된 파일 목록

-   **✨ 신규 생성**
    -   `src/hooks/useAppNavigation.ts`
    -   `src/hooks/useIntersectionObserver.ts`
-   **✏️ 수정 (리팩토링)**
    -   `src/app/_components/HomePage.client.tsx`
    -   `src/components/search/SearchResultPanel.tsx`
