# ViewModel 패턴 및 Multiple Back Stacks 구현

**날짜**: 2025-12-30
**작성자**: Claude Sonnet 4.5
**목적**: Redux ViewModel 패턴 구현 및 도메인별 상태 영구 보존

---

## 📋 작업 개요

Alpha-Match Frontend에서 발견된 **상태 손실 버그**를 해결하기 위해 ViewModel 패턴을 구현했습니다. 기존에는 Hook의 `useState`로 검색 결과를 관리하여 모드 전환 시 데이터가 손실되었으나, Redux에 저장하는 방식으로 변경하여 **Multiple Back Stacks** 패턴을 완성했습니다.

---

## 🐛 발견된 버그

### 증상

```
시나리오:
1. Candidate 모드에서 검색 수행 → 결과 10건 표시
2. 결과 중 하나를 클릭하여 상세 페이지 이동
3. Recruiter 모드로 전환
4. 다시 Candidate 모드로 복귀
5. 뒤로가기 버튼 클릭

예상: 이전 검색 결과 10건 표시
실제: "검색 결과가 없습니다" 메시지 출력 ❌
```

### 근본 원인 분석

**문제 #1: useState의 생명주기**
```typescript
// useSearchMatches.ts (Before)
const [modeMatches, setModeMatches] = useState<Record<UserMode, MatchItem[]>>({
  [UserMode.CANDIDATE]: [],
  [UserMode.RECRUITER]: [],
});
```
- Hook의 로컬 상태는 컴포넌트 재렌더링 시 손실 가능
- React reconciliation 과정에서 예측 불가능한 초기화 발생

**문제 #2: useEffect의 불필요한 API 재호출**
```typescript
// HomePage.client.tsx (Before)
useEffect(() => {
  if (pageViewMode === 'results' && selectedSkills.length > 0 && !isInitial) {
    runSearch(...); // Redux 캐시를 확인하지 않고 무조건 API 호출
  }
}, [userMode, pageViewMode, isInitial]);
```
- 뒤로가기 시 pageViewMode 변경 → useEffect 트리거
- Redux에 이미 matches가 있어도 새로운 API 호출
- API가 빈 결과 반환 시 기존 데이터 덮어쓰기

---

## ✅ 구현 내역

### 1. searchSlice에 matches 필드 추가 (ViewModel Layer)

**파일**: `src/services/state/features/search/searchSlice.ts`

```typescript
// Before
export interface ModeSpecificSearchState {
  selectedSkills: string[];
  selectedExperience: string | null;
  isInitial: boolean;
}

// After
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
```

**추가된 Action:**
```typescript
setMatches: (state, action: PayloadAction<{ userMode: UserMode; matches: MatchItem[] }>) => {
  const { userMode, matches } = action.payload;
  state[userMode].matches = matches; // Redux에 영구 저장
}
```

### 2. useSearchMatches Hook Redux 연동

**파일**: `src/hooks/useSearchMatches.ts`

**변경 사항:**
```typescript
// Before: useState로 로컬 상태 관리
const [modeMatches, setModeMatches] = useState<Record<UserMode, MatchItem[]>>({
  [UserMode.CANDIDATE]: [],
  [UserMode.RECRUITER]: [],
});

// After: Redux에서 읽기
const dispatch = useAppDispatch();
const matches = useAppSelector((state) => state.search[currentUiMode].matches);
```

**API 응답 처리:**
```typescript
// Before
if (data && data.searchMatches) {
  setModeMatches(prev => ({
    ...prev,
    [mode]: data.searchMatches.matches,
  }));
}

// After: Redux ViewModel에 저장
if (data && data.searchMatches) {
  dispatch(setMatches({
    userMode: mode,
    matches: data.searchMatches.matches,
  }));
}
```

### 3. useEffect에 Redux 캐시 확인 로직 추가

**파일**: `src/app/_components/HomePage.client.tsx`

```typescript
// Before
useEffect(() => {
  if (pageViewMode === 'results' && selectedSkills.length > 0 && !isInitial) {
    runSearch(userMode, selectedSkills, selectedExperience);
  }
}, [userMode, pageViewMode, isInitial]);

// After: matches.length === 0 조건 추가
useEffect(() => {
  if (pageViewMode === 'results'
      && selectedSkills.length > 0
      && !isInitial
      && matches.length === 0) { // ← Redux 캐시 체크
    runSearch(userMode, selectedSkills, selectedExperience);
  }
}, [userMode, pageViewMode, isInitial]);
```

**로직 흐름:**
```
뒤로가기 (detail → results)
  ↓
pageViewMode 변경 → useEffect 트리거
  ↓
Redux에 matches 있는지 확인
  ↓
  ┌─────────────┬─────────────┐
  │ matches > 0 │ matches = 0 │
  ▼             ▼
캐시 사용 ✅    API 호출 🔄
```

---

## 🏗️ 아키텍처 개선

### Before: 2-Layer (취약한 구조)

```
┌─────────────────────────────────┐
│  View Layer (React Components)  │
│  + Hook useState (matches)      │ ← 휘발성!
└────────────┬────────────────────┘
             │
┌────────────▼────────────────────┐
│  Data Layer (Apollo Client)     │
│  InMemoryCache                  │
└─────────────────────────────────┘
```

**문제점:**
- matches가 Hook의 로컬 상태에만 존재
- 컴포넌트 재렌더링 시 손실 위험
- 도메인별 분리 불가능

### After: 3-Layer (ViewModel 패턴)

```
┌─────────────────────────────────┐
│  View Layer (React Components)  │
└────────────┬────────────────────┘
             │
┌────────────▼────────────────────┐
│  ViewModel Layer (Redux)        │ ← 영구 저장!
│  - searchSlice:                 │
│    CANDIDATE: {                 │
│      matches,                   │
│      selectedSkills             │
│    },                           │
│    RECRUITER: { ... }           │
│  - uiSlice: pageViewMode 등     │
└────────────┬────────────────────┘
             │
┌────────────▼────────────────────┐
│  Data Layer (Apollo Client)     │
│  InMemoryCache                  │
└─────────────────────────────────┘
```

**장점:**
- matches가 Redux에 영구 저장
- 도메인별(CANDIDATE/RECRUITER) 독립 상태
- Multiple Back Stacks 패턴 완성

---

## 📊 Multiple Back Stacks 패턴

### 개념

Android의 Bottom Navigation처럼 각 도메인(UserMode)마다 독립적인 상태 스택을 유지:

```
┌─────────────────────────────────────┐
│     CANDIDATE Mode Stack            │
├─────────────────────────────────────┤
│ matches: [10건]                     │
│ pageViewMode: 'detail'              │
│ selectedMatchId: '3'                │
│ selectedSkills: ['Python', 'React'] │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│     RECRUITER Mode Stack            │
├─────────────────────────────────────┤
│ matches: []                         │
│ pageViewMode: 'dashboard'           │
│ selectedMatchId: null               │
│ selectedSkills: []                  │
└─────────────────────────────────────┘
```

### 동작 흐름

```
1. CANDIDATE 모드 검색
   Redux[CANDIDATE].matches = [10건]
   Redux[CANDIDATE].pageViewMode = 'results'

2. 상세 페이지 이동
   Redux[CANDIDATE].selectedMatchId = '3'
   Redux[CANDIDATE].pageViewMode = 'detail'

3. RECRUITER 모드로 전환
   Redux.userMode = RECRUITER
   (CANDIDATE 상태는 그대로 보존)

4. CANDIDATE 모드로 복귀
   Redux.userMode = CANDIDATE
   → pageViewMode = 'detail' (이전 상태 복원)
   → selectedMatchId = '3'
   → 이전 상세 페이지 표시 ✅

5. 뒤로가기
   Redux[CANDIDATE].pageViewMode = 'results'
   useEffect 실행:
   → matches.length = 10 (Redux 캐시)
   → API 호출 스킵
   → 이전 검색 결과 10건 표시 ✅
```

---

## 🎯 해결된 문제

| 문제 | Before | After |
|-----|--------|-------|
| **상태 손실** | Hook useState 사용 → 손실 | Redux 저장 → 영구 보존 |
| **불필요한 API 호출** | 뒤로가기마다 새 요청 | Redux 캐시 우선 사용 |
| **도메인 분리** | 단일 상태 공유 | 도메인별 독립 스택 |
| **Multiple Back Stacks** | 미구현 | Android 패턴 완성 |

---

## 📝 수정된 파일 목록

### ✏️ 수정
1. `src/services/state/features/search/searchSlice.ts`
   - `ModeSpecificSearchState`에 `matches: MatchItem[]` 추가
   - `setMatches` action 추가
   - `MatchItem` import 추가

2. `src/hooks/useSearchMatches.ts`
   - `useState<modeMatches>` 제거
   - Redux `useAppSelector`로 matches 읽기
   - `dispatch(setMatches(...))`로 Redux에 저장
   - useCallback 의존성에 `dispatch` 추가

3. `src/app/_components/HomePage.client.tsx`
   - useEffect에 `matches.length === 0` 조건 추가
   - Redux 캐시 우선 사용 로직

4. `Frontend/Front-Server/GEMINI.md`
   - "완료" 섹션 제거
   - "상태 관리 분리 (ViewModel 패턴)" 섹션 추가
   - 트러블슈팅 문서 참조 추가

5. `Frontend/Front-Server/docs/ARCHITECTURE.md`
   - "상태 관리" 섹션에 ViewModel 패턴 설명 추가
   - Redux의 역할 명확화 (ViewModel Layer)

### ✨ 신규 생성
6. `docs/troubleshooting/ViewModel_Multiple_Back_Stacks.md`
   - React 상태 관리 생명주기 설명
   - useState vs Redux 비교
   - 버그 원인 및 해결 과정 상세 문서화

7. `docs/hist/2025-12-30_ViewModel_Multiple_Back_Stacks.md` (이 문서)

---

## 🔍 테스트 결과

### 시나리오 테스트

**Before (버그):**
```
1. Candidate 검색 → 결과 10건
2. 상세 페이지 → Recruiter 전환 → Candidate 복귀
3. 뒤로가기
   결과: "검색 결과가 없습니다" ❌
```

**After (수정):**
```
1. Candidate 검색 → 결과 10건 (Redux 저장)
2. 상세 페이지 → Recruiter 전환 → Candidate 복귀
   결과: 이전 상세 페이지 정상 표시 ✅
3. 뒤로가기
   결과: Redux 캐시 활용, 검색 결과 10건 표시 ✅
```

### 빌드 결과

```bash
$ npm run build

✓ Compiled successfully in 20.0s
✓ Running TypeScript
✓ Generating static pages (3/3)

Route (app)      Revalidate  Expire
┌ ○ /                    1h      1y
└ ○ /_not-found

○  (Static)  prerendered as static content
```

---

## 📚 학습 포인트

### 1. React 상태의 생명주기 이해

- **useState**: 컴포넌트 인스턴스에 바인딩, 언마운트 시 손실
- **Redux**: 컴포넌트 트리 외부, 영구 보존
- **Apollo Cache**: 네트워크 레벨, GraphQL 쿼리 캐싱

### 2. ViewModel 패턴의 중요성

Android/iOS MVVM 패턴을 React에 적용:
- View: React Components
- ViewModel: Redux Slices
- Model: Apollo Client + GraphQL

### 3. Multiple Back Stacks의 UX 가치

각 도메인마다 독립적인 네비게이션 스택 유지:
- 사용자가 모드 전환 후 돌아왔을 때 이전 상태 그대로 복원
- 더 나은 사용자 경험 제공

---

**작업 완료일**: 2025-12-30
**테스트 상태**: ✅ 빌드 성공, 시나리오 테스트 통과
**성능 개선**: Redux 캐시 활용으로 불필요한 API 호출 제거
