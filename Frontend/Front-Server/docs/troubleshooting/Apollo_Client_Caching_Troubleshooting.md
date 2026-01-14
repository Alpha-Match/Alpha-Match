# 트러블슈팅: Apollo Client 캐싱 및 스피너 문제

**작성일**: 2026-01-14

## 1. 문제 상황 (Problem)

프론트엔드 애플리케이션에서 특정 화면(`SearchResultAnalysisPanel`)을 다시 방문했을 때, 이전에 데이터를 성공적으로 불러왔음에도 불구하고 로딩 스피너가 항상 표시되는 현상이 발생했습니다. ViewModel 패턴과 `redux-persist`를 통해 상태 관리를 하고 있음에도 불구하고 데이터가 즉시 로드되지 않는 문제였습니다.

## 2. 원인 분석 (Root Cause Analysis)

문제의 근본 원인은 Apollo Client의 `fetchPolicy` 설정에 있었습니다.

### 2.1. Apollo Client `fetchPolicy` 개요

`fetchPolicy`는 Apollo Client가 GraphQL 쿼리를 실행할 때 캐시와 네트워크를 어떻게 활용할지 결정하는 중요한 설정입니다. 주요 `fetchPolicy` 옵션은 다음과 같습니다:

-   `cache-first` (기본값): 캐시를 먼저 확인하고, 유효한 데이터가 있으면 캐시 데이터를 반환합니다. 캐시에 데이터가 없거나 유효하지 않으면 네트워크 요청을 보냅니다. **가장 빠른 응답을 기대할 수 있으며, 이전에 로드된 데이터가 즉시 표시됩니다.**
-   `network-only`: 항상 네트워크 요청을 보내고, 응답을 캐시에 저장합니다. 캐시 데이터가 있더라도 무시하고 최신 데이터를 가져옵니다. **데이터의 최신성이 매우 중요할 때 사용되지만, 네트워크 지연이 발생할 수 있습니다.**
-   `cache-and-network`: 캐시 데이터를 즉시 반환하여 UI를 빠르게 표시한 후, 백그라운드에서 네트워크 요청을 보내 최신 데이터로 UI를 업데이트합니다. **초기 로딩 속도와 데이터 최신성을 모두 고려할 때 유용합니다.**
-   `no-cache`: 네트워크 요청만 보내고, 응답을 캐시에 저장하지 않습니다.
-   `cache-only`: 캐시 데이터만 반환합니다. 캐시에 데이터가 없으면 오류를 반환합니다.

### 2.2. 프로젝트의 기본 `fetchPolicy` 설정

`Frontend/Front-Server/src/core/client/services/api/apollo-client.ts` 파일의 `makeClient` 함수에서 전역 `defaultOptions`가 다음과 같이 설정되어 있었습니다.

```typescript
// src/core/client/services/api/apollo-client.ts
export const makeClient = () => {
    return new ApolloClient({
        // ...
        defaultOptions: {
          watchQuery: {
            fetchPolicy: 'cache-and-network',
            errorPolicy: 'all',
          },
          query: {
            fetchPolicy: 'network-only', // 문제의 원인
            errorPolicy: 'all',
          },
        },
    });
};
```

`useQuery` 훅은 `fetchPolicy`를 명시적으로 지정하지 않으면, 쿼리가 "watching" 상태가 아닌 경우(`watchQuery`가 아닌 일반 `query`로 간주) `defaultOptions.query.fetchPolicy`를 따릅니다. 따라서 `SearchResultAnalysisPanel.tsx` 및 `MainDashboard.tsx`, `TopSkills.tsx` 내의 `useQuery` 훅들은 `network-only` 정책을 따르고 있었습니다.

### 2.3. ViewModel과 `redux-persist`의 역할

-   **ViewModel (Redux Toolkit)**: UI 상태 및 검색 조건(`selectedSkills`, `searchedSkills`), 검색 결과(`matches`, `totalCount`)를 관리하고 모드 전환 시 상태를 보존합니다. `redux-persist`는 이 Redux 상태를 로컬 스토리지에 영구 저장합니다.
-   **Apollo Client**: GraphQL 쿼리 결과(서버 상태)를 자체 `InMemoryCache`에 저장합니다.

두 시스템은 서로 다른 계층의 상태를 관리하며, Apollo Client의 쿼리 결과 캐시는 Redux 상태와 별개로 작동합니다. `network-only` 정책은 Apollo Client의 `InMemoryCache`에 유효한 데이터가 있더라도 항상 네트워크 요청을 강제하기 때문에, ViewModel에 검색 조건(`searchedSkills`)이 존재해도 새로운 네트워크 요청과 로딩 스피너가 발생했습니다.

## 3. 해결책 (Solution)

각 컴포넌트의 `useQuery` 훅에서 `fetchPolicy`를 `cache-first`로 명시적으로 설정하여, Apollo Client 캐시의 데이터를 우선적으로 사용하도록 변경했습니다.

-   **`SearchResultAnalysisPanel.tsx`**: `GET_SEARCH_STATISTICS` 쿼리에 `fetchPolicy: 'cache-first'` 적용.
-   **`src/core/client/hooks/data/useSearchMatches.ts`**: `SEARCH_MATCHES_QUERY` 쿼리에 `fetchPolicy: 'cache-first'` 적용. (무한 스크롤의 `fetchMore`는 별도로 캐시 병합 로직을 가지고 있음)
-   **`src/app/_components/MainDashboard.tsx`**: `GET_TOP_COMPANIES` 쿼리에 `fetchPolicy: 'cache-first'` 적용.
-   **`src/components/search/analysis/TopSkills.tsx`**: `GET_SEARCH_STATISTICS` 쿼리에 `fetchPolicy: 'cache-first'` 적용.

**수정 예시 (`SearchResultAnalysisPanel.tsx`):**

```typescript
// src/components/search/analysis/SearchResultAnalysisPanel.tsx
export const SearchResultAnalysisPanel: React.FC<...> = ({ ... }) => {
	// ...
	const { data: statsData, loading: statsLoading } = useQuery<
		SearchStatisticsData,
		SearchStatisticsVars
	>(GET_SEARCH_STATISTICS, {
		variables: { mode: userMode, skills: searchedSkills },
		skip: searchedSkills.length === 0,
		fetchPolicy: 'cache-first', // 캐시 데이터 우선 사용
	});
	// ...
};
```

## 4. 해결 결과 (Result)

`fetchPolicy`를 `cache-first`로 변경함으로써, 동일한 검색 조건으로 화면을 다시 방문했을 때 Apollo Client 캐시에 유효한 데이터가 있다면 즉시 해당 데이터를 표시하고, 불필요한 네트워크 요청과 로딩 스피너의 표시를 방지할 수 있게 되었습니다. 이는 사용자 경험(UX)을 크게 향상시킵니다.

`redux-persist`를 통한 Redux 상태 동기화는 검색 조건(`searchedSkills`) 자체를 유지하는 데 기여하며, `fetchPolicy: 'cache-first'`는 해당 조건으로 가져왔던 GraphQL 쿼리 결과(서버 상태)를 빠르게 재활용하는 역할을 합니다. 이로써 두 캐싱 메커니즘이 상호 보완적으로 작동하게 됩니다.

---

**최종 수정일**: 2026-01-14
