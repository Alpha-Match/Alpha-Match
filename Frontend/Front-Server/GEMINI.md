# Front-Server - Claude Instructions

**역할:** GraphQL API 소비 → 헤드헌터-구인공고 매칭 UI 제공
**기술 스택:** Next.js 16 + React 19 + Apollo Client 4 + Redux Toolkit

---

## 📋 문서 목적

- **GEMINI.md (이 문서)**: AI가 참조할 메타정보 + 코드 위치
- **README.md**: 사람이 읽을 아키텍처/컨벤션 상세 설명

---

## 🗺️ 핵심 문서 경로

## 🗺️ 핵심 문서 경로
- **아키텍처 및 컨벤션**: `README.md` (이 디렉토리)
- **아키텍처 가이드**: `docs/ARCHITECTURE.md`
- **캐싱 전략**: `docs/CACHING_STRATEGY.md`
- **Apollo Client 패턴**: `docs/APOLLO_CLIENT_PATTERNS.md`

---

## 📂 구현된 코드 위치 (AI가 읽어야 할 경로)

### 🚀 엔트리포인트 (App Router)

- `src/app/layout.tsx` - 루트 레이아웃
- `src/app/page.tsx` - 메인 페이지

### ⚙️ Configuration

- `src/services/api/apollo-client.ts` - Apollo Client 설정
- `src/services/state/` - Redux 스토어 및 슬라이스
  - `src/services/state/store.ts` - Redux Store
  - `src/services/state/hooks.ts` - Custom Redux Hooks
  - `src/services/state/features/ui/uiSlice.ts` - UI 상태 슬라이스
  - `src/services/state/features/search/searchSlice.ts` - 검색 조건 슬라이스
  - `src/services/state/features/notification/notificationSlice.ts` - 전역 알림 슬라이스

### 🎨 Components (기능/화면 단위)

- `src/components/common/` - 범용 컴포넌트
- `src/components/dashboard/` - 대시보드
- `src/components/input-panel/` - 검색 입력 패널
- `src/components/layout/` - 전역 레이아웃
- `src/components/search/` - 검색 결과

### 📡 GraphQL & Hooks

- `src/graphql/queries/` - GraphQL 쿼리
- `src/hooks/` - 커스텀 React Hooks (e.g., `useSearchMatches`)

---

## 🚀 현재 구현 상태

### 🔄 진행 중
- 없음.

### ⏳ 예정
- GraphQL 쿼리 구현 (API Server 연동)
- 벡터 유사도 시각화 상세 구현
- 단위/E2E 테스트 코드 작성

## ⚠️ AI가 반드시 알아야 할 규칙

### 1. 코드 컨벤션 참조
**상세 컨벤션은 README.md와 GEMINI.md 참조!** AI는 코드 작성 전에:
1. `README.md` 또는 `GEMINI.md` 읽기 (아키텍처 패턴 이해)
2. 기존 컴포넌트 읽기 (위 경로 참조)
3. 같은 패턴으로 구현

### 2. Next.js App Router 패턴
- `src/app/` - 페이지 및 레이아웃
- Server Component vs Client Component 구분
- `'use client'` 지시어 사용 시점 명확히

### 3. 상태 관리 분리
- **서버 상태**: Apollo Client (GraphQL 캐시)
- **클라이언트 상태**: Redux Toolkit (UI 상태, 필터 등)

### 4. 타입 안정성
- 모든 컴포넌트에 Props 타입 정의
- GraphQL 응답 타입 정의

### 5. 스타일링
- Tailwind CSS 유틸리티 우선
- 커스텀 CSS는 `globals.css`에 최소화

### 6. 에러 처리
- Apollo Error Link로 전역 에러 처리
- Redux notificationSlice로 사용자 알림

---

## 7. Apollo Client v4 에러 처리 상세 가이드

`Alpha-Match` 프로젝트는 Apollo Client v4를 사용하며, v3에서 v4로 마이그레이션 시 에러 처리 방식에 중대한 변경 사항이 있었습니다. 아래 내용은 `apollo-v4-migration.txt` 파일에서 추출된 핵심 변경 사항을 요약한 것입니다.

### 7.1. 주요 변경 사항

-   **`ApolloError` 클래스 제거**: v3에서 모든 에러를 감싸던 `ApolloError` 클래스가 완전히 제거되었습니다. 이는 디버깅 시 스택 트레이스 추적을 용이하게 합니다.
-   **GraphQL 에러**: `CombinedGraphQLErrors` 인스턴스로 캡슐화됩니다.
    -   **마이그레이션 패턴**: `CombinedGraphQLErrors.is(error)`를 통해 GraphQL 에러 여부를 확인하고, `error.errors` 속성으로 실제 GraphQL 에러 배열에 접근합니다.
-   **네트워크 에러**: 래핑되지 않고 그대로 반환됩니다.
    -   **마이그레이션 패턴**: `error` 객체 자체를 통해 네트워크 에러 메시지에 접근합니다 (`error.message`).
-   **프로토콜 에러**: `CombinedProtocolErrors` 인스턴스로 캡슐화됩니다.
    -   **마이그레이션 패턴**: `CombinedProtocolErrors.is(error)`를 통해 프로토콜 에러 여부를 확인하고, `error.errors` 속성으로 실제 에러 배열에 접근합니다.
-   **`clientErrors` 속성 제거**: v3에서 사용되지 않았던 `clientErrors` 속성은 v4에서 완전히 제거되었으며, 비-GraphQL/비-프로토콜 에러는 그대로 전달됩니다.

### 7.2. 예시: `onError` 링크 적용

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
```
---

**최종 수정일:** 2025-12-26