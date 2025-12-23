# Front-Server

> **헤드헌터-구인공고 매칭 UI (Next.js 16 + Apollo Client 4)**

GraphQL API를 소비하여 벡터 기반 유사도 검색 결과를 사용자에게 제공하는 Frontend 애플리케이션입니다.

---

## 📋 주요 기능

- 🔍 **검색 인터페이스**: 키워드 및 필터 기반 검색
- 📊 **GraphQL 데이터 소비**: Apollo Client로 효율적 캐싱
- 🎨 **Tailwind CSS**: 반응형 UI
- 🔄 **Redux Toolkit**: 클라이언트 상태 관리
- ⚡ **Next.js App Router**: Server/Client Component 최적화
- 🚨 **전역 에러 처리**: Apollo Error Link + Redux Notification

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
│   ├── lib/                    # 라이브러리 설정 (Apollo Client 등)
│   │
│   ├── store/                  # Redux 전역 상태 관리
│   │   ├── features/           #   - 기능별 Slice (ui, search, notification)
│   │   ├── hooks.ts            #   - 타입이 적용된 Redux Hooks
│   │   └── index.ts            #   - Store 설정
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
├── CLAUDE.md                   # AI 개발 가이드 (Legacy)
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

import { useQuery } from '@apollo/client';
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
// searchSlice.ts
export const searchSlice = createSlice({
  name: 'search',
  initialState: {
    keyword: '',
    filters: {}
  },
  reducers: {
    setKeyword: (state, action) => {
      state.keyword = action.payload;
    }
  }
});

// Component에서 사용
const keyword = useSelector((state: RootState) => state.search.keyword);
const dispatch = useDispatch();
```

### 4. Tailwind CSS

```tsx
<div className="flex items-center justify-between px-4 py-2 bg-blue-500 hover:bg-blue-600 rounded-lg">
  <span className="text-white font-bold">검색</span>
</div>
```

---

## 🔧 설정 가이드

### Apollo Client 설정

`src/lib/apollo-client.ts`:

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

- [GEMINI.md](GEMINI.md) - Gemini AI 작성 상세 아키텍처
- [아키텍처 가이드](docs/ARCHITECTURE.md)
- [캐싱 전략](docs/CACHING_STRATEGY.md)
- [데이터 플로우](docs/DATA_FLOW.md)

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

**최종 수정일:** 2025-12-18
