# Front-Server 아키텍처 설계 문서

## 1. 디렉토리 구조

본 프로젝트는 애플리케이션이 복잡해져도 유지보수와 확장이 쉽도록 **기능/화면 단위(Feature-based)** 설계를 채택합니다. 모든 컴포넌트는 `src/components` 디렉토리 하위에 기능별로 그룹화됩니다.

| 디렉토리 | 역할 및 설계 이유 |
| :--- | :--- |
| **`src/app`** | **Next.js 16+의 표준 라우팅**: App Router의 규약으로, 파일 기반으로 페이지와 레이아웃을 직관적으로 관리합니다. 전역 Providers (`src/app/providers.tsx`) 및 루트 레이아웃 (`src/app/layout.tsx`)을 포함합니다. |
| **`src/components`** | **기능/화면 단위 컴포넌트 루트**: 모든 UI 컴포넌트는 이곳에 기능별로 분류되어 저장됩니다. |
| `components/common` | **범용 컴포넌트**: `Button`, `Tooltip`, `Icon` 등 특정 도메인에 종속되지 않고 앱 전반에서 재사용되는 가장 작은 단위의 UI 컴포넌트를 관리합니다. |
| `components/layout` | **레이아웃 컴포넌트**: `Header`, `ThemeManager` 등 앱의 전체적인 구조와 레이아웃을 담당하는 컴포넌트를 관리합니다. |
| `components/dashboard` | **대시보드 컴포넌트**: `DefaultDashboard`와 그를 구성하는 `GenericTreemap`, `CategoryPieChart` 등 대시보드 화면과 관련된 컴포넌트들을 관리합니다. |
| `components/input-panel`| **입력 패널 컴포넌트**: `InputPanel`, `SkillSelector` 등 사용자의 입력을 받는 좌측 패널과 관련된 컴포넌트들을 관리합니다. |
| **`src/lib`** | **공통 유틸리티**: 앱 전반에서 사용되는 보조 함수, 상수 등 특정 도메인에 종속되지 않는 공통 유틸리티를 관리합니다. |
| **`src/services`** | **외부 서비스 및 클라이언트 상태 관리**: 외부 API 연동 및 전역 클라이언트 상태를 관리하는 핵심 로직입니다. |
| `services/api` | **API 연동 로직**: GraphQL 클라이언트 (`apollo-client.ts`), GraphQL 쿼리 정의 등 외부 API와의 통신 및 설정을 담당합니다. |
| `services/state` | **전역 클라이언트 상태 관리**: Redux Toolkit 설정을 관리합니다. `features` 디렉토리 안에 각 기능별 `slice` 파일(`uiSlice`, `searchSlice` 등)을 두어 상태를 분리합니다. |
| **`src/hooks`** | **커스텀 React Hooks**: `useSearchMatches`와 같이 여러 컴포넌트에서 재사용될 수 있는 비즈니스 로직 및 상태 관련 로직을 분리하여 관리합니다. |
| **`src/types`** | **글로벌 타입 정의**: 애플리케이션 전반에서 사용되는 TypeScript 인터페이스, Enum 등 공통 타입 정의를 관리합니다. |
| **`src/constants`** | **전역 상수 관리**: API 엔드포인트, 설정 값, 테마 색상 등 애플리케이션 전반에서 사용되는 변경되지 않는 값들을 중앙에서 관리합니다. |

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

-   **클라이언트 상태 관리 (Client State): `Redux Toolkit`**
    -   API와 무관한 순수 UI 상태(예: 모달의 열림/닫힘 상태, 테마 설정) 및 여러 컴포넌트가 공유해야 하는 전역 상태를 관리합니다.
    -   주요 사용 예시:
        -   **`searchSlice`**: 검색 필터 옵션 (예: `activeTab`, `selectedSkills`, `selectedExperience`) 및 동적으로 로드된 기술 스택 목록 (`skillCategories`)과 같은 UI 관련 상태를 관리합니다.
        -   **`notificationSlice`**: `Apollo Error Link`에서 디스패치되는 에러 메시지나 일반 알림 메시지 등 전역 알림 상태를 관리합니다.
    -   `Redux Toolkit`은 보일러플레이트를 줄여주고, 예측 가능한 상태 관리를 가능하게 합니다.

이러한 역할 분리는 각 라이브러리가 가장 잘하는 것에 집중하게 하여 코드의 복잡성을 줄이고 예측 가능성을 높입니다.

---

**최종 수정일:** 2025-12-26