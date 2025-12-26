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

**최종 수정일:** 2025-12-26