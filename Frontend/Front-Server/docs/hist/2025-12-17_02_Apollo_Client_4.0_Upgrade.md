# 작업 요약: Apollo Client 4.0 업그레이드

- **날짜**: 2025-12-17

## 1. 개요

프론트엔드 프로젝트의 GraphQL 클라이언트를 Apollo Client 3.x에서 최신 안정 버전인 4.0으로 업그레이드했습니다. 이는 향후 호환성 및 최신 기능 활용을 위한 중요한 단계입니다. 마이그레이션 가이드(`https://www.apollographql.com/docs/react/migrating/apollo-client-4-migration`)에 따라 주요 변경 사항을 반영했습니다.

## 2. 주요 작업 내용

### 가. 패키지 버전 업데이트

*   **`Frontend/Front-Server/package.json`**:
    *   `@apollo/client`: `^3.9.11` -> `^4.0.11`로 업데이트
    *   `graphql`: `^16.8.1` -> `^16.9.0`로 업데이트 (AC 4.0과의 호환성을 위해 최신 16.x 버전으로 유지)
*   `npm install`을 실행하여 업데이트된 패키지를 설치했습니다.

### 나. `errorLink` 리팩토링 (`src/lib/apollo-client.ts`)

*   **목표**: Apollo Client 4.0에서 `onError` 콜백의 `graphQLErrors` 및 `networkError` 속성이 단일 `error` 객체로 통합되는 변경 사항에 대비하여 `errorLink`를 수정했습니다.
*   **구현 내용**:
    *   `onError` 콜백 함수 시그니처를 `(error)`로 변경하고, `error.graphQLErrors` 및 `error.networkError`를 직접 참조하도록 로직을 업데이트했습니다.
*   **결과**: Apollo Client 4.0의 에러 처리 패턴과 완벽하게 일치하는 방식으로 전역 에러 알림이 작동하도록 업데이트되었습니다.

### 다. `useLazyQuery` 결과 구조 반영 (`src/app/page.tsx`)

*   **목표**: `useLazyQuery` 훅이 반환하는 결과 객체의 구조가 Apollo Client 4.0에서 변경됨에 따라, 이를 사용하는 `page.tsx` 컴포넌트를 업데이트했습니다.
*   **구현 내용**:
    *   `useLazyQuery`의 비구조화 할당을 `[runSearch, { loading, data, error }]`에서 `const [runSearch, result] = useLazyQuery(...)`로 변경했습니다.
    *   `loading`, `data`, `error` 속성에 대한 모든 참조를 `result.loading`, `result.data`, `result.error`로 업데이트했습니다.
*   **결과**: `page.tsx`가 Apollo Client 4.0의 쿼리 결과 반환 패턴에 맞춰 정상적으로 작동하도록 수정되었습니다.

## 3. 결론

Apollo Client 4.0 업그레이드를 통해 프로젝트는 최신 라이브러리 버전을 사용하게 되었으며, 에러 처리 및 쿼리 훅 사용 방식이 미래 호환적인 패턴으로 개선되었습니다. 이는 애플리케이션의 안정성과 유지보수성을 더욱 강화할 것입니다.

---

**최종 수정일**: 2025-12-17
