# 트러블슈팅: React Hooks 규칙 위반 (`useAppSelector` in `useEffect`)

**날짜**: 2025-12-27

## 1. 문제 상황 (Problem Statement)

`src/app/page.tsx` 파일의 `useEffect` 훅 내부에서 `useAppSelector`와 같은 React Hooks를 호출했을 때, 다음과 같은 런타임 에러가 발생했다.

```
Error: You might have mismatching versions of React and the renderer (such as React DOM).
... (omitted stack trace) ...
```
이 에러는 주로 React Hooks의 규칙을 위반했을 때 나타나는 메시지로, 훅 호출 위치가 부적절함을 시사한다. 특히 `useEffect` 내부에서 `const { isInitial } = useAppSelector((state) => state.search[userMode]);` 코드가 문제였다.

## 2. 근본 원인 (Root Cause)

React Hooks는 다음과 같은 엄격한 규칙을 따른다:
- **최상위에서만 훅을 호출해야 합니다**: 훅을 루프, 조건문, 또는 중첩된 함수 내에서 호출해서는 안 됩니다.
- **React 함수 컴포넌트나 커스텀 훅 내에서만 훅을 호출해야 합니다**.

문제 코드(`useAppSelector` 훅 호출)가 `useEffect` 콜백 함수 내부에 위치하면서, React Hooks의 "최상위 호출" 규칙을 위반했다. `useAppSelector`는 결국 `useSelector`를 래핑하는 커스텀 훅이므로, `useEffect` 콜백 내에서 호출될 수 없다. 이는 React가 훅의 상태를 올바르게 추적하지 못하게 하여 예측 불가능한 동작이나 위와 같은 런타임 에러를 발생시킨다.

## 3. 해결책 (Solution)

`useEffect` 내부에서 필요한 상태(`isInitial`)를 가져오기 위해 `useAppSelector`를 호출하는 대신, 컴포넌트의 **최상위 레벨**에서 해당 훅을 호출하여 상태를 미리 가져오고, 그 값을 `useEffect`의 종속성 배열에 추가하여 사용하는 방식으로 수정했다.

### 이전 코드 (`src/app/page.tsx`)

```typescript
// userMode 변경 시 자동으로 검색을 다시 실행하여 이전 상태를 복원
useEffect(() => {
  // ERROR: Hook call inside useEffect callback
  const { isInitial } = useAppSelector((state) => state.search[userMode]); 
  if (pageViewMode === 'results' && selectedSkills.length > 0 && !isInitial) {
    runSearch(userMode, selectedSkills, selectedExperience);
  }
}, [userMode, pageViewMode, selectedSkills, selectedExperience, runSearch]);
```

### 수정된 코드 (`src/app/page.tsx`)

```typescript
// HomePage 컴포넌트의 최상위 레벨 (다른 훅들과 함께 호출)
const { selectedSkills, selectedExperience } = useAppSelector((state) => state.search[userMode]);
const { isInitial } = useAppSelector((state) => state.search[userMode]); // 최상위로 이동

// userMode 변경 시 자동으로 검색을 다시 실행하여 이전 상태를 복원
useEffect(() => {
  // isInitial 값은 이제 useEffect 외부에서 이미 가져온 상태
  if (pageViewMode === 'results' && selectedSkills.length > 0 && !isInitial) {
    runSearch(userMode, selectedSkills, selectedExperience);
  }
  // isInitial을 의존성 배열에 추가
}, [userMode, pageViewMode, selectedSkills, selectedExperience, isInitial, runSearch]); 
```

### 일반적인 가이드 (React 19 & Apollo Client v4 패턴)

이번 문제 해결 과정을 통해 다시 한번 React Hooks의 규칙을 상기할 필요가 있다. React 19 및 Apollo Client v4 환경에서는 다음과 같은 패턴을 활용하여 더욱 선언적이고 안전한 코드를 작성할 수 있다.

-   **React Hooks 규칙 준수**: 훅은 항상 컴포넌트의 최상위 레벨에서 호출되어야 하며, 조건문, 루프, 중첩 함수 내에서는 호출할 수 없다. `useEffect`는 부수 효과를 관리하는 용도이며, 훅 자체를 그 안에서 호출하는 것은 규칙 위반이다.
-   **`useSuspenseQuery` 활용**: Apollo Client v4와 React Suspense를 함께 사용하여 데이터 로딩 상태를 컴포넌트 내부에서 직접 관리하는 대신, 상위 `<Suspense>` 컴포넌트로 위임하여 더욱 깔끔한 로딩 UI를 구현할 수 있다.
-   **`useTransition` 활용**: 사용자 인터랙션(`onClick` 등)으로 인한 상태 변경이 UI 업데이트를 트리거할 때, `useTransition`을 사용하여 해당 업데이트를 "전환(Transition)"으로 표시하고, 지연 로딩이나 비동기 작업을 처리하는 동안 UI가 응답성을 유지하도록 할 수 있다. 이 훅은 `isPending` 상태를 제공하여 로딩 스피너 등의 피드백을 쉽게 구현하게 한다.
-   **`QueryBoundary` 패턴**: 애플리케이션 전반에 걸쳐 일관된 로딩 및 에러 UI를 제공하기 위해 `QueryBoundary`와 같은 범용 컴포넌트를 활용한다. `useSuspenseQuery`와 함께 사용될 때, `QueryBoundary`는 에러 폴백 역할(Error Boundary)을 수행하며, `loading` 상태는 `<Suspense>`가 담당한다.

이러한 패턴들을 명확히 이해하고 적용함으로써, 안정적이고 유지보수하기 쉬운 React 애플리케이션을 구축할 수 있다.
