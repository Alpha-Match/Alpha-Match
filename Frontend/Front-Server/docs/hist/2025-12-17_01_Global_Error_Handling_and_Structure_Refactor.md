# 작업 요약: 전역 에러 처리 및 파일 구조 리팩토링

- **날짜**: 2025-12-17

## 1. 개요

API 통신 실패 시 사용자 경험을 개선하고, 프로젝트의 파일 구조를 더욱 체계적으로 정리하기 위해 다음과 같은 작업을 수행했습니다.

## 2. 주요 작업 내용

### 가. 전역 GraphQL 에러 처리 시스템 구현

*   **목표**: Apollo Client를 통한 GraphQL 및 네트워크 요청 실패 시 사용자에게 직관적인 알림을 제공하고, 에러 처리 로직을 중앙 집중화합니다.
*   **구현 내용**:
    *   **`src/store/features/notification/notificationSlice.ts` 생성**: 전역 알림(토스트/스낵바) 상태를 관리하는 Redux Slice를 정의했습니다. `message`, `type` (`error`, `success`, `info`), `open` 상태를 포함하며, `showNotification`, `hideNotification` 액션을 제공합니다.
    *   **`src/store/store.ts` 업데이트**: `notificationSlice`를 Redux 스토어에 통합하고, `Apollo Client`의 `errorLink`에서 스토어 인스턴스에 접근할 수 있도록 스토어를 싱글톤(`export const store = makeStore();`)으로 노출했습니다.
    *   **`src/components/Notification.tsx` 생성**: `notificationSlice`의 상태를 구독하여 알림 메시지를 UI로 표시하는 재사용 가능한 React 컴포넌트를 구현했습니다. 이 컴포넌트는 `src/app/layout.tsx`에 통합되었습니다.
    *   **`src/lib/apollo-client.ts` 업데이트**: Apollo Client `errorLink`를 구현하여 모든 GraphQL 및 네트워크 에러를 가로채도록 설정했습니다. 에러 발생 시, `notificationSlice`의 `showNotification` 액션을 디스패치하여 사용자에게 적절한 알림 메시지를 표시합니다.
*   **결과**: API 요청 중 발생하는 에러에 대해 일관되고 사용자 친화적인 피드백 메커니즘이 마련되었습니다.

### 나. `types.ts` 및 `constants.ts` 파일 구조 리팩토링

*   **목표**: 프로젝트의 규모가 커짐에 따라 `src` 디렉토리 하위의 파일들을 더 논리적으로 구성하여 가독성과 유지보수성을 향상시킵니다.
*   **구현 내용**:
    *   `src/types.ts` 파일을 `src/types/index.ts`로 이동하고, `src/types` 디렉토리를 생성했습니다.
    *   `src/constants.ts` 파일을 `src/constants/index.ts`로 이동하고, `src/constants` 디렉토리를 생성했습니다.
    *   이동된 파일들을 참조하는 모든 컴포넌트 및 모듈(예: `page.tsx`, `InputPanel.tsx`, `searchSlice.ts`, `AppInitializer.tsx`)의 import 경로를 새로운 구조에 맞게 업데이트했습니다.
*   **결과**: 관련 정의들이 응집력 있게 관리되어 코드 탐색이 용이해졌으며, 프로젝트의 스케일업에 대비한 기반을 마련했습니다.

### 다. 동적 `TECH_STACKS` 연동 준비

*   **목표**: `InputPanel`에서 사용하는 기술 스택 목록을 하드코딩된 값 대신 API에서 동적으로 가져오도록 준비합니다.
*   **구현 내용**:
    *   `src/constants/index.ts`의 `TECH_STACKS`를 `TECH_STACKS_DEMO`로 이름을 변경하여 폴백(fallback) 용도를 명확히 했습니다.
    *   `src/lib/graphql/queries.ts`에 `GET_SKILL_CATEGORIES` GraphQL 쿼리를 정의하여 기술 카테고리를 가져올 계약을 마련했습니다.
    *   `src/store/features/search/searchSlice.ts`에 `skillCategories` 상태 및 `setSkillCategories` 액션을 추가하여 동적으로 로드된 스킬 목록을 관리할 수 있도록 했습니다.
    *   `src/components/AppInitializer.tsx`를 생성하여 애플리케이션 로드 시 `GET_SKILL_CATEGORIES` 쿼리를 실행하고, 성공 시 Redux 스토어에 스킬 목록을 저장하며, 실패 시 `TECH_STACKS_DEMO`를 사용하는 폴백 로직을 구현했습니다.
    *   `src/components/InputPanel.tsx`를 리팩토링하여 `skillCategories`를 Redux 스토어에서 가져와 렌더링하도록 변경했습니다.
*   **결과**: 프론트엔드의 기술 스택 선택 UI가 API의 데이터와 연동될 수 있는 기반을 마련했으며, 백엔드 API가 준비되지 않아도 데모 데이터로 기능할 수 있도록 설정되었습니다.

## 3. 결론

이번 작업을 통해 프론트엔드 애플리케이션의 안정성, 사용자 경험, 코드 구조가 크게 향상되었습니다. 전역 에러 처리 시스템과 동적 데이터 연동 준비는 향후 개발의 견고한 기반이 될 것입니다.

---

**최종 수정일**: 2025-12-17
