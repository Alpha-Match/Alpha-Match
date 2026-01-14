# 트러블슈팅: GraphQL 에러 발생 시 토스트 알림 누락

**날짜**: 2025-12-27

## 1. 문제 상황 (Symptom)

GraphQL 쿼리에서 변수 타입 불일치(`VariableTypeMismatch`)와 같은 유효성 검사 오류(Validation Error)가 발생했을 때, 에러가 UI에 직접 노출되었지만 사용자에게 피드백을 주기 위한 토스트 알림(Toast Notification)이 전혀 나타나지 않았다.

## 2. 원인 분석 (Root Cause Analysis)

문제의 근본 원인은 `src/services/api/apollo-client.ts` 파일의 `onError` 링크 핸들러가 오래된 **Apollo Client v3 패턴**으로 구현되어 있었기 때문이다.

### 이전 코드 (`apollo-client.ts`)

```typescript
import { onError } from "@apollo/client/link/error";
import { GraphQLError } from "graphql";

// ...

const errorLink = onError((errorResponse: any) => {
  if (errorResponse.graphQLErrors) {
    errorResponse.graphQLErrors.forEach(/* ... */);
    // ... 이벤트 디스패치
  }

  if (errorResponse.networkError) {
    console.error(`[Network error]: ${errorResponse.networkError.message}`);
    // ... 이벤트 디스패치
  }
});
```

이 v3 방식은 `graphQLErrors`와 `networkError` 두 속성을 개별적으로 확인한다. 하지만 이번에 발생한 `VariableTypeMismatch`와 같은 유효성 검사 오류는 `graphQLErrors` 배열에 포함되지 않고 다른 형태로 전달되어, 기존 로직에서는 이를 감지하지 못했다. 결과적으로 `show-notification` 커스텀 이벤트가 트리거되지 않아 토스트 알림이 표시되지 않았다.

이는 프로젝트의 `APOLLO_CLIENT_PATTERNS.md` 문서에 명시된 **v4 패턴**과도 일치하지 않는 구현이었다.

## 3. 해결책 (Solution)

Apollo Client v4의 공식 패턴에 따라 `onError` 링크를 단일 `error` 객체를 처리하는 방식으로 리팩토링했다. 이 `error` 객체는 모든 종류의 오류(GraphQL, 네트워크, 서버 등)를 포함하며, 제공되는 클래스 기반 타입 가드(`CombinedGraphQLErrors.is`, `ServerError.is`)를 통해 명확하게 오류 유형을 식별할 수 있다.

### 수정된 코드 (`apollo-client.ts`)

```typescript
import { onError } from "@apollo/client/link/error";
import { CombinedGraphQLErrors, ServerError } from '@apollo/client/errors';

// ...

const errorLink = onError(({ error }) => {
  let userMessage = "An unexpected error occurred.";

  if (CombinedGraphQLErrors.is(error)) {
    // GraphQL 유효성 검사 및 실행 오류 모두 처리
    console.error('[GraphQL error]:', error.errors);
    userMessage = error.errors.map(e => e.message).join(' ');
  } else if (ServerError.is(error)) {
    // 서버 측 HTTP 오류 (e.g., 5xx) 처리
    console.error(`[Server error]: ${error.message}`);
    userMessage = 'Server is not responding. Please try again later.';
  } else if (error) {
    // 일반 네트워크 오류 처리
    console.error(`[Network error]: ${error.message}`);
    userMessage = 'Server connection failed. Please check your network.';
  }

  // 모든 종류의 에러에 대해 일관되게 알림 이벤트를 발생시킴
  document.dispatchEvent(new CustomEvent('show-notification', {
    detail: { message: userMessage, type: 'error' }
  }));
});
```

### 해결 결과

- **일관된 에러 처리**: v4 패턴을 적용함으로써 유효성 검사 오류를 포함한 모든 GraphQL 및 네트워크 오류를 안정적으로 감지하게 되었다.
- **알림 시스템 정상화**: 모든 종류의 오류가 `show-notification` 이벤트를 정상적으로 발생시켜, 사용자에게 토스트 알림으로 피드백을 제공할 수 있게 되었다.
- **코드 안정성 및 명확성**: `any` 타입 캐스팅을 제거하고, 타입 가드를 사용하여 코드의 안정성과 가독성을 높였다.
