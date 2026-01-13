# Next.js Server Components 마이그레이션 및 Search 컴포넌트 개선

**날짜**: 2025-12-30
**작성자**: Claude Sonnet 4.5
**목적**: Next.js App Router의 Server Components를 제대로 활용하고, Search 컴포넌트의 도메인별 분리 및 API 연동 개선

---

## 📋 작업 개요

Alpha-Match Frontend가 Next.js App Router를 사용하고 있었지만, 모든 페이지가 `'use client'`로 선언되어 Server Components의 이점을 전혀 활용하지 못하고 있었습니다. 이번 작업을 통해:

1. **Server Components 아키텍처 도입** - 초기 데이터를 서버에서 fetch하여 FCP 개선
2. **Search 컴포넌트 개선** - useMatchDetail Hook 연동, 도메인별 UI 분리 (Recruit/Candidate)
3. **타입 정합성 확보** - MatchItem에서 description 등 존재하지 않는 필드 제거

---

## ✅ 완료된 작업

### 1. **MatchDetailPanel 완전 개편** (v1.0 → v2.0)

#### 문제점
- 존재하지 않는 `match.description` 필드 사용
- Detail 데이터를 fetch하지 않고 MatchItem의 데이터만 표시
- Recruit/Candidate 도메인 구분 없이 범용 UI만 제공

#### 해결 방안
```tsx
// Before: MatchItem의 description 필드 직접 사용 (존재하지 않음)
<MatchDetailPanel match={selectedMatch} onBack={onBackToList} />

// After: useMatchDetail Hook으로 서버에서 Detail 데이터 fetch
<MatchDetailPanel
  matchId={selectedMatchId}
  userMode={userMode}
  onBack={onBackToList}
  activeColor={activeColor}
/>
```

#### 구현 세부사항

**도메인별 UI 분리**:
```tsx
// CANDIDATE 모드: 채용 공고 상세
if (userMode === UserMode.CANDIDATE && recruitDetail) {
  return (
    <div>
      <Briefcase icon /> {/* 채용 공고 아이콘 */}
      <h2>{recruitDetail.position}</h2>
      <p>{recruitDetail.companyName}</p>

      {/* 메타 정보 */}
      - 경력: {recruitDetail.experienceYears}년
      - 영어: {recruitDetail.englishLevel}
      - 게시일: {recruitDetail.publishedAt}
      - 주요 키워드: {recruitDetail.primaryKeyword}

      {/* 상세 설명 */}
      {recruitDetail.description}
    </div>
  );
}

// RECRUITER 모드: 후보자 상세
if (userMode === UserMode.RECRUITER && candidateDetail) {
  return (
    <div>
      <User icon /> {/* 후보자 아이콘 */}
      <h2>후보자 프로필</h2>
      <p>{candidateDetail.positionCategory}</p>

      {/* 보유 기술 스택 */}
      {candidateDetail.skills}

      {/* 원본 이력서 (있는 경우) */}
      {candidateDetail.originalResume}
    </div>
  );
}
```

---

### 2. **Server Components 아키텍처 구축**

#### 디렉토리 구조
```
Frontend/Front-Server/
├── src/
│   ├── app/
│   │   ├── page.tsx                    # ✨ Server Component (async)
│   │   ├── layout.tsx                  # Server Component
│   │   └── _components/
│   │       └── HomePage.client.tsx     # ✨ Client Component
│   └── lib/
│       └── server/
│           └── api.ts                  # ✨ Server-side API functions
```

#### Server Component (page.tsx)

**파일**: `src/app/page.tsx`

```tsx
import { getSkillCategories } from '../core/server/api';
import { HomePageClient } from './_components/HomePage.client';

export default async function HomePage() {
  // 서버 사이드에서 초기 스킬 카테고리 데이터를 가져옵니다
  const initialSkillCategories = await getSkillCategories();

  // 클라이언트 컴포넌트에 초기 데이터를 전달
  return <HomePageClient initialSkillCategories={initialSkillCategories} />;
}
```

**특징**:
- `async` 함수로 선언 → Server Component
- `'use client'` 지시어 없음
- 서버에서 GraphQL 쿼리 실행
- Next.js의 `fetch`는 자동 캐싱 (revalidate 옵션으로 제어)

---

#### Server-side API Functions

**파일**: `src/lib/server/api.ts`

```typescript
export async function getSkillCategories(): Promise<SkillCategory[]> {
  const response = await fetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: `
        query GetSkillCategories {
          skillCategories {
            category
            skills
          }
        }
      `,
    }),
    next: {
      revalidate: 3600, // 1시간마다 재검증
    },
  });

  const result = await response.json();
  return result.data?.skillCategories || [];
}
```

**장점**:
- **서버에서만 실행** → 클라이언트 번들 크기 감소
- **Next.js 자동 캐싱** → 동일 요청은 재사용
- **revalidate 옵션** → ISR (Incremental Static Regeneration)

---

#### Client Component (HomePage.client.tsx)

**파일**: `src/app/_components/HomePage.client.tsx`

```tsx
'use client';

export function HomePageClient({ initialSkillCategories }: HomePageClientProps) {
  const dispatch = useAppDispatch();

  // 서버에서 전달받은 초기 데이터를 Redux에 로드
  useEffect(() => {
    if (initialSkillCategories && initialSkillCategories.length > 0) {
      const allSkills = initialSkillCategories.flatMap(category => category.skills);
      dispatch(setSkillCategories(allSkills));
    }
  }, [initialSkillCategories, dispatch]);

  // 사용자 인터랙션 처리
  const handleSearch = () => { ... };

  return <div>...</div>;
}
```

**특징**:
- `'use client'` 지시어 명시
- Redux, Apollo Client, useState 등 클라이언트 전용 기능 사용
- 서버에서 전달받은 `initialSkillCategories`를 Redux에 주입

---

### 3. **타입 정합성 확보**

#### 추가된 타입

**파일**: `src/types/index.ts`

```typescript
/**
 * SkillCategory - 스킬 카테고리 정보
 * API-Server의 SkillCategory 타입과 일치
 */
export interface SkillCategory {
  category: string;
  skills: string[];
}
```

#### 제거된 필드

**MatchItem에서 제거**:
- ❌ `description` - Detail 쿼리에서만 제공
- ❌ `location` - API에서 제공하지 않음
- ❌ `salary` - API에서 제공하지 않음

**수정된 컴포넌트**:
1. `MatchDetailPanel.tsx` - description 제거, useMatchDetail Hook 사용
2. `VisualizationPanel.tsx` - description, location, salary 제거
3. `MainContentPanel.tsx` - matchId 전달 방식으로 변경

---

### 4. **빌드 에러 수정**

빌드 과정에서 발견된 기존 코드 문제들을 수정:

| 컴포넌트 | 문제 | 해결 방안 |
|---------|------|----------|
| **QueryBoundary** | `ApolloError` import 실패 (Apollo Client 4) | `Error`로 통일 |
| **BaseTooltip** | `color` prop 누락 | interface에 추가 + 스타일 적용 |
| **LoadingSpinner** | `color` prop 누락 | interface에 추가 + 조건부 적용 |
| **ExperienceSelector** | `selectedExperience` 접근 오류 | `state.search[mode].selectedExperience`로 수정 |
| **VisualizationPanel** | `isInitial` 접근 오류 | `state.search[mode].isInitial`로 수정 |

---

## 📊 아키텍처 개선 효과

| 항목 | Before | After |
|------|---------|-------|
| **Initial Load** | 클라이언트에서 fetch | **서버에서 fetch** → FCP 개선 |
| **Server Components** | 0% 활용 (모두 'use client') | **page.tsx는 Server Component** |
| **초기 데이터** | Apollo Client useQuery | **Server-side fetch + props 전달** |
| **캐싱** | Apollo InMemoryCache만 | **Next.js fetch 캐싱 + Apollo 캐싱** |
| **Detail 조회** | MatchItem의 불완전한 데이터 | **useMatchDetail Hook + 도메인별 UI** |
| **타입 안정성** | 존재하지 않는 필드 참조 | **완전한 타입 정합성** |

---

## 🎯 Next.js Server Components 활용 현황

### ✅ 활용 중
- **Server Components** - page.tsx에서 초기 데이터 fetch
- **Metadata API** - SEO 최적화
- **App Router** - 파일 기반 라우팅
- **fetch revalidate** - ISR 패턴

### ⏳ 향후 활용 가능
- **Streaming SSR** - Suspense 경계 설정
- **Route Handlers** - `/api` 디렉토리
- **generateStaticParams** - 정적 페이지 생성
- **Dynamic Routes** - `/match/[id]` 패턴

---

## 📝 수정된 파일 목록

### ✨ 신규 생성
```
src/
├── lib/server/
│   └── api.ts                                  # Server-side API functions
├── app/_components/
│   └── HomePage.client.tsx                     # Client Component
└── types/index.ts                              # SkillCategory 타입 추가
```

### ✏️ 수정
```
src/
├── app/
│   └── page.tsx                                # 'use client' 제거, async 함수로 변경
├── components/
│   ├── common/
│   │   ├── BaseTooltip.tsx                     # color prop 추가
│   │   ├── LoadingSpinner.tsx                  # color prop 추가
│   │   └── QueryBoundary.tsx                   # ApolloError → Error
│   ├── input-panel/
│   │   └── ExperienceSelector.tsx              # state.search[mode] 접근
│   ├── layout/
│   │   └── MainContentPanel.tsx                # matchId 전달 방식 변경
│   └── search/
│       ├── MatchDetailPanel.tsx                # v2.0 완전 개편
│       └── VisualizationPanel.tsx              # description, location, salary 제거
```

---

## 🔍 주요 학습 포인트

### 1. Next.js App Router의 올바른 활용
```tsx
// ❌ 잘못된 패턴
'use client';  // page.tsx에서 모든 것을 클라이언트로

export default function HomePage() {
  const { data } = useQuery(...);  // 클라이언트에서 fetch
  return <div>{data}</div>;
}

// ✅ 올바른 패턴
// page.tsx (Server Component)
export default async function HomePage() {
  const data = await getServerSideData();  // 서버에서 fetch
  return <ClientComponent initialData={data} />;
}

// ClientComponent.tsx
'use client';
export function ClientComponent({ initialData }) {
  // 인터랙션 처리
}
```

### 2. Redux와 Server Components의 통합
```tsx
// Server Component에서 초기 데이터 fetch
const initialData = await getServerData();

// Client Component로 전달
<ClientComponent initialData={initialData} />

// Client Component에서 Redux에 주입
useEffect(() => {
  dispatch(setInitialData(initialData));
}, [initialData]);
```

### 3. GraphQL과 Server Components
```tsx
// core/server/api.ts (서버에서만 실행)
export async function getSkillCategories() {
  const response = await fetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    body: JSON.stringify({ query: ... }),
    next: { revalidate: 3600 },  // Next.js 캐싱
  });
  return response.json();
}

// Apollo Client는 클라이언트 사이드 인터랙션용으로만 사용
```

---

## ✅ 체크리스트

- [x] MatchDetailPanel에 useMatchDetail Hook 연동
- [x] Recruit/Candidate 도메인별 UI 분리
- [x] Server Components 아키텍처 설계
- [x] page.tsx를 Server Component로 전환
- [x] HomePage.client.tsx Client Component 분리
- [x] lib/server/api.ts 서버 사이드 함수 구현
- [x] SkillCategory 타입 추가
- [x] 타입 에러 수정 (10개 컴포넌트)
- [x] Next.js 빌드 성공
- [x] 문서화

---

## 🚀 향후 개선 방향

### Phase 2: Streaming SSR
```tsx
// app/page.tsx
import { Suspense } from 'react';

export default async function Page() {
  return (
    <Suspense fallback={<LoadingFallback />}>
      <DashboardAsync />  // 서버에서 비동기 렌더링
    </Suspense>
  );
}
```

### Phase 3: Route Handlers
```tsx
// app/api/skills/route.ts
export async function GET() {
  const data = await getSkillCategories();
  return Response.json(data);
}
```

### Phase 4: React 19 `use` Hook (선택적)
```tsx
// Server Component에서 Promise 전달
const skillsPromise = getSkillCategories();

return <ClientComponent skillsPromise={skillsPromise} />;

// Client Component에서 use Hook
'use client';
const skills = use(skillsPromise);  // Suspense 필요
```

---

**작업 완료일**: 2025-12-30
**테스트 상태**: ✅ Next.js Build 성공, Server Components 정상 작동
**성능 개선**: 초기 로딩 시 서버 사이드 렌더링으로 FCP 개선 예상
