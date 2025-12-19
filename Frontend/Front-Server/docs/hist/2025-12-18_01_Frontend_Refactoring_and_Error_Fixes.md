# Frontend - Refactoring 및 오류 수정 내역 (2025-12-18_01)

## 📋 개요
`Frontend/Front-Server` 프로젝트에서 발생한 `TypeError: Cannot read properties of undefined (reading 'deploymentId')` 오류를 시작으로, 광범위한 타입스크립트 컴파일 오류 해결, Apollo Client 4.0 마이그레이션 패턴 적용, 그리고 컴포넌트 구조 리팩토링 작업을 수행했습니다. 주요 변경 사항 및 해결된 문제들은 다음과 같습니다.

## ✅ 해결된 문제 및 개선 사항

### 1. `TypeError: Cannot read properties of undefined (reading 'deploymentId')` 해결
-   Next.js `16.0.10` 버전의 유효성에 대한 논의 및 확인 후, `node_modules`와 `.next` 디렉토리 초기화를 통해 해결되었습니다. 초기 버전 문제로 추정되었던 내부 오류를 제거했습니다.

### 2. 타입스크립트 컴파일 에러 수정
-   **React 타입 불일치**: `package.json`에서 `react@19.x.x`와 `@types/react@18.x.x` 간의 버전 불일치로 인한 오류를 `@types/react@19.x.x`로 업데이트하여 해결했습니다.
-   **`MatchItem` 인터페이스 확장**: `src/types/index.ts`의 `MatchItem` 인터페이스에 `VisualizationPanel.tsx`에서 사용되는 `description`, `location`, `salary` 속성을 추가했습니다.
-   **`VisualizationPanel.tsx` 속성명 통일**: `MatchItem` 인터페이스의 리팩토링된 속성명(`score`, `company`, `skills`)을 사용하도록 `VisualizationPanel.tsx`를 수정하고, `implicit any` 오류를 해결했습니다.
-   **`apollo-client.ts` 타입 및 로직 수정**:
    -   `onError` 링크의 콜백 함수 타입 추론 문제를 해결했습니다. (컴파일러의 타입 추론 문제로 `any` 캐스팅을 사용하여 임시 해결)
    -   `GraphQLError` 임포트 및 타입 명시, 에러 객체 구조 분해 방식 변경을 통해 Apollo Client 4.0의 에러 핸들링 패턴에 맞게 조정했습니다.
-   **`AppInitializer.tsx` 데이터 변환 로직**: `setSkillCategories` Redux 액션이 `string[]`을 기대하지만, GraphQL 응답 및 `TECH_STACKS_DEMO`가 객체 배열 형태여서 발생한 타입 불일치 오류를 `flatMap`을 사용하여 평탄화 처리함으로써 해결했습니다.
-   **`InputPanel.tsx` `key` prop 경고**: `skillCategories.map`에서 중복될 수 있는 `skill` 값에 `idx`를 추가하여 고유한 `key`를 생성하도록 수정했습니다.

### 3. 전역 에러 알림 시스템 리팩토링
-   **Apollo Link와 Redux 분리**: `apollo-client.ts`에서 Redux `store.dispatch`를 직접 호출하는 대신, 커스텀 DOM 이벤트(`'show-notification'`)를 발생시키도록 변경했습니다.
-   **`Notification.tsx` 이벤트 리스너 추가**: `Notification.tsx` 컴포넌트가 이 커스텀 이벤트를 수신하고 Redux 액션을 디스패치하도록 수정하여, 컴포넌트 외부에서 스토어를 직접 조작하는 문제점을 해결했습니다.
-   **UX 개선**: 알림 토스트에 5초 자동 숨김 및 슬라이드-인 애니메이션을 추가했습니다. (`globals.css`에 애니메이션 스타일 추가)

### 4. 컴포넌트 구조 리팩토링 및 관심사 분리
-   **`useSearchMatches` 커스텀 훅 추출**: `page.tsx`의 GraphQL 데이터 페칭 로직을 `src/hooks/useSearchMatches.ts` 커스텀 훅으로 분리하여 `page.tsx`의 역할을 단순화했습니다.
-   **`InputPanel.tsx` 하위 컴포넌트 분리 및 재구성**:
    -   `InputPanel.tsx` 내의 헤더, 경험 선택, 스킬 선택, 검색 버튼 부분을 각각 `InputPanelHeader.tsx`, `ExperienceSelector.tsx`, `SkillSelector.tsx`, `SearchButton.tsx` 컴포넌트로 분리했습니다.
    -   **파일 구조 계층화**: 이 분리된 컴포넌트들과 `InputPanel.tsx`를 `src/components/input-panel/` 디렉토리로 이동시켜 파일 구조를 조직화했습니다.
    -   **Props Drilling 감소**: `Header.tsx`, `InputPanel.tsx`, `VisualizationPanel.tsx`에서 Redux 훅을 직접 사용하여 필요한 상태를 가져오고, 핸들러 함수를 중앙 집중화하여 props drilling을 최소화했습니다.
-   **`ExperienceSelector` 비활성화**: 사용자 요청에 따라 `InputPanel.tsx`에서 `ExperienceSelector` 컴포넌트를 주석 처리하여 비활성화했습니다.

### 5. Apollo Client 4.0 패턴 문서화
-   `Frontend/Front-Server/docs/APOLLO_CLIENT_PATTERNS.md` 문서를 생성하여 Apollo Client 4.0의 주요 변경 사항(에러 핸들링, `useLazyQuery` 사용법, 타입스크립트 및 임포트 패턴, 링크 구성)을 정리하고 프로젝트의 고정 문서에 반영했습니다.

---

**특이사항**:
-   네트워크 에러 시 토스트 알림이 여전히 제대로 표시되지 않는 문제가 보고되었습니다. 이는 `errorLink` 내부 로직이 `networkError`를 제대로 감지하지 못하는 것으로 보입니다. 현재 `apollo-client.ts`에 `any` 캐스팅을 사용하여 컴파일 오류를 우회한 상태이며, 이 부분에 대한 추가적인 디버깅 및 수정이 필요합니다.
-   이번 리팩토링은 사용자 요구에 따라 "Smart/Dumb" 컴포넌트 패턴을 적용하여, `InputPanel`을 스마트 컨테이너로, 그 하위 컴포넌트들을 덤 컴포넌트로 만들었습니다. 이 과정에서 `InputPanel` 관련 Redux 로직이 `InputPanel.tsx` 파일 내로 중앙화되었습니다.
