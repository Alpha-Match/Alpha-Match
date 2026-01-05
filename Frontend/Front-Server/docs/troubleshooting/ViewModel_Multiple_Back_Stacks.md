# ViewModel 생명주기와 Multiple Back Stacks 패턴 트러블슈팅

**작성일**: 2025-12-30
**대상**: React + Redux + Apollo Client 아키텍처
**키워드**: ViewModel, Multiple Back Stacks, useState vs Redux, 상태 영속성

---

## 📋 목차

1. [문제 개요](#문제-개요)
2. [React 상태 관리의 생명주기](#react-상태-관리의-생명주기)
3. [발견된 버그와 근본 원인](#발견된-버그와-근본-원인)
4. [해결 방법: ViewModel 패턴 구현](#해결-방법-viewmodel-패턴-구현)
5. [Multiple Back Stacks 패턴 완성](#multiple-back-stacks-패턴-완성)
6. [Best Practices](#best-practices)

---

## 문제 개요

### 증상

```
시나리오:
1. Candidate 모드에서 검색 수행 → 결과 10건 표시
2. 결과 중 하나를 클릭하여 상세 페이지 이동
3. Recruiter 모드로 전환
4. 다시 Candidate 모드로 복귀 (이전 상세 페이지 유지)
5. 뒤로가기 버튼 클릭

예상 동작: 이전 검색 결과 10건이 그대로 표시
실제 동작: "검색 결과가 없습니다" 메시지 출력 ❌
```

### 원인 요약

1. **useState의 생명주기 한계**: Hook의 로컬 상태로 `matches`를 관리하여 컴포넌트 재렌더링 시 초기화됨
2. **useEffect의 불필요한 재실행**: 뒤로가기 시 `pageViewMode` 변경으로 useEffect 트리거 → 새로운 API 호출
3. **Redux 캐시 미활용**: ViewModel 패턴 미구현으로 검색 결과가 영구 저장되지 않음

---

## React 상태 관리의 생명주기

### 1. useState: 컴포넌트 로컬 상태

```typescript
// ❌ 문제의 코드 (useSearchMatches Hook)
export const useSearchMatches = () => {
  const [modeMatches, setModeMatches] = useState<Record<UserMode, MatchItem[]>>({
    [UserMode.CANDIDATE]: [],
    [UserMode.RECRUITER]: [],
  });

  // ...
  return { matches: modeMatches[currentUiMode] };
};
```

**문제점:**
- `useState`는 **컴포넌트 인스턴스에 바인딩**됨
- 컴포넌트가 언마운트되거나 key prop이 변경되면 상태 손실
- React의 reconciliation 과정에서 예측하기 어려운 상태 초기화 발생 가능

**생명주기:**
```
Component Mount → useState 초기화 → 상태 업데이트
       ↓
Component Unmount → 상태 손실 ❌
       ↓
Component Re-mount → useState 다시 초기화 (이전 값 없음)
```

### 2. Redux: 전역 영속 상태 (ViewModel)

```typescript
// ✅ 해결책 (Redux ViewModel)
export const searchSlice = createSlice({
  name: 'search',
  initialState: {
    [UserMode.CANDIDATE]: {
      selectedSkills: [],
      matches: [], // ← Redux에 저장
    },
    [UserMode.RECRUITER]: {
      selectedSkills: [],
      matches: [],
    },
  },
  reducers: {
    setMatches: (state, action) => {
      const { userMode, matches } = action.payload;
      state[userMode].matches = matches;
    },
  },
});
```

**장점:**
- Redux 스토어는 **React 컴포넌트 트리 외부**에 존재
- 컴포넌트 언마운트/리마운트와 무관하게 상태 유지
- 도메인별(UserMode별) 상태 분리 가능 → Multiple Back Stacks 구현

**생명주기:**
```
Redux Store 생성 (앱 시작 시 1회)
       ↓
Component A Mount → useSelector로 상태 읽기
       ↓
Component A Unmount → Redux 상태는 그대로 유지 ✅
       ↓
Component B Mount → 동일한 Redux 상태 접근 가능
```

### 3. Apollo Client: GraphQL 캐시

```typescript
// Apollo Client InMemoryCache
const client = new ApolloClient({
  cache: new InMemoryCache({
    typePolicies: {
      Query: {
        fields: {
          searchMatches: {
            // 캐시 정책 설정
          },
        },
      },
    },
  }),
});
```

**역할:**
- **네트워크 레벨 캐시**: API 응답을 캐싱하여 중복 요청 방지
- Redux와는 **별개의 계층**: Apollo는 Data Layer, Redux는 ViewModel Layer

---

## 발견된 버그와 근본 원인

### 버그 #1: useState로 인한 상태 손실

**문제 코드:**
```typescript
// useSearchMatches.ts (Before)
const [modeMatches, setModeMatches] = useState<Record<UserMode, MatchItem[]>>({
  [UserMode.CANDIDATE]: [],
  [UserMode.RECRUITER]: [],
});

// API 호출 후
setModeMatches(prev => ({
  ...prev,
  [mode]: data.searchMatches.matches,
})); // ← Hook 로컬 상태에만 저장
```

**근본 원인:**
- `useState`는 컴포넌트 레벨 상태
- `useSearchMatches`를 호출하는 `HomePage.client.tsx`가 재렌더링되면 Hook이 재초기화될 수 있음
- 특히 모드 전환이나 복잡한 상태 변경 시 React의 reconciliation이 Hook을 재생성할 가능성

**결과:**
- Candidate → Recruiter → Candidate 전환 시 matches 데이터 손실
- 뒤로가기 했을 때 빈 배열(`[]`)로 초기화된 상태만 남음

---

### 버그 #2: useEffect의 불필요한 API 재호출

**문제 코드:**
```typescript
// HomePage.client.tsx (Before)
useEffect(() => {
  if (pageViewMode === 'results' && selectedSkills.length > 0 && !isInitial) {
    runSearch(userMode, selectedSkills, selectedExperience);
    // ↑ 뒤로가기 시에도 무조건 새로운 API 호출!
  }
}, [userMode, pageViewMode, isInitial]);
```

**문제 시나리오:**
```
1. Detail 페이지에서 뒤로가기 클릭
   → pageViewMode: 'detail' → 'results' 변경

2. useEffect 의존성 배열에 pageViewMode가 있음
   → useEffect 실행

3. 조건문 체크:
   pageViewMode === 'results' ✓
   selectedSkills.length > 0 ✓
   !isInitial ✓

4. runSearch 호출
   → 새로운 API 요청 발송
   → 기존 Redux에 있던 matches는 무시됨

5. API 응답이 빈 배열이거나 에러 발생
   → matches = []로 덮어써짐
   → "검색 결과가 없습니다" 출력
```

**근본 원인:**
- ViewModel 캐시(Redux)를 확인하지 않고 무조건 API 호출
- `pageViewMode` 변경이 "새로운 검색"을 의미하는지, "이전 결과 복원"을 의미하는지 구분 안 됨

---

## 해결 방법: ViewModel 패턴 구현

### Step 1: Redux에 matches 저장 (ViewModel Layer)

```typescript
// searchSlice.ts
export interface ModeSpecificSearchState {
  selectedSkills: string[];
  selectedExperience: string | null;
  isInitial: boolean;
  matches: MatchItem[]; // ← ViewModel: 검색 결과 영구 저장
}

const initialModeSpecificState: ModeSpecificSearchState = {
  selectedSkills: [],
  selectedExperience: ExperienceLevel.MID,
  isInitial: true,
  matches: [], // 초기값
};

export const searchSlice = createSlice({
  name: 'search',
  initialState: {
    [UserMode.CANDIDATE]: { ...initialModeSpecificState },
    [UserMode.RECRUITER]: { ...initialModeSpecificState },
    skillCategories: [],
    skillsLoaded: false,
  },
  reducers: {
    setMatches: (state, action: PayloadAction<{ userMode: UserMode; matches: MatchItem[] }>) => {
      const { userMode, matches } = action.payload;
      state[userMode].matches = matches; // Redux에 영구 저장
    },
    // ... 기타 reducers
  },
});
```

### Step 2: Hook을 Redux 연동으로 변경

```typescript
// useSearchMatches.ts (After)
export const useSearchMatches = () => {
  const dispatch = useAppDispatch();
  const currentUiMode = useAppSelector((state) => state.ui.userMode);
  const matches = useAppSelector((state) => state.search[currentUiMode].matches); // Redux에서 읽기

  const [runSearchQuery, { loading }] = useLazyQuery<SearchMatchesData>(SEARCH_MATCHES_QUERY);

  const runSearch = useCallback(async (mode: UserMode, skills?: (string | null)[], experience?: string | null) => {
    // ... validation

    try {
      const { data } = await runSearchQuery({ variables: { mode, skills, experience } });

      if (data && data.searchMatches) {
        // Redux ViewModel에 저장 (도메인별 영구 보존)
        dispatch(setMatches({
          userMode: mode,
          matches: data.searchMatches.matches,
        }));
      }
    } catch (e) {
      // 에러 시 빈 배열로 초기화
      dispatch(setMatches({ userMode: mode, matches: [] }));
    }
  }, [dispatch, runSearchQuery]);

  return {
    runSearch,
    loading,
    matches, // Redux에서 읽어온 현재 모드의 matches
  };
};
```

**변경 사항:**
- ❌ `useState<modeMatches>` 제거
- ✅ `useAppSelector`로 Redux에서 matches 읽기
- ✅ `dispatch(setMatches(...))`로 Redux에 저장
- ✅ useCallback 의존성에 `dispatch` 추가

### Step 3: useEffect에서 Redux 캐시 확인

```typescript
// HomePage.client.tsx (After)
useEffect(() => {
  if (pageViewMode === 'results'
      && selectedSkills.length > 0
      && !isInitial
      && matches.length === 0) { // ← Redux 캐시 확인 추가!
    runSearch(userMode, selectedSkills, selectedExperience);
  }
  // eslint-disable-next-line react-hooks/exhaustive-deps
}, [userMode, pageViewMode, isInitial]);
```

**로직 흐름:**
```
1. pageViewMode가 'results'로 변경 (뒤로가기)
   ↓
2. useEffect 실행
   ↓
3. Redux에 이미 matches가 있는지 확인
   ↓
   ┌─────────────┬─────────────┐
   ▼             ▼             ▼
matches.length > 0  matches.length === 0
   │                 │
   │                 │
   ▼                 ▼
Redux 캐시 사용    API 호출
(재사용) ✅        (새 검색) 🔄
```

---

## Multiple Back Stacks 패턴 완성

### 개념

**Android의 Multiple Back Stacks:**
- 각 탭(Bottom Navigation)마다 독립적인 백 스택 유지
- 탭 전환 시 이전 탭의 스택은 보존되고, 복귀 시 그대로 복원

**Alpha-Match의 적용:**
```
┌─────────────────────────────────────────┐
│        UserMode (Navigation)            │
│  ┌──────────────┬──────────────┐        │
│  │  CANDIDATE   │  RECRUITER   │        │
│  └──────┬───────┴──────┬───────┘        │
└─────────┼──────────────┼────────────────┘
          │              │
          ▼              ▼
    ┌─────────┐    ┌─────────┐
    │ Redux   │    │ Redux   │
    │ State   │    │ State   │
    │ Stack   │    │ Stack   │
    ├─────────┤    ├─────────┤
    │ matches │    │ matches │
    │ skills  │    │ skills  │
    │ pageView│    │ pageView│
    │ matchId │    │ matchId │
    └─────────┘    └─────────┘
```

### 구현

**Redux Slice 구조:**
```typescript
interface SearchState {
  [UserMode.CANDIDATE]: ModeSpecificSearchState; // 독립 스택
  [UserMode.RECRUITER]: ModeSpecificSearchState; // 독립 스택
  skillCategories: string[];
  skillsLoaded: boolean;
}

interface ModeSpecificSearchState {
  selectedSkills: string[];
  selectedExperience: string | null;
  isInitial: boolean;
  matches: MatchItem[]; // 각 모드마다 별도 저장
}
```

**UI Slice 구조:**
```typescript
interface UiState {
  userMode: UserMode; // 현재 활성 모드
  [UserMode.CANDIDATE]: ModeSpecificUiState; // 독립 스택
  [UserMode.RECRUITER]: ModeSpecificUiState; // 독립 스택
}

interface ModeSpecificUiState {
  pageViewMode: 'dashboard' | 'results' | 'detail';
  selectedMatchId: string | null;
}
```

### 동작 흐름

```
User Action:
  Candidate 모드 진입
      ↓
  검색 (Python, React)
      ↓
  Redux: searchSlice[CANDIDATE].matches = [결과 10건]
  Redux: uiSlice[CANDIDATE].pageViewMode = 'results'
      ↓
  결과 중 #3번 클릭
      ↓
  Redux: uiSlice[CANDIDATE].selectedMatchId = '3'
  Redux: uiSlice[CANDIDATE].pageViewMode = 'detail'
      ↓
  Recruiter 모드로 전환
      ↓
  Redux: userMode = RECRUITER
  화면: Recruiter의 pageViewMode에 따라 렌더링
        (CANDIDATE 상태는 Redux에 그대로 보존됨)
      ↓
  다시 Candidate 모드로 전환
      ↓
  Redux: userMode = CANDIDATE
  화면: Redux에서 CANDIDATE 상태 복원
        → pageViewMode = 'detail', matchId = '3'
        → 이전 상세 페이지 그대로 표시 ✅
      ↓
  뒤로가기 클릭
      ↓
  Redux: uiSlice[CANDIDATE].pageViewMode = 'results'
  화면: useEffect 실행
        → matches.length === 10 (Redux 캐시)
        → API 호출 스킵, Redux의 matches 사용
        → 이전 검색 결과 10건 표시 ✅
```

---

## Best Practices

### 1. 상태 저장 위치 결정 가이드

| 상태 유형 | 저장 위치 | 이유 |
|---------|---------|------|
| **API 응답 데이터 (재사용 필요)** | Redux (ViewModel) | 모드 전환 시에도 유지 필요 |
| **UI 네비게이션 상태** | Redux (uiSlice) | 도메인별 독립 스택 유지 |
| **폼 입력 임시 값** | useState | 컴포넌트 레벨에서만 필요 |
| **모달 열림/닫힘** | useState | 컴포넌트 레벨에서만 필요 |
| **GraphQL 쿼리 결과 (1회성)** | Apollo Cache | 자동 캐싱, 재요청 방지 |

### 2. useEffect 의존성 배열 최적화

**❌ 나쁜 예:**
```typescript
useEffect(() => {
  runSearch(userMode, selectedSkills, selectedExperience);
}, [userMode, selectedSkills, selectedExperience, runSearch]);
// ↑ selectedSkills가 변경될 때마다 자동 검색 발생 (의도하지 않은 동작)
```

**✅ 좋은 예:**
```typescript
useEffect(() => {
  // 명확한 조건: 모드 전환 시에만, 그리고 Redux 캐시가 없을 때만
  if (pageViewMode === 'results' && selectedSkills.length > 0 && !isInitial && matches.length === 0) {
    runSearch(userMode, selectedSkills, selectedExperience);
  }
  // eslint-disable-next-line react-hooks/exhaustive-deps
}, [userMode, pageViewMode, isInitial]);
// ↑ userMode, pageViewMode, isInitial만 의존성에 포함
```

### 3. Redux Action 네이밍 컨벤션

```typescript
// ✅ 명확한 Action 이름
setMatches({ userMode, matches }) // "설정"의 의미: 덮어쓰기
appendMatches({ userMode, matches }) // "추가"의 의미: 기존 데이터에 추가
clearMatches({ userMode }) // "초기화"의 의미: 빈 배열로

// ❌ 모호한 Action 이름
updateMatches() // "업데이트"가 덮어쓰기인지 추가인지 불명확
```

### 4. 디버깅 팁

**Redux DevTools 활용:**
```javascript
// searchSlice의 상태 확인
// Redux DevTools에서:
{
  search: {
    CANDIDATE: {
      matches: [...], // ← 여기에 데이터가 있는지 확인
      selectedSkills: ["Python", "React"],
      isInitial: false
    },
    RECRUITER: {
      matches: [],
      selectedSkills: [],
      isInitial: true
    }
  }
}
```

**Console Logging 전략:**
```typescript
// useSearchMatches.ts
const runSearch = useCallback(async (mode, skills, experience) => {
  console.log('[Search] Query started:', { mode, skills, experience });

  const { data } = await runSearchQuery({ ... });

  console.log('[Search] Query result:', data);

  dispatch(setMatches({ userMode: mode, matches: data.searchMatches.matches }));
  console.log('[Search] Redux updated:', mode, data.searchMatches.matches.length);
}, [dispatch, runSearchQuery]);
```

### 5. 타입 안정성

**Redux Slice 타입 정의:**
```typescript
// searchSlice.ts
import { MatchItem } from '../../../../types'; // 반드시 import

export interface ModeSpecificSearchState {
  selectedSkills: string[];
  selectedExperience: string | null;
  isInitial: boolean;
  matches: MatchItem[]; // ← MatchItem 타입 명시
}

// Action Payload 타입
setMatches: (state, action: PayloadAction<{ userMode: UserMode; matches: MatchItem[] }>) => {
  // TypeScript가 타입 체크
}
```

---

## 요약

### 문제
- useState로 matches 관리 → 컴포넌트 재렌더링 시 손실
- useEffect가 뒤로가기 시에도 API 재호출 → Redux 캐시 무시

### 해결
1. **Redux에 matches 저장** (ViewModel 패턴)
2. **useSearchMatches Hook을 Redux 연동**으로 변경
3. **useEffect에 `matches.length === 0` 조건 추가**하여 캐시 우선 사용

### 결과
- ✅ 모드 전환 후에도 검색 결과 보존
- ✅ 뒤로가기 시 Redux 캐시 활용 (불필요한 API 호출 방지)
- ✅ Multiple Back Stacks 패턴 완성 (Android 네비게이션과 동일)

---

**참고 문서:**
- `GEMINI.md` - ViewModel 패턴 개요
- `ARCHITECTURE.md` - 3-Layer 상태 관리 아키텍처
- `docs/hist/2025-12-30_Server_Components_Migration.md` - Server Components 구조
