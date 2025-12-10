# Front-Server (Next.js) - Claude Instructions

**프로젝트명:** Alpha-Match Frontend
**작성일자:** 2025-12-10
**기술 스택:** Next.js 16.0.7 + TypeScript + React Query + GraphQL Client

---

## 📋 프로젝트 개요

Alpha-Match의 사용자 인터페이스를 제공하는 프론트엔드 서버입니다. Next.js 16.0.7 기반으로 구축되며, React Query를 활용한 효율적인 데이터 캐싱과 GraphQL을 통한 유연한 API 연동을 특징으로 합니다.

---

## 🎯 핵심 역할

1. **GraphQL API 소비**
   - API 서버의 GraphQL 엔드포인트 호출
   - 필요한 데이터만 선택적으로 요청 (Over-fetching 방지)

2. **데이터 캐싱**
   - React Query를 활용한 클라이언트 사이드 캐싱
   - API 호출 최소화 및 사용자 경험 향상

3. **UI/UX 제공**
   - 채용 공고 검색 인터페이스
   - 매칭 결과 시각화
   - 반응형 디자인

---

## 🏗️ 기술 스택

### Core
- **Next.js 16.0.7**: React 기반 프레임워크
- **TypeScript**: 타입 안정성
- **React 19**: 컴포넌트 기반 UI

### State Management & Data Fetching
- **React Query (TanStack Query)**: 서버 상태 관리 및 캐싱
- **GraphQL Client**: urql 또는 Apollo Client

### Styling
- **Tailwind CSS** 또는 **CSS Modules** (선택)
- **Shadcn/ui** 또는 **MUI** (컴포넌트 라이브러리)

---

## 📂 프로젝트 구조 (예정)

```
Frontend/Front-Server/
├── src/
│   ├── app/              # Next.js 16 App Router
│   │   ├── layout.tsx
│   │   ├── page.tsx
│   │   └── search/
│   │       └── page.tsx
│   │
│   ├── components/       # 재사용 가능한 컴포넌트
│   │   ├── ui/          # 기본 UI 컴포넌트
│   │   ├── SearchBar.tsx
│   │   └── RecruitCard.tsx
│   │
│   ├── lib/             # 유틸리티 및 설정
│   │   ├── graphql/     # GraphQL 쿼리/뮤테이션
│   │   │   ├── queries.ts
│   │   │   └── client.ts
│   │   └── react-query/ # React Query 설정
│   │       └── queryClient.ts
│   │
│   └── types/           # TypeScript 타입 정의
│       └── recruit.ts
│
├── public/              # 정적 파일
├── package.json
├── tsconfig.json
├── next.config.ts
└── CLAUDE.md           # 현재 문서
```

---

## 🔧 주요 기능

### 1. 채용 공고 검색
- 키워드 기반 검색
- 필터링 (경력, 영어 레벨 등)
- 실시간 검색 결과

### 2. 매칭 결과 표시
- Vector Similarity 기반 추천 공고
- 유사도 점수 시각화
- 상세 정보 모달

### 3. 캐싱 전략
- React Query를 통한 자동 캐싱
- Stale Time / Cache Time 설정
- Optimistic Update

---

## 🚀 GraphQL 연동

### API Endpoint
```
http://localhost:8080/graphql
```

### 예시 쿼리
```graphql
query SearchRecruits($keyword: String!, $limit: Int) {
  searchRecruits(keyword: $keyword, limit: $limit) {
    id
    companyName
    expYears
    englishLevel
    primaryKeyword
    similarity
  }
}
```

### React Query 통합
```typescript
const { data, isLoading, error } = useQuery({
  queryKey: ['recruits', keyword],
  queryFn: () => graphqlClient.request(SEARCH_RECRUITS, { keyword }),
  staleTime: 5 * 60 * 1000, // 5분
});
```

---

## 📝 개발 가이드

### 초기 설정
```bash
cd Frontend/Front-Server
npm install
npm run dev
```

### 환경 변수 (.env.local)
```bash
NEXT_PUBLIC_GRAPHQL_ENDPOINT=http://localhost:8080/graphql
```

### 코딩 컨벤션
- 컴포넌트명: PascalCase
- 파일명: kebab-case
- 함수명: camelCase
- TypeScript 필수 (any 금지)

---

## 🔗 관련 문서

- [루트 CLAUDE.md](../../CLAUDE.md)
- [API Server CLAUDE.md](../../Backend/Api-Server/CLAUDE.md)
- [Entire Structure](../../Backend/Batch-Server/docs/Entire_Structure.md)

---

## ✅ 현재 진행 상황

### 예정
- ⏳ Next.js 프로젝트 초기 설정
- ⏳ GraphQL Client 설정
- ⏳ React Query 설정
- ⏳ 기본 레이아웃 구성
- ⏳ 검색 페이지 구현
- ⏳ 매칭 결과 페이지 구현

---

**최종 수정일:** 2025-12-10
