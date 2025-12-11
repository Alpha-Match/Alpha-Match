# 🆔 UUID 생성 전략

*Python AI Server - UUID v7/ULID 생성 및 관리*

---

## 📋 목차

1. [UUID 생성 전략 개요](#1-uuid-생성-전략-개요)
2. [UUID v7 vs ULID 비교](#2-uuid-v7-vs-ulid-비교)
3. [UUID v7 구현](#3-uuid-v7-구현)
4. [ULID 구현](#4-ulid-구현)
5. [성능 비교](#5-성능-비교)
6. [Best Practices](#6-best-practices)

---

# 1. UUID 생성 전략 개요

## 1.1 왜 Python 서버에서 UUID를 생성하는가?

### AutoIncrement의 문제점

```python
# ❌ Bad: AutoIncrement 방식 (DB에서 생성)
# - DB에서 시퀀스 생성
# - 병렬 Insert 시 경합 발생
# - 대규모 처리 시 병목 현상

# ✅ Good: UUID 방식 (Python 서버에서 생성)
# - 클라이언트에서 UUID 생성
# - DB 경합 없음
# - 대규모 병렬 처리 가능
```

### 이점

1. **DB 경합 제거**
   - AutoIncrement 시퀀스 경합 없음
   - 대규모 병렬 Insert 안정성 확보

2. **시간순 정렬 보장**
   - UUID v7/ULID는 타임스탬프 기반
   - DB 인덱스 성능 최적화

3. **분산 시스템 친화적**
   - 여러 Python 서버 인스턴스에서 동시 생성 가능
   - 충돌 가능성 극히 낮음

---

# 2. UUID v7 vs ULID 비교

## 2.1 비교 표

| 특징 | UUID v7 | ULID |
|-----|---------|------|
| **표준** | ✅ RFC 9562 | ⚠️ 비표준 (사실상 표준) |
| **길이** | 36자 (하이픈 포함) | 26자 (Base32) |
| **형식** | `018c8d5e-7f8a-7000-8000-123456789abc` | `01H4XQJZQY5K3N7J9M8P6R4T2V` |
| **시간 정밀도** | 밀리초 (48비트) | 밀리초 (48비트) |
| **랜덤 비트** | 74비트 | 80비트 |
| **정렬 가능** | ✅ 시간순 | ✅ 시간순 |
| **DB 타입** | `UUID` | `VARCHAR(26)` |
| **Python 라이브러리** | `uuid6` | `python-ulid` |
| **인덱싱 성능** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **가독성** | 🟡 보통 | ✅ 좋음 (대소문자 구분 없음) |
| **권장도** | ✅ **권장** | 🟡 대안 |

## 2.2 선택 기준

### UUID v7을 선택해야 하는 경우 ✅

- PostgreSQL의 표준 UUID 타입 사용
- RFC 표준 준수 필요
- Java Batch Server와의 호환성 (Java UUID 타입)
- 장기적인 표준 지원 필요

### ULID를 선택해야 하는 경우

- 더 짧은 문자열 선호 (URL 친화적)
- Base32 인코딩 선호 (대소문자 구분 없음)
- 가독성 우선

**본 프로젝트 선택:** UUID v7 (PostgreSQL UUID 타입 호환성)

---

# 3. UUID v7 구현

## 3.1 라이브러리 설치

```bash
pip install uuid6
```

## 3.2 기본 사용법

```python
from uuid6 import uuid7

# UUID v7 생성
id = uuid7()
print(id)  # UUID 객체: UUID('018c8d5e-7f8a-7000-8000-123456789abc')

# 문자열로 변환
id_str = str(id)
print(id_str)  # '018c8d5e-7f8a-7000-8000-123456789abc'
```

## 3.3 DataFrame에 UUID 추가

```python
import pandas as pd
from uuid6 import uuid7

def add_uuid_v7_to_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    """
    DataFrame의 각 행에 UUID v7을 추가

    Args:
        df: 원본 DataFrame

    Returns:
        UUID 컬럼이 추가된 DataFrame
    """
    # UUID v7 생성 (각 행마다)
    df['id'] = [str(uuid7()) for _ in range(len(df))]

    # 또는 apply 사용
    # df['id'] = df.apply(lambda x: str(uuid7()), axis=1)

    return df

# 사용 예시
df = pd.DataFrame({
    'company_name': ['Company A', 'Company B'],
    'exp_years': [5, 3],
    'english_level': ['Advanced', 'Intermediate'],
    'primary_keyword': ['Backend', 'Frontend'],
    'job_post_vectors': [np.random.rand(1536), np.random.rand(1536)]
})

df = add_uuid_v7_to_dataframe(df)
print(df['id'].head())
```

## 3.4 시간순 정렬 검증

```python
import time

# UUID v7 여러 개 생성
uuids = []
for i in range(5):
    uuids.append(str(uuid7()))
    time.sleep(0.01)  # 10ms 대기

# 출력
for i, uuid_str in enumerate(uuids):
    print(f"{i+1}. {uuid_str}")

# 정렬 검증
sorted_uuids = sorted(uuids)
assert uuids == sorted_uuids, "UUID v7은 시간순으로 정렬되어야 합니다"
print("✅ UUID v7 시간순 정렬 검증 완료")
```

## 3.5 타임스탬프 추출

```python
from uuid6 import uuid7
from datetime import datetime

def extract_timestamp_from_uuid7(uuid_str: str) -> datetime:
    """
    UUID v7에서 타임스탬프 추출

    Args:
        uuid_str: UUID v7 문자열

    Returns:
        datetime 객체
    """
    # UUID 객체로 변환
    uuid_obj = UUID(uuid_str)

    # UUID v7의 첫 48비트는 Unix timestamp (milliseconds)
    # UUID의 time 필드 추출
    timestamp_ms = (uuid_obj.time >> 16) & 0xFFFFFFFFFFFF

    # datetime으로 변환
    return datetime.fromtimestamp(timestamp_ms / 1000.0)

# 사용 예시
id = str(uuid7())
print(f"UUID: {id}")

timestamp = extract_timestamp_from_uuid7(id)
print(f"Timestamp: {timestamp}")
```

---

# 4. ULID 구현

## 4.1 라이브러리 설치

```bash
pip install python-ulid
```

## 4.2 기본 사용법

```python
from ulid import ULID

# ULID 생성
id = ULID()
print(id)  # ULID 객체: ULID('01H4XQJZQY5K3N7J9M8P6R4T2V')

# 문자열로 변환
id_str = str(id)
print(id_str)  # '01H4XQJZQY5K3N7J9M8P6R4T2V'

# UUID로 변환 (DB 저장용)
uuid_obj = id.uuid
print(uuid_obj)  # UUID('018c8d5e-7f8a-4000-8000-123456789abc')
```

## 4.3 DataFrame에 ULID 추가

```python
import pandas as pd
from ulid import ULID

def add_ulid_to_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    """
    DataFrame의 각 행에 ULID를 추가

    Args:
        df: 원본 DataFrame

    Returns:
        ULID 컬럼이 추가된 DataFrame
    """
    # ULID 생성 (각 행마다)
    df['id'] = [str(ULID()) for _ in range(len(df))]

    return df
```

## 4.4 ULID → UUID 변환

```python
from ulid import ULID
from uuid import UUID

def ulid_to_uuid(ulid_str: str) -> str:
    """
    ULID 문자열을 UUID 문자열로 변환

    Args:
        ulid_str: ULID 문자열

    Returns:
        UUID 문자열
    """
    ulid_obj = ULID.from_str(ulid_str)
    uuid_obj = ulid_obj.uuid
    return str(uuid_obj)

# 사용 예시
ulid_str = str(ULID())
uuid_str = ulid_to_uuid(ulid_str)

print(f"ULID: {ulid_str}")
print(f"UUID: {uuid_str}")
```

---

# 5. 성능 비교

## 5.1 생성 속도 벤치마크

```python
import time
from uuid6 import uuid7
from ulid import ULID
import uuid

def benchmark_uuid_generation(count: int = 100000):
    """UUID 생성 성능 비교"""

    # UUID v4 (랜덤)
    start = time.time()
    for _ in range(count):
        uuid.uuid4()
    uuid4_time = time.time() - start

    # UUID v7
    start = time.time()
    for _ in range(count):
        uuid7()
    uuid7_time = time.time() - start

    # ULID
    start = time.time()
    for _ in range(count):
        ULID()
    ulid_time = time.time() - start

    print(f"UUID v4: {uuid4_time:.4f}s ({count/uuid4_time:.0f} ops/sec)")
    print(f"UUID v7: {uuid7_time:.4f}s ({count/uuid7_time:.0f} ops/sec)")
    print(f"ULID:    {ulid_time:.4f}s ({count/ulid_time:.0f} ops/sec)")

# 실행
benchmark_uuid_generation()

# 예상 결과 (시스템마다 다름):
# UUID v4: 0.1234s (810000 ops/sec)
# UUID v7: 0.1456s (687000 ops/sec)
# ULID:    0.1789s (559000 ops/sec)
```

## 5.2 정렬 성능 벤치마크

```python
import random

def benchmark_sorting(count: int = 100000):
    """UUID 정렬 성능 비교"""

    # UUID v4 생성 및 정렬
    uuid4_list = [str(uuid.uuid4()) for _ in range(count)]
    random.shuffle(uuid4_list)
    start = time.time()
    sorted(uuid4_list)
    uuid4_sort_time = time.time() - start

    # UUID v7 생성 및 정렬
    uuid7_list = [str(uuid7()) for _ in range(count)]
    random.shuffle(uuid7_list)
    start = time.time()
    sorted(uuid7_list)
    uuid7_sort_time = time.time() - start

    # ULID 생성 및 정렬
    ulid_list = [str(ULID()) for _ in range(count)]
    random.shuffle(ulid_list)
    start = time.time()
    sorted(ulid_list)
    ulid_sort_time = time.time() - start

    print(f"UUID v4 정렬: {uuid4_sort_time:.4f}s")
    print(f"UUID v7 정렬: {uuid7_sort_time:.4f}s")
    print(f"ULID 정렬:    {ulid_sort_time:.4f}s")

# 실행
benchmark_sorting()
```

## 5.3 메모리 사용량 비교

```python
import sys

# 각 UUID 타입의 메모리 사용량
uuid4_obj = uuid.uuid4()
uuid7_obj = uuid7()
ulid_obj = ULID()

print(f"UUID v4 객체: {sys.getsizeof(uuid4_obj)} bytes")
print(f"UUID v7 객체: {sys.getsizeof(uuid7_obj)} bytes")
print(f"ULID 객체:    {sys.getsizeof(ulid_obj)} bytes")

# 문자열 변환 시
uuid4_str = str(uuid4_obj)
uuid7_str = str(uuid7_obj)
ulid_str = str(ulid_obj)

print(f"\nUUID v4 문자열: {sys.getsizeof(uuid4_str)} bytes ({len(uuid4_str)} chars)")
print(f"UUID v7 문자열: {sys.getsizeof(uuid7_str)} bytes ({len(uuid7_str)} chars)")
print(f"ULID 문자열:    {sys.getsizeof(ulid_str)} bytes ({len(ulid_str)} chars)")

# 예상 결과:
# UUID v4 문자열: 85 bytes (36 chars)
# UUID v7 문자열: 85 bytes (36 chars)
# ULID 문자열:    75 bytes (26 chars)  ← 더 짧음!
```

---

# 6. Best Practices

## 6.1 pkl 파일에 UUID 추가

### 기존 pkl 파일에 UUID 추가

```python
import pandas as pd
from uuid6 import uuid7

def add_uuid_to_existing_pkl(
    input_path: str,
    output_path: str,
    uuid_type: str = 'uuid7'
):
    """
    기존 pkl 파일에 UUID를 추가하여 새로운 pkl 파일로 저장

    Args:
        input_path: 입력 pkl 파일 경로
        output_path: 출력 pkl 파일 경로
        uuid_type: 'uuid7' 또는 'ulid'
    """
    # pkl 파일 로드
    df = pd.read_pickle(input_path)
    print(f"Loaded {len(df)} rows from {input_path}")

    # UUID 추가
    if uuid_type == 'uuid7':
        df['id'] = [str(uuid7()) for _ in range(len(df))]
    elif uuid_type == 'ulid':
        df['id'] = [str(ULID()) for _ in range(len(df))]
    else:
        raise ValueError(f"Unknown uuid_type: {uuid_type}")

    # UUID를 첫 번째 컬럼으로 이동
    cols = ['id'] + [col for col in df.columns if col != 'id']
    df = df[cols]

    # 저장
    df.to_pickle(output_path)
    print(f"Saved {len(df)} rows to {output_path}")

    # 검증
    df_check = pd.read_pickle(output_path)
    print(f"Verification: {len(df_check)} rows loaded")
    print(f"First UUID: {df_check['id'].iloc[0]}")

# 사용 예시
add_uuid_to_existing_pkl(
    input_path='data/processed_recruitment_data.pkl',
    output_path='data/processed_recruitment_data_with_uuid.pkl',
    uuid_type='uuid7'
)
```

## 6.2 gRPC Streaming 시 UUID 생성

### 옵션 1: pkl 파일에 미리 UUID 추가 (권장)

```python
def prepare_data_with_uuid():
    """사전에 pkl 파일에 UUID 추가"""
    df = pd.read_pickle('data/raw_recruitment_data.pkl')

    # UUID v7 추가
    df['id'] = [str(uuid7()) for _ in range(len(df))]

    # 저장
    df.to_pickle('data/processed_recruitment_data.pkl')
    print(f"UUID added to {len(df)} rows")

# 한 번만 실행
prepare_data_with_uuid()
```

### 옵션 2: Streaming 시 UUID 생성 (비권장)

```python
def GetEmbeddings(self, request, context):
    """Streaming 시 UUID 생성 (성능 저하 가능성)"""
    df = pd.read_pickle('data/recruitment_data.pkl')

    for chunk_df in chunker(df, chunk_size):
        for _, row in chunk_df.iterrows():
            # ⚠️ 매번 UUID 생성 (성능 저하)
            embedding = embedding_stream_pb2.Embedding(
                id=str(uuid7()),  # ← 여기서 생성
                company_name=row['company_name'],
                # ...
            )
            yield embedding
```

**권장:** pkl 파일에 미리 UUID를 추가하고, Streaming 시에는 기존 UUID를 사용

## 6.3 UUID 충돌 방지

```python
def check_uuid_uniqueness(df: pd.DataFrame):
    """DataFrame의 UUID 중복 검사"""
    duplicates = df[df.duplicated(subset=['id'], keep=False)]

    if len(duplicates) > 0:
        print(f"⚠️ Warning: {len(duplicates)} duplicate UUIDs found!")
        print(duplicates[['id']])
        return False
    else:
        print("✅ All UUIDs are unique")
        return True

# 사용 예시
df = pd.read_pickle('data/processed_recruitment_data.pkl')
check_uuid_uniqueness(df)
```

## 6.4 Checkpoint를 위한 UUID 필터링

```python
def filter_by_last_processed_uuid(
    df: pd.DataFrame,
    last_processed_uuid: str
) -> pd.DataFrame:
    """
    마지막 처리된 UUID 이후의 데이터만 필터링

    Args:
        df: 원본 DataFrame
        last_processed_uuid: 마지막 처리된 UUID

    Returns:
        필터링된 DataFrame
    """
    # UUID v7/ULID는 문자열 비교로 시간순 정렬 가능
    filtered_df = df[df['id'] > last_processed_uuid]

    print(f"Filtered: {len(filtered_df)}/{len(df)} rows")
    return filtered_df

# 사용 예시
df = pd.read_pickle('data/processed_recruitment_data.pkl')
filtered_df = filter_by_last_processed_uuid(
    df,
    last_processed_uuid='018c8d5e-7f8a-7000-8000-123456789abc'
)
```

---

# 7. 트러블슈팅

## 7.1 UUID v7 설치 오류

### 문제
```bash
ERROR: Could not find a version that satisfies the requirement uuid6
```

### 해결
```bash
# 최신 pip으로 업그레이드
pip install --upgrade pip

# uuid6 설치
pip install uuid6

# 또는 특정 버전 지정
pip install uuid6==2024.1.12
```

## 7.2 ULID 설치 오류

### 문제
```bash
ERROR: Could not find a version that satisfies the requirement python-ulid
```

### 해결
```bash
# 정확한 패키지명 사용
pip install python-ulid

# 또는
pip install ulid-py
```

## 7.3 UUID 문자열 변환 오류

### 문제
```python
# TypeError: Object of type UUID is not JSON serializable
import json
uuid_obj = uuid7()
json.dumps({"id": uuid_obj})  # ❌ 에러
```

### 해결
```python
# ✅ 문자열로 변환
uuid_str = str(uuid_obj)
json.dumps({"id": uuid_str})  # 정상 작동
```

---

# 8. 요약

## 8.1 핵심 포인트

1. **Python 서버에서 UUID 생성**
   - DB 경합 제거
   - 대규모 병렬 처리 가능

2. **UUID v7 권장**
   - PostgreSQL UUID 타입 호환
   - RFC 표준 준수
   - 시간순 정렬 보장

3. **pkl 파일에 미리 UUID 추가**
   - Streaming 성능 최적화
   - 일관성 보장

4. **Checkpoint 지원**
   - `last_processed_uuid` 활용
   - 문자열 비교로 필터링 가능

## 8.2 체크리스트

- [ ] uuid6 라이브러리 설치
- [ ] pkl 파일에 UUID v7 추가
- [ ] UUID 중복 검사
- [ ] gRPC 메시지에 UUID 포함
- [ ] Checkpoint 로직 구현
- [ ] UUID 성능 벤치마크

---

**최종 수정일:** 2025-12-11
