# Front-Server

> **헤드헌터-구인공고 매칭 UI (Next.js 16 + Apollo Client 4)**

GraphQL API를 소비하여 벡터 기반 유사도 검색 결과를 사용자에게 제공하는 Frontend 애플리케이션입니다.

---

## 📋 주요 기능

- 🔍 **검색 인터페이스**: 키워드 및 필터 기반 검색 및 데스크탑 3단 레이아웃을 통한 개선된 탐색
- 📊 **GraphQL 데이터 소비**: Apollo Client로 효율적 캐싱
- 🎨 **Tailwind CSS**: 반응형 UI
- 🔄 **Redux Toolkit**: 클라이언트 상태 관리
- ⚡ **Next.js App Router**: Server/Client Component 최적화
- 🚨 **전역 에러 처리**: Apollo Error Link + Redux Notification

---

## 🚀 UI/UX 개선 사항 (2026-01-12 업데이트)

사용자 피드백을 반영하여 데스크탑 환경에서의 검색 UI를 재구성하고 전반적인 UX를 향상시켰습니다.

### 1. 데스크탑 레이아웃 재구성: 3단 Master-Detail View

데스크탑 검색 결과 화면이 너무 많은 정보를 한 번에 보여주어 압박감을 준다는 점을 해결하고, 다음와 같이 **3단 레이아웃**으로 재구성합니다 (`pageViewMode`가 'dashboard'가 아닐 때).

-   **좌측 (1단, `w-[380px]`): 검색 조건 입력 패널**
    -   `InputPanel` (항상 표시되어 검색 조건 변경 가능)
-   **중앙 (2단, `w-[450px]`): 검색 결과 분석 패널**
    -   `SearchResultAnalysisPanel` (검색된 스킬에 대한 통계, 차트 등 분석 정보 표시)
-   **우측 (3단, `flex-1`): 결과 목록 및 상세 정보 영역**
    -   **초기 상태:** `SearchResultPanel` (검색 결과 리스트만 표시)
    -   **항목 클릭 시:** `MatchDetailPanel` (선택된 항목의 상세 정보 표시)
    -   `MatchDetailPanel` 내의 '뒤로가기' 버튼을 클릭하면 다시 `SearchResultPanel` (목록)로 돌아갑니다.

이를 통해 각 패널의 책임이 명확해지고, 정보의 밀도를 적절히 분배하여 화면의 압박감을 해소하며, 사용자가 검색 조건 입력, 분석 결과 확인, 목록 탐색, 상세 정보 확인이라는 흐름을 자연스럽게 따라갈 수 있도록 돕습니다.

### 2. Header에 전역 '대시보드로 돌아가기' 버튼 추가

상단 헤더(`Header`)에 '🏠 대시보드' 버튼을 추가하여, `pageViewMode`가 'dashboard'가 아닐 때 항상 표시됩니다. 이 버튼은 사용자가 어떤 화면에 있든 한 번의 클릭으로 초기 대시보드로 돌아갈 수 있도록 접근성을 높입니다.

### 3. `TwoLevelPieChart` 색상 일관성 확보

`TwoLevelPieChart.tsx` 컴포넌트 내 `skillColor` 계산 로직에서 `chroma(...).brighten(0.8)` 부분을 제거하여, 하위 기술 스택도 해당 카테고리와 동일한 색상을 사용하도록 수정합니다. 이를 통해 차트 내에서 카테고리와 하위 스킬 간의 시각적 연결성이 강화되고, 전체적인 테마 일관성이 향상됩니다.

---

## 🛠️ 기술 스택

- **Next.js 16**: React 19 기반 프레임워크
- **Apollo Client 4**: GraphQL 클라이언트 + 캐싱
- **Redux Toolkit**: 전역 상태 관리
- **Tailwind CSS**: 유틸리티 CSS
- **TypeScript**: 타입 안정성

---

## 📂 프로젝트 구조

```
Frontend/Front-Server/
│
├── src/
│   ├── app/                    # Next.js App Router (페이지 및 레이아웃)
│   │
│   ├── components/             # 기능/화면 단위 컴포넌트
│   │   ├── common/             #   - 범용 컴포넌트 (Button, Tooltip, Icon 등)
│   │   ├── dashboard/          #   - 대시보드 화면 관련 컴포넌트
│   │   ├── input-panel/        #   - 검색 입력 패널 관련 컴포넌트
│   │   ├── layout/             #   - 전역 레이아웃 (Header 등)
│   │   └── search/             #   - 검색 결과 화면 관련 컴포넌트
│   │
│   ├── services/               # 외부 서비스 및 클라이언트 상태 관리
│   │   ├── api/                #   - API 연동 로직 (GraphQL 클라이언트, 쿼리)
│   │   └── state/              #   - 전역 클라이언트 상태 관리 (Redux slices, hooks, store)
│   │
│   ├── lib/                    # 공통 유틸리티
│   │
│   ├── graphql/                # GraphQL (쿼리, 타입 등)
│   │
│   ├── hooks/                  # 커스텀 React Hooks (e.g., useSearchMatches)
│   │
│   ├── types/                  # 전역 TypeScript 타입
│   │
│   └── constants/              # 전역 상수
│
├── docs/                       # 개발 문서
│
├── package.json
├── GEMINI.md                   # AI 개발 가이드
└── README.md                   # 이 문서
```

---

## 🚀 빠른 시작

### 사전 요구사항

- **Node.js** 20+
- **API Server** 실행 중 (GraphQL 엔드포인트)

### 1. 의존성 설치

```bash
cd Frontend/Front-Server
npm install
```

### 2. 환경 변수 설정

`.env.local` 파일 생성:

```env
NEXT_PUBLIC_GRAPHQL_ENDPOINT=http://localhost:8080/graphql
```

### 3. 개발 서버 실행

```bash
npm run dev
```

브라우저에서 http://localhost:3000 접속

### 4. 빌드 및 프로덕션 실행

```bash
# 빌드
npm run build

# 프로덕션 실행
npm start
```

---

## 📝 코드 컨벤션

### 1. Server vs Client Component

**Server Component (기본):**
- 데이터 fetching
- 직접 DB 접근 (필요 시)
- SEO 최적화

**Client Component (`'use client'`):**
- 상태 관리 (useState, Redux)
- 이벤트 핸들러
- 브라우저 API 사용

### 2. Apollo Client 사용

```typescript
'use client';

import { useQuery } from '@apollo/client/react';
import { SEARCH_RECRUITS } from '@/graphql/queries';

export default function SearchResults() {
  const { data, loading, error } = useQuery(SEARCH_RECRUITS, {
    variables: { keyword: 'React' }
  });

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;

  return <div>{/* 결과 렌더링 */}</div>;
}
```

### 3. Redux 상태 관리

```typescript
// src/services/state/features/search/searchSlice.ts (파일 위치)
import { createSlice } from '@reduxjs/toolkit';
// ... (초기 상태 및 리듀서 정의) ...

// Component에서 사용 (예시)
import { useAppSelector, useAppDispatch } from '@/services/state/hooks';
import type { RootState } from '@/services/state/store';

function MyComponent() {
  const keyword = useAppSelector((state: RootState) => state.search.keyword);
  const dispatch = useAppDispatch();

  // ...
}
```

### 4. Tailwind CSS

```tsx
<div className="flex items-center justify-between px-4 py-2 bg-blue-500 hover:bg-blue-600 rounded-lg">
  <span className="text-white font-bold">검색</span>
</div>
```

---

## 5. React 19 개발 패턴 가이드

이 문서는 React 19에서 도입된 주요 기능과 권장 패턴을 요약하여, `Alpha-Match` 프론트엔드 개발 시 일관되고 현대적인 코드 스타일을 유지하기 위해 작성되었습니다.

---

### 5.1. Actions: 데이터 변경 로직의 혁신

**개념**: 서버 데이터 변경(생성, 수정, 삭제)과 관련된 비동기 로직을 처리하는 새로운 방식입니다. Actions는 데이터 제출부터 UI 피드백(로딩, 에러, 성공)까지의 전체 흐름을 React가 자동으로 관리하게 해줍니다.

**핵심 이점**:
- **Pending 상태 자동 관리**: `useState`로 `isLoading`과 같은 상태를 수동으로 관리할 필요가 없어집니다.
- **에러 처리 간소화**: `try/catch` 블록 대신, React가 에러를 잡아내어 UI에 쉽게 표시할 수 있습니다.
- **낙관적 업데이트(Optimistic Updates)**: 서버 응답을 기다리지 않고 UI를 먼저 긍정적으로 업데이트하여 사용자 경험을 향상시킬 수 있습니다. (`useOptimistic` 훅 사용)

#### 사용 패턴: `useTransition`과의 결합

가장 기본적인 Actions 패턴으로, `useTransition`을 사용하여 Pending 상태를 추적할 수 있습니다.

**Before (React < 19)**
```tsx
function AddToCartButton({ productId }) {
  const [isLoading, setIsLoading] = useState(false);

  const handleClick = async () => {
    setIsLoading(true);
    await addToCart(productId);
    setIsLoading(false);
  };

  return (
    <button onClick={handleClick} disabled={isLoading}>
      {isLoading ? 'Adding...' : 'Add to Cart'}
    </button>
  );
}
```

**After (React 19)**
```tsx
import { useTransition } from 'react';

function AddToCartButton({ productId }) {
  const [isPending, startTransition] = useTransition();

  const handleClick = () => {
    startTransition(async () => {
      await addToCart(productId);
    });
  };

  return (
    <button onClick={handleClick} disabled={isPending}>
      {isPending ? 'Adding...' : 'Add to Cart'}
    </button>
  );
}
```

---

### 5.2. `use` 훅: 조건부 렌더링의 미래

**개념**: `Promise`나 `Context` 같은 "읽을 수 있는(readable)" 값을 렌더링 중에 직접 사용할 수 있게 해주는 훅입니다.

**핵심 이점**:
- **조건부 로직 내에서 호출 가능**: 일반적인 훅과 달리, `if`, `for`, `early return` 문 안에서도 `use`를 호출할 수 있습니다.
- **코드 간소화**: `Promise`를 `Suspense`와 함께 사용하면, 데이터 로딩 상태를 더욱 깔끔하게 처리할 수 있습니다.

**Before (Context)**
```tsx
import { useContext } from 'react';
import { ThemeContext } from './ThemeContext';

function MyComponent() {
  const theme = useContext(ThemeContext);
  return <div className={theme}>...</div>;
}
```

**After (React 19)**
```tsx
import { use } from 'react';
import { ThemeContext } from './ThemeContext';

function MyComponent() {
  // if, return 등 조건문 안에서도 사용 가능
  const theme = use(ThemeContext);
  return <div className={theme}>...</div>;
}
```

---

### 5.3. `<form>`과 Actions

React 19에서는 HTML의 `<form>` 태그가 Actions를 직접 지원하도록 강화되었습니다. 폼 상태 관리를 위한 `useFormState`와 `useFormStatus` 훅이 함께 도입되었습니다.

#### `useFormStatus`
- `<form>`의 자식 컴포넌트에서 폼의 제출 상태(`pending`, `data`, `method`)를 알 수 있게 해줍니다.

#### `useFormState`
- 폼 액션의 결과에 따라 상태를 업데이트합니다. 서버로부터 받은 에러 메시지 등을 표시하는 데 유용합니다.

**예시: 로그인 폼**
```tsx
'use client';

import { useFormState, useFormStatus } from 'react-dom';
import { login } from './actions'; // 서버 액션 또는 클라이언트 액션

const initialState = {
  message: null,
};

function SubmitButton() {
  const { pending } = useFormStatus();
  return (
    <button type="submit" disabled={pending}>
      {pending ? 'Submitting...' : 'Login'}
    </button>
  );
}

export function LoginForm() {
  const [state, formAction] = useFormState(login, initialState);

  return (
    <form action={formAction}>
      <input type="email" name="email" required />
      <input type="password" name="password" required />
      <SubmitButton />
      {state?.message && <p style={{ color: 'red' }}>{state.message}</p>}
    </form>
  );
}
```

---

### 5.4. `ref`를 prop으로 전달

**개념**: `forwardRef`를 사용하지 않고도 `ref`를 함수 컴포넌트에 직접 prop으로 전달할 수 있습니다.

**Before**
```tsx
import { forwardRef } from 'react';

const MyInput = forwardRef((props, ref) => {
  return <input {...props} ref={ref} />;
});
```

**After (React 19)**
```tsx
function MyInput({ ref, ...props }) {
  return <input {...props} ref={ref} />;
}

// 또는 props로 바로 받기
function MyInput(props) {
  return <input {...props} />;
}
```

이 변경 사항은 코드를 더 직관적이고 간결하게 만들어주며, `forwardRef` 사용 시 발생하던 혼란을 줄여줍니다.

---

## 🔧 설정 가이드

### Apollo Client 설정

`src/services/api/apollo-client.ts`:

```typescript
import { ApolloClient, InMemoryCache, createHttpLink } from '@apollo/client';

const httpLink = createHttpLink({
  uri: process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT
});

export const client = new ApolloClient({
  link: httpLink,
  cache: new InMemoryCache()
});
```

### Error Link (전역 에러 처리)

```typescript
import { onError } from '@apollo/client/link/error';

const errorLink = onError(({ graphQLErrors, networkError }) => {
  if (graphQLErrors) {
    // Redux notification에 알림 추가
    dispatch(addNotification({ message: 'GraphQL 에러 발생', type: 'error' }));
  }
});
```

---

## 📚 개발 가이드

### GraphQL 쿼리 추가

1. `src/graphql/queries/{domain}.ts` 생성
2. gql 태그로 쿼리 작성
3. Component에서 `useQuery` 사용

```typescript
// src/graphql/queries/recruit.ts
import { gql } from '@apollo/client';

export const SEARCH_RECRUITS = gql`
  query SearchRecruits($keyword: String!, $limit: Int) {
    searchRecruits(keyword: $keyword, limit: $limit) {
      id
      companyName
      similarity
    }
  }
`;
```

### 새로운 페이지 추가

Next.js App Router 사용:

```
src/app/
├── page.tsx            # /
├── search/
│   └── page.tsx        # /search
└── results/
    └── [id]/
        └── page.tsx    # /results/:id
```

---

## 🧪 테스트

### 단위 테스트 (예정)

```bash
npm test
```

### E2E 테스트 (예정)

```bash
npm run test:e2e
```

---

## 📖 관련 문서

- [GEMINI.md](./GEMINI.md) - AI 개발 가이드 (상세 아키텍처 및 패턴)
- [아키텍처 가이드](./docs/ARCHITECTURE.md)
- [캐싱 전략](./docs/CACHING_STRATEGY.md)
- [APOLLO_CLIENT_PATTERNS.md](./docs/APOLLO_CLIENT_PATTERNS.md)

---

## 🐛 트러블슈팅

### GraphQL 연결 실패

```
Error: Network error: Failed to fetch
```

**해결:**
1. API Server 실행 확인
2. `.env.local`에서 GraphQL 엔드포인트 확인
3. CORS 설정 확인 (API Server)

### Hydration 에러

```
Error: Hydration failed because the initial UI does not match
```

**해결:**
- Server/Client Component 구분 확인
- `'use client'` 지시어 적절히 사용
- localStorage 등 브라우저 API는 Client Component에서만

### Redux State가 초기화됨

```
Redux state resets on page refresh
```

**해결:**
- Redux Persist 사용 (선택적)
- 또는 URL 쿼리 파라미터로 상태 관리

---

**최종 수정일:** 2026-01-12
