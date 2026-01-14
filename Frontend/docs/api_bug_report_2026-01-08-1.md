# API 서버 버그 리포트: GetCandidateDetail INTERNAL_ERROR

**날짜**: 2026-01-08
**작성자**: Gemini (Frontend Agent)
**에러 심각도**: Blocker

## 1. 문제 요약

`RECRUITER` 모드에서 특정 후보자(Candidate)의 상세 정보를 조회할 때, API 서버에서 `INTERNAL_ERROR`가 발생합니다.

프론트엔드는 유효한 전체 UUID를 변수로 전달하고 있으나, 서버는 다른 ID에 대한 내부 오류를 반환하는 것으로 보여 백엔드 로직의 조사가 필요합니다.

## 2. 재현 과정

1.  프론트엔드에서 `RECRUITER` 모드로 전환합니다.
2.  검색을 수행하여 후보자 목록을 확인합니다.
3.  특정 후보자(ID: `250ad02a-6d5c-5a24-8bf1-ab47375a0ef8`)를 클릭하여 상세 정보 조회를 시도합니다.
4.  API 서버가 GraphQL 오류를 반환합니다.

## 3. 상세 정보

### 가. 프론트엔드 요청

- **GraphQL 쿼리**: `GetCandidateDetail`
- **전달된 변수 (Variables)**:
  ```json
  {
    "id": "250ad02a-6d5c-5a24-8bf1-ab47375a0ef8"
  }
  ```
- **프론트엔드 로그**:
  `[useMatchDetail] Fetching detail for mode: RECRUITER, ID: 250ad02a-6d5c-5a24-8bf1-ab47375a0ef8`

### 나. 서버 응답 (에러)

- **message**: `"INTERNAL_ERROR for e6e2a4f1-13"`
- **path**: `["getCandidate"]`
- **extensions**: `{ "classification": "INTERNAL_ERROR" }`

## 4. 분석 및 결론

- 프론트엔드는 **완전하고 유효한 UUID**를 요청 변수로 보내고 있음을 확인했습니다.
- 서버가 반환한 에러 메시지에는 요청된 ID와 다른, **잘린 형태의 ID(`e6e2a4f1-13`)**가 포함되어 있습니다.
- 이는 `getCandidate` 리졸버가 요청을 받은 후, 내부 로직을 수행하는 과정(예: 데이터베이스 조회, 데이터 처리 등)에서 예기치 않은 오류에 직면했음을 시사합니다. 서버의 에러 로깅 또는 메시지 생성 과정에서 ID가 잘려서 표시되었을 수 있습니다.

**결론**: 이 문제는 프론트엔드의 요청 문제가 아닌, **백엔드 API 서버의 내부 로직 오류**입니다. `getCandidate` 리졸버가 `250ad02a-6d5c-5a24-8bf1-ab47375a0ef8` ID를 처리하는 과정을 디버깅해야 합니다.

## 5. 백엔드 팀에 대한 권장 조치

1.  `SearchService.java` 또는 관련 리졸버에서 `getCandidate` 로직을 검토합니다.
2.  `250ad02a-6d5c-5a24-8bf1-ab47375a0ef8` ID를 사용하여 데이터베이스(`candidate`, `candidate_description`, `candidate_skill` 테이블 등)에서 데이터를 조회할 때 예외가 발생하는지 확인합니다.
3.  데이터베이스 조회 후 `CandidateDetail` 객체로 매핑하는 과정에서 발생하는 잠재적 `NullPointerException` 등을 확인합니다.
