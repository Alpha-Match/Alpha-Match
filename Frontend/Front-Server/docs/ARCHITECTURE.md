# Front-Server 아키텍처 설계 문서

## 1. 디렉토리 구조

본 프로젝트는 애플리케이션이 복잡해져도 유지보수와 확장이 쉽도록 **기능/화면 단위(Feature-based)** 설계를 채택합니다. 모든 컴포넌트는 `src/components` 디렉토리 하위에 기능별로 그룹화됩니다.

| 디렉토리 | 역할 및 설계 이유 |
| :--- | :--- |
| **`src/app`** | **Next.js 16+의 표준 라우팅**: App Router의 규약으로, 파일 기반으로 페이지와 레이아웃을 직관적으로 관리합니다. 전역 Providers (`src/app/providers.tsx`) 및 루트 레이아웃 (`src/app/layout.tsx`)을 포함합니다. **Server Components를 기본으로 활용**하여 초기 데이터를 서버에서 fetch합니다. |
| `app/page.tsx` | **Server Component (async)**: 서버에서 초기 데이터를 fetch하여 Client Component에 props로 전달합니다. `'use client'` 지시어 없이 async/await 사용. |
| `app/_components/` | **Client Components**: `'use client'` 지시어를 명시하여 사용자 인터랙션을 처리합니다 (예: `HomePage.client.tsx`). |
| **`src/lib/server`** | **Server Components 전용 API 함수**: 서버에서만 실행되는 데이터 fetching 로직을 관리합니다. 클라이언트 번들에 포함되지 않아 번들 크기 감소 효과가 있습니다. Next.js의 `fetch` API를 활용하여 자동 캐싱 및 revalidate 옵션을 지원합니다. |
| **`src/components`** | **기능/화면 단위 컴포넌트 루트**: 모든 UI 컴포넌트는 이곳에 기능별로 분류되어 저장됩니다. 대부분 Client Components입니다. |
| `components/common` | **범용 컴포넌트**: `Button`, `Tooltip`, `Icon` 등 특정 도메인에 종속되지 않고 앱 전반에서 재사용되는 가장 작은 단위의 UI 컴포넌트를 관리합니다. |
| `components/layout` | **레이아웃 컴포넌트**: `Header`, `ThemeManager`, `TabController` 등 앱의 전체적인 구조와 레이아웃을 담당하는 컴포넌트를 관리합니다. `Header`는 전역 대시보드 버튼을 포함하여 사용자가 초기 대시보드 화면으로 쉽게 이동할 수 있도록 지원합니다. |
| `components/dashboard` | **대시보드 컴포넌트**: `MainDashboard`는 초기 화면에서 전체 화면으로 렌더링될 수 있으며, 이를 구성하는 `GenericTreemap`, `SearchedSkillsCategoryDistributionChart` 등 대시보드 화면과 관련된 컴포넌트들을 관리합니다. |
| `components/input-panel`| **입력 패널 컴포넌트**: `InputPanel`, `SkillSelector` 등 사용자의 입력을 받는 좌측 패널과 관련된 컴포넌트들을 관리합니다. |
| `components/search` | **검색 결과 화면 관련 컴포넌트**: `SearchResultPanel` (검색 결과 목록), `MatchDetailPanel` (상세 정보), `SearchResultAnalysisPanel` (검색 결과 분석) 등 검색 결과 표시 및 관련 분석 기능을 제공하는 컴포넌트들을 관리합니다. 이들은 새로운 3단 레이아웃의 중앙 및 우측 영역을 구성하며, `SearchResultPanel`에서 항목 클릭 시 `MatchDetailPanel`로 전환되는 등의 상호작용을 포함합니다. |
| **`src/services`** | **외부 서비스 및 클라이언트 상태 관리**: 외부 API 연동 및 전역 클라이언트 상태를 관리하는 핵심 로직입니다. |
| `services/api` | **API 연동 로직 (클라이언트 전용)**: GraphQL 클라이언트 (`apollo-client.ts`), GraphQL 쿼리 정의 등 클라이언트 측 API 통신 및 설정을 담당합니다. |
| `services/state` | **전역 클라이언트 상태 관리**: Redux Toolkit 설정을 관리합니다. `features` 디렉토리 안에 각 기능별 `slice` 파일(`uiSlice`, `searchSlice` 등)을 두어 상태를 분리합니다. **도메인별 state 분리** (CANDIDATE/RECRUITER). |
| **`src/hooks`** | **커스텀 React Hooks**: `useSearchMatches`, `useMatchDetail`과 같이 여러 컴포넌트에서 재사용될 수 있는 비즈니스 로직 및 상태 관련 로직을 분리하여 관리합니다. <br> - **`useAppNavigation`**: 앱 내 네비게이션 상태 및 액션을 캡슐화하여 `HomePage.client.tsx`와 같은 컴포넌트의 책임을 분리하고 컴포넌트를 간소화합니다. <br> - **`useIntersectionObserver`**: 요소의 가시성을 감지하는 로직(무한 스크롤 등)을 추상화하여 `SearchResultPanel.tsx`와 같은 컴포넌트의 코드를 간결하게 만듭니다. <br> - **`useHydrated`**: 서버-클라이언트 렌더링 불일치(Hydration 오류)를 방지하기 위해 컴포넌트가 클라이언트에서 완전히 하이드레이션되었는지 여부를 추적합니다. <br> - **`useMediaQuery`**: 브라우저의 미디어 쿼리(`(min-width: 1024px)`)를 구독하여 화면 크기에 따라 데스크탑/모바일 레이아웃을 동적으로 전환하는 데 사용됩니다. |
| **`src/types`** | **글로벌 타입 정의**: 애플리케이션 전반에서 사용되는 TypeScript 인터페이스, Enum 등 공통 타입 정의를 관리합니다. |
| **`src/constants`** | **전역 상수 관리**: API 엔드포인트, 설정 값, 테마 색상 등 애플리케이션 전반에서 사용되는 변경되지 않는 값들을 중앙에서 관리합니다. |

### 1.1. 스타일링 규칙 (중앙 집중형 테마)

본 프로젝트는 Tailwind CSS와 커스텀 테마 시스템을 통해 UI 스타일의 일관성과 유지보수성을 극대화합니다. `GEMINI.md`의 "5. 스타일링" 섹션에서 상세 규칙을 확인할 수 있습니다.

---

## 2. 기술 스택 선정 이유

### 가. GraphQL 클라이언트: Apollo Client

본 프로젝트는 **Apollo Client**를 GraphQL 클라이언트로 채택합니다.

| 라이브러리 | 핵심 특징 | Alpha-Match 프로젝트에서의 고려사항 |
| :--- | :--- | :--- |
| **Apollo Client (채택)** | **강력한 정규화 캐시**, 풍부한 기능, 거대한 생태계 | **장점**: 채용 공고, 후보자 등 복잡하게 얽힌 데이터의 일관성을 자동으로 유지하는 데 가장 유리합니다. 캐시 업데이트 로직을 최소화할 수 있어 생산성이 높습니다.<br>**단점**: 번들 사이즈가 상대적으로 크지만, 프로젝트의 복잡성을 고려할 때 얻는 이점이 더 큽니다. |
| **urql** | 경량, 우수한 확장성 (Exchanges) | **장점**: 가볍고 빠르게 시작할 수 있습니다.<br>**단점**: 기본 캐시(문서 캐시)는 데이터 일관성 유지를 위해 수동 작업이 더 많이 필요할 수 있습니다. 정규화 캐시를 추가할 수 있지만, 그럴 경우 Apollo Client 대비 장점이 희석됩니다. |
| **Relay** | 최고 수준의 성능, 컴파일러 기반 | **장점**: 최고의 런타임 성능.<br>**단점**: 학습 곡선이 매우 높고, 서버에 특정 명세를 요구하는 등 제약이 커서 초기 개발 속도와 유연성이 중요한 현 단계에 적합하지 않습니다. |

**결론**: `Alpha-Match`는 추천 시스템으로서 데이터 간의 관계가 복잡할 것으로 예상되므로, `Apollo Client`의 강력한 정규화 캐시는 장기적으로 개발 생산성과 데이터 안정성을 크게 향상시킬 것입니다.

### 나. 상태 관리: Apollo Client + Redux Toolkit

본 프로젝트는 서버 상태와 클라이언트 상태를 명확히 분리하여 관리합니다.

-   **서버 상태 관리 (Server State): `Apollo Client`**
    -   API로부터 받아오는 모든 데이터(채용 공고, 검색 결과, 기술 스택 목록 등)는 Apollo Client가 관리합니다.
    -   내장된 캐시(`InMemoryCache`)와 `Apollo Error Link`를 통해 데이터 캐싱, 로딩, 에러 상태를 자동으로 처리하여 UI와 서버 데이터 동기화를 간소화합니다. 특히, `Apollo Error Link`는 GraphQL 및 네트워크 에러를 전역적으로 가로채 `Redux Toolkit`의 `notificationSlice`를 통해 사용자에게 알림을 제공합니다.

-   **클라이언트 상태 관리 (Client State): `Redux Toolkit` - ViewModel 패턴**
    -   `Redux Toolkit`은 **ViewModel Layer**로서 UI 상태와 검색 결과를 영구 저장합니다.
    -   주요 사용 예시:
        -   **`searchSlice` (ViewModel)**:
            -   검색 필터 옵션: `selectedSkills`, `selectedExperience`
            -   **검색 결과 캐시**: `matches: MatchItem[]` - **중요!** Apollo Client에서 받아온 결과를 Redux에 저장하여 도메인별로 영구 보존
            -   기술 스택 목록: `skillCategories`
            -   도메인별 분리: `CANDIDATE`, `RECRUITER` 모드마다 독립적인 상태 유지 (Multiple Back Stacks)
        -   **`uiSlice`**: 도메인별 `pageViewMode`, `selectedMatchId` 등 UI 네비게이션 상태
        -   **`notificationSlice`**: `Apollo Error Link`에서 디스패치되는 에러 메시지나 일반 알림 메시지 등 전역 알림 상태를 관리합니다.
    -   **ViewModel 패턴의 핵심**:
        -   Hook의 `useState`로 `matches`를 관리하지 말 것 → 컴포넌트 재렌더링 시 손실됨
        -   반드시 `dispatch(setMatches({ userMode, matches }))`로 Redux에 저장
        -   이를 통해 모드 전환 후에도 검색 결과가 유지됨 (Android의 Multiple Back Stacks 패턴)

이러한 3-Layer 아키텍처(View - ViewModel - Data)는 각 계층의 책임을 명확히 분리하여 코드의 복잡성을 줄이고 상태의 영구성을 보장합니다.

#### 데이터 페칭 책임 (Data Fetching Responsibilities)

-   **페이지 컨테이너 컴포넌트 (`HomePage.client.tsx` 등):** 앱의 핵심적인 상태(e.g., `matches` 리스트)를 책임집니다. `useSearchMatches`와 같은 커스텀 훅을 통해 데이터를 가져오고, 그 결과를 Redux에 저장하며, 여러 하위 컴포넌트에 props로 전달하는 오케스트레이터(Orchestrator) 역할을 합니다.
-   **분석/통계 위젯 컴포넌트 (`SearchResultPanel.tsx` 및 그 하위):** 페이지의 핵심 데이터와는 별개인, 부가적인 분석/통계 데이터를 표시하는 컴포넌트는 자체적으로 데이터를 가져오는 것이 권장됩니다.
    -   **예시:** `SearchResultPanel`은 `GET_SEARCH_STATISTICS` 쿼리를 직접 호출하여 검색 결과의 전체 개수(`totalCount`)를 가져오고, 이를 자신과 자식 컴포넌트(`SearchResultStats`)에서 사용합니다.
    -   **장점:** 이 패턴은 컴포넌트의 독립성과 재사용성을 높이고, 상위 컴포넌트가 모든 데이터를 가져와 전달해야 하는 부담(props drilling)을 줄여줍니다.

### 다. Next.js Server Components 아키텍처

본 프로젝트는 **Next.js App Router의 Server Components**를 적극 활용하여 초기 로딩 성능을 최적화합니다.

#### Server Components vs Client Components 분리 전략

| 구분 | Server Component | Client Component |
| :--- | :--- | :--- |
| **선언 방법** | 기본값 (별도 지시어 불필요) | `'use client'` 지시어 명시 |
| **실행 환경** | 서버에서만 실행 | 서버 + 클라이언트 모두 |
| **주요 용도** | 초기 데이터 fetch, SEO | 사용자 인터랙션, 상태 관리 |
| **사용 가능 기능** | async/await, 서버 전용 API | useState, useEffect, Redux, Event Handlers |

#### 구현 패턴 예시

```typescript
// app/page.tsx (Server Component)
import { getSkillCategories } from '../lib/server/api';
import { HomePageClient } from './_components/HomePage.client';

export default async function HomePage() {
  // 서버에서 초기 데이터 fetch
  const initialSkillCategories = await getSkillCategories();

  // Client Component에 props로 전달
  return <HomePageClient initialSkillCategories={initialSkillCategories} />;
}

// lib/server/api.ts (Server-only)
export async function getSkillCategories(): Promise<SkillCategory[]> {
  const response = await fetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    body: JSON.stringify({ query: ... }),
    next: { revalidate: 3600 }, // Next.js 자동 캐싱
  });
  return response.json();
}

// app/_components/HomePage.client.tsx (Client Component)
'use client';
export function HomePageClient({ initialSkillCategories }: Props) {
  // Redux에 초기 데이터 주입
  useEffect(() => {
    dispatch(setSkillCategories(initialSkillCategories));
  }, [initialSkillCategories, dispatch]);

  // 사용자 인터랙션 처리
  const handleSearch = () => { ... };

  return <div>...</div>;
}
```

#### Server Components의 장점

1. **번들 크기 감소**: 서버 전용 코드가 클라이언트 번들에 포함되지 않음
2. **초기 로딩 성능 향상**: FCP (First Contentful Paint) 개선
3. **자동 캐싱**: Next.js fetch API의 revalidate 옵션 활용
4. **SEO 최적화**: 서버에서 렌더링된 HTML 제공

---

**최종 수정일:** 2026-01-12
**주요 변경사항:**
- 데이터 페칭 책임 분리 패턴 명시 (컨테이너 컴포넌트 vs 위젯 컴포넌트)
- Server Components 아키텍처 섹션 추가
- 디렉토리 구조에 `lib/server/` 및 `app/_components/` 추가
- 도메인별 state 분리 (CANDIDATE/RECRUITER) 명시