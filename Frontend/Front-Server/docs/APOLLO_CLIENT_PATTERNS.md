# Frontend - Apollo Client 4.0 Core Patterns

This document outlines the key coding patterns and conventions to be used for Apollo Client 4.0 within the Alpha-Match project. All new code should adhere to these patterns to ensure consistency and leverage the latest features and type safety improvements.

## 1. Error Handling

Apollo Client 4.0 unifies error handling into a single `error` property and introduces specific error classes.

### 1.1. `onError` Link

The `onError` link callback no longer receives separate `graphQLErrors` and `networkError` properties directly. Instead, check for the error type using the static `.is()` method on the error classes.

**Legacy Pattern (v3):**
```typescript
const errorLink = onError(({ graphQLErrors, networkError }) => {
  if (graphQLErrors) {
    // ...
  }
  if (networkError) {
    // ...
  }
});
```

**Correct Pattern (v4):**
```typescript
import { onError } from '@apollo/client/link/error';
import { CombinedGraphQLErrors, ServerError } from '@apollo/client/errors';

const errorLink = onError(({ error }) => {
  if (CombinedGraphQLErrors.is(error)) {
    error.errors.forEach(({ message }) =>
      console.log(`[GraphQL error]: ${message}`)
    );
  } else if (ServerError.is(error)) {
    console.log(`[Server error]: ${error.message}`);
  } else if (error) {
    // For generic network errors or others
    console.log(`[Network error]: ${error.message}`);
    // Dispatch notification event
    document.dispatchEvent(
      new CustomEvent('show-notification', {
        detail: { message: 'Server connection failed.', type: 'error' }
      })
    );
  }
});
```
*Note: In our project, we dispatch a custom event to show a UI notification.*

### 1.2. Hook Error Handling (`useQuery`, `useMutation`)

The `error` property from hooks like `useQuery` is now a unified error object. To check for specific error types, use the same class-based static methods.

**Legacy Pattern (v3):**
```typescript
const { data, error } = useQuery(QUERY);
if (error && error.graphQLErrors) {
  // handle GraphQL errors
}
```

**Correct Pattern (v4):**
```typescript
import { CombinedGraphQLErrors } from '@apollo/client/errors';

const { data, error } = useQuery(QUERY);

if (CombinedGraphQLErrors.is(error)) {
  // handle GraphQL errors
  console.log(error.errors);
} else if (error) {
  // handle network or other errors
  console.log(error.message);
}
```

## 2. `useLazyQuery` Usage

The `useLazyQuery` hook has significant behavioral changes. It is now intended primarily for user-initiated actions (e.g., button clicks).

### Key Changes:

1.  **`variables` are passed to the `execute` function**, not the hook itself.
2.  **`onCompleted` and `onError` callbacks are removed.** Use `async/await` with `try/catch` on the `execute` function promise instead.

**Legacy Pattern (v3):**
```typescript
const [execute, { data }] = useLazyQuery(QUERY, {
  variables: { id: 1 },
  onCompleted: (data) => { /* ... */ },
  onError: (error) => { /* ... */ },
});

const handleClick = () => execute();
```

**Correct Pattern (v4):
```typescript
const [execute, { data }] = useLazyQuery(QUERY);

const handleClick = async () => {
  try {
    const { data } = await execute({ variables: { id: 1 } });
    // Handle success...
  } catch (error) {
    // Handle error...
  }
};
```
*For queries that need to run on render, use `useQuery` with the `skip` option instead of `useLazyQuery` inside `useEffect`.*

## 3. TypeScript and Imports

### 3.1. React Hook Imports

All React-specific APIs, including hooks, must be imported from `@apollo/client/react`.

**Legacy Pattern (v3):**
```typescript
import { useQuery, gql } from '@apollo/client';
```

**Correct Pattern (v4):**
```typescript
import { useQuery } from '@apollo/client/react'; // React hooks from '/react'
import { gql } from '@apollo/client'; // Core APIs from root
```

### 3.2. Typing Apollo Link Context

To add custom properties to the `context` object in a type-safe way, use TypeScript's declaration merging to extend the `DefaultContext` interface. Create a `.d.ts` file (e.g., `apollo-client.d.ts`) for this.

**`apollo-client.d.ts`:**
```typescript
import "@apollo/client";

declare module "@apollo/client" {
  // Extend the default context to include custom properties
  interface DefaultContext {
    customHeader?: string;
    requestId?: number;
  }
}
```

## 4. Apollo Link Composition

The standalone `from`, `concat`, and `split` functions are deprecated. Use the static methods on the `ApolloLink` class.

**Legacy Pattern (v3):**
```typescript
import { from, createHttpLink } from '@apollo/client';
const link = from([errorLink, new HttpLink(...)]);
```

**Correct Pattern (v4):**
```typescript
import { ApolloLink, HttpLink } from '@apollo/client';
const link = ApolloLink.from([errorLink, new HttpLink(...)]);
```
This ensures code is more explicit and future-proof.

---

## 5. Apollo Client v4 에러 처리 상세 가이드

`Alpha-Match` 프로젝트는 Apollo Client v4를 사용하며, v3에서 v4로 마이그레이션 시 에러 처리 방식에 중대한 변경 사항이 있었습니다. 아래 내용은 `apollo-v4-migration.txt` 파일에서 추출된 핵심 변경 사항을 요약한 것입니다.

### 5.1. 주요 변경 사항

-   **`ApolloError` 클래스 제거**: v3에서 모든 에러를 감싸던 `ApolloError` 클래스가 완전히 제거되었습니다. 이는 디버깅 시 스택 트레이스 추적을 용이하게 합니다.
-   **GraphQL 에러**: `CombinedGraphQLErrors` 인스턴스로 캡슐화됩니다.
    -   **마이그레이션 패턴**: `CombinedGraphQLErrors.is(error)`를 통해 GraphQL 에러 여부를 확인하고, `error.errors` 속성으로 실제 GraphQL 에러 배열에 접근합니다.
-   **네트워크 에러**: 래핑되지 않고 그대로 반환됩니다.
    -   **마이그레이션 패턴**: `error` 객체 자체를 통해 네트워크 에러 메시지에 접근합니다 (`error.message`).
-   **프로토콜 에러**: `CombinedProtocolErrors` 인스턴스로 캡슐화됩니다.
    -   **마이그레이션 패턴**: `CombinedProtocolErrors.is(error)`를 통해 프로토콜 에러 여부를 확인하고, `error.errors` 속성으로 실제 에러 배열에 접근합니다.
-   **`clientErrors` 속성 제거**: v3에서 사용되지 않았던 `clientErrors` 속성은 v4에서 완전히 제거되었으며, 비-GraphQL/비-프로토콜 에러는 그대로 전달됩니다.

### 5.2. 예시: `onError` 링크 적용

`src/services/api/apollo-client.ts` 파일의 `onError` 링크는 위 변경 사항을 반영하여 모든 유형의 에러를 효과적으로 처리하고 토스트 알림을 트리거하도록 구현되어 있습니다.

```typescript
import { onError } from "@apollo/client/link/error";
import { CombinedGraphQLErrors, ServerError } from '@apollo/client/errors'; // ServerError는 네트워크 에러를 포괄

const errorLink = onError(({ error }) => {
  let userMessage = "An unexpected error occurred.";

  if (CombinedGraphQLErrors.is(error)) {
    console.error('[GraphQL error]:', error.errors);
    userMessage = error.errors.map(e => e.message).join(' ');
  } else if (ServerError.is(error)) {
    console.error(`[Server error]: ${error.message}`);
    userMessage = 'Server is not responding. Please try again later.';
  } else if (error) {
    console.error(`[Network error]: ${error.message}`);
    userMessage = 'Server connection failed. Please check your network.';
  }

  document.dispatchEvent(new CustomEvent('show-notification', {
    detail: { message: userMessage, type: 'error' }
  }));
});
```
---

**최종 수정일:** 2025-12-26