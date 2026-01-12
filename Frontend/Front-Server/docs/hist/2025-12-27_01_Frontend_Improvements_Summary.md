# 2025-12-27 Frontend 개선 작업 요약

## 1. UserMode별 검색 상태 보존 및 메인 콘텐츠 동기화

`UserMode` (CANDIDATE / RECRUITER) 변경 시 검색 조건(`selectedSkills`, `selectedExperience`)과 검색 결과(`matches`)가 초기화되지 않고 각 모드에 맞춰 유지되도록 로직을 개선했습니다.

### 1.1. `searchSlice.ts` 리팩토링
- `SearchState` 구조를 `UserMode`별로 분리하여 `selectedSkills`, `selectedExperience`, `isInitial`을 각 모드별로 관리하도록 변경했습니다.
- `toggleSkill`, `setExperience`, `setSearchPerformed`, `resetSearch` 액션의 페이로드에 `userMode`를 포함하도록 수정하여, 현재 활성화된 모드의 상태만 조작하도록 했습니다.

### 1.2. `useSearchMatches.ts` 리팩토링
- `matches` 상태를 `UserMode`별로 저장하는 `modeMatches`로 변경했습니다.
- `useAppSelector`를 통해 현재 `userMode`를 가져와 쿼리 실행 시 이전에 저장된 `matches`를 반환하도록 했습니다.
- `useEffect` 내에서 `data` 또는 `error` 발생 시 `lastQueryMode`를 기반으로 해당 모드의 `matches`를 업데이트/초기화하도록 했습니다.

### 1.3. `page.tsx` 및 관련 컴포넌트 업데이트
- `page.tsx`에서 `selectedSkills`, `selectedExperience` 셀렉터를 `useAppSelector((state) => state.search[userMode])` 형태로 수정했습니다.
- `setSearchPerformed` 디스패치 시 `userMode`를 페이로드로 전달하도록 수정했습니다.
- `SkillSelector.tsx`, `SearchButton.tsx`, `ClearButton.tsx`에서도 각 컴포넌트의 `userMode`를 기반으로 `searchSlice`의 상태에 접근하도록 셀렉터와 디스패치 로직을 업데이트했습니다.
- `page.tsx`에 `useEffect`를 추가하여 `userMode` 변경 시 `pageViewMode`가 'results'이고 이전 검색 이력이 있는 경우, 자동으로 `runSearch`를 다시 실행하여 화면이 동기화되도록 했습니다.

## 2. InputPanel 개선

### 2.1. "Clear" 버튼 기능 및 위치 조정
- `InputPanel` 푸터에 있던 "Clear" 버튼을 `SkillSelector` 컴포넌트 내부(Tech Stack 라벨 우측)로 이동하고, `src/components/common/ClearButton.tsx`로 재배치했습니다.
- 이 버튼은 이제 `userMode`에 해당하는 `selectedSkills`만 초기화하고, `resetView()` (대시보드로 돌아가기) 기능은 제외했습니다.

### 2.2. Tech Stack 검색 기능 추가
- `SkillSelector.tsx` 내부에 검색 입력 필드를 추가하여 기술 목록을 검색하고 필터링할 수 있도록 했습니다.

## 3. UI/UX 및 React 19 패턴 개선

### 3.1. `GenericTreemap.tsx` 렌더링 오류 수정
- `foreignObject`의 `width` 및 `height` 계산 시 음수 값이 전달되는 문제를 `Math.max(0, ...)`를 사용하여 해결했습니다.

### 3.2. `Notification.tsx` (토스트 메시지) 너비 및 스타일 수정
- 토스트 메시지가 텍스트 길이에 관계없이 적절한 너비를 가지도록 `max-w-sm` 및 `min-w-[320px]` Tailwind CSS 클래스를 적용했습니다.
- 긴 텍스트가 줄바꿈되도록 `break-words` 클래스를 추가하고, 메시지 컨테이너의 `w-0 flex-1`을 `w-full flex-1`로 변경했습니다.

### 3.3. `QueryBoundary.tsx` 개선 및 버그 수정
- `QueryBoundaryProps`의 `loading` prop을 필수가 아닌 선택 사항(`loading?: boolean`)으로 변경하여 `Suspense` 기반 컴포넌트와 함께 사용할 때 타입 에러를 해결했습니다.
- `import { ApolloError } from ' @apollo/client';`의 잘못된 임포트 경로를 `import { ApolloError } from '@apollo/client';`로 수정했습니다.
- `DefaultErrorComponent`에 `lucide-react`의 `AlertTriangle` 아이콘을 추가하고, 에러 메시지를 사용자 친화적인 "데이터를 불러오는 중 오류가 발생했습니다."로 변경했습니다. 상세 에러는 콘솔에 기록됩니다.

### 3.4. React 19 `useTransition` 패턴 적용
- `page.tsx`의 검색(`handleSearch`) 기능에 `useTransition` 훅을 적용하여, 검색 실행 중 `isPending` 상태를 활용한 UI 피드백을 제공하도록 했습니다. 이로써 `isLoading` 상태를 수동으로 관리할 필요가 줄어들었습니다.

### 3.5. React 19 `useSuspenseQuery` 패턴 적용
- `MainDashboard.tsx`를 `useQuery`에서 Apollo Client의 `useSuspenseQuery`로 리팩토링하여 React Suspense 기반 데이터 페칭을 활용하도록 했습니다.
- `page.tsx`에서는 `MainDashboard`를 `<Suspense>`와 `QueryBoundary`로 감싸 로딩 및 에러 상태를 선언적으로 처리하도록 했습니다.

### 3.7. `useSearchMatches.ts` 무한 재시도 버그 수정
- `useSearchMatches` 훅에서 `runSearch` 함수가 렌더링마다 새로운 참조를 생성하여 `page.tsx`의 `useEffect`를 불필요하게 재실행시키는 문제를 해결했습니다. `runSearch` 함수를 `useCallback`으로 메모이제이션하여 참조 안정성을 확보했습니다.

### 3.8. `page.tsx` React Hooks 규칙 위반 에러 수정
- `useEffect` 내부에서 `useAppSelector`를 호출하여 발생한 "You might have mismatching versions of React and the renderer" 에러를 수정했습니다. `isInitial` 상태를 훅의 최상위 레벨에서 가져오도록 변경하고 `useEffect`의 의존성 배열에 추가했습니다. 이는 React Hooks 규칙("Hooks는 최상위에서만 호출되어야 한다")을 준수하도록 합니다.
