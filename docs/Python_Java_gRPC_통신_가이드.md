# Python ↔ Java 양방향 gRPC 통신 가이드

**작성일:** 2025-12-12
**버전:** 1.0
**대상:** Alpha-Match 프로젝트 (Demo-Python ↔ Backend/Batch-Server)

---

## 📋 목차

1. [개요](#1-개요)
2. [gRPC 통신 패턴](#2-grpc-통신-패턴)
3. [Proto 파일 상세 설명](#3-proto-파일-상세-설명)
4. [Python 측 구현 (제네릭 패턴)](#4-python-측-구현-제네릭-패턴)
5. [Java 측 구현 (제네릭 패턴)](#5-java-측-구현-제네릭-패턴)
6. [데이터 플로우](#6-데이터-플로우)
7. [용어 설명](#7-용어-설명)
8. [수도 코드](#8-수도-코드)
9. [확장 방법](#9-확장-방법)
10. [트러블슈팅](#10-트러블슈팅)

---

## 1. 개요

### 1.1 목적

Alpha-Match 프로젝트에서 Python AI 서버와 Java Batch 서버 간의 대용량 Embedding 데이터를 효율적으로 전송하기 위한 gRPC 기반 통신 시스템입니다.

### 1.2 핵심 요구사항

| 요구사항 | 설명 | 해결 방법 |
|---------|------|----------|
| **대용량 데이터 전송** | 141,897 rows (약 500MB) | gRPC Streaming |
| **메모리 효율성** | 한번에 전체 데이터 로드 불가 | Chunk 단위 분할 (300 rows) |
| **재시작 지원** | 네트워크 장애 시 이어서 전송 | Checkpoint (UUID 기반) |
| **도메인 확장성** | Recruit, Candidate 등 다양한 도메인 | Generic 패턴 + Factory |
| **양방향 통신** | Python → Java, Java → Python | Server + Client Streaming |

### 1.3 기술 스택

| 구분 | Python 측 | Java 측 |
|-----|----------|---------|
| **언어** | Python 3.11+ | Java 21 |
| **Framework** | FastAPI + gRPC | Spring Boot 4.0 + gRPC |
| **gRPC** | grpcio 1.60.0 | grpc-spring-boot-starter |
| **데이터 처리** | Pandas + NumPy | Jackson + JPA |
| **스트림 처리** | AsyncIO | Project Reactor (WebFlux) |

---

## 2. gRPC 통신 패턴

### 2.1 양방향 통신 구조

```
┌─────────────────┐                  ┌──────────────────┐
│  Python Server  │                  │  Java Batch      │
│  (Port 50051)   │                  │  Server          │
└─────────────────┘                  └──────────────────┘
         │                                    │
         │  ┌──────────────────────────────┐  │
         │  │  1. Server Streaming RPC     │  │
         │  │  StreamEmbedding()           │  │
         │◄─┼──────────────────────────────┼──┤
         │  │  Request: last_uuid, size    │  │
         ├──┼──────────────────────────────┼─►│
         │  │  Response: stream RowChunk   │  │
         │  └──────────────────────────────┘  │
         │                                    │
         │  ┌──────────────────────────────┐  │
         │  │  2. Client Streaming RPC     │  │
         │  │  IngestDataStream()          │  │
         ├──┼──────────────────────────────┼─►│
         │  │  Request: stream Ingest..    │  │
         │◄─┼──────────────────────────────┼──┤
         │  │  Response: IngestDataResp..  │  │
         │  └──────────────────────────────┘  │
         │                                    │
```

### 2.2 Server Streaming (Pattern 1)

**목적:** Java Batch Server가 요청하면 Python이 데이터를 스트리밍 전송

**흐름:**
```
Java → Python: StreamEmbeddingRequest
                (last_processed_uuid, chunk_size)

Python → Java: RowChunk (300 rows)
Python → Java: RowChunk (300 rows)
Python → Java: RowChunk (300 rows)
...
Python → Java: RowChunk (97 rows) [마지막]
```

**사용 사례:**
- 정기 배치 작업 (Scheduler)
- 전체 데이터 동기화
- Checkpoint 기반 재시작

### 2.3 Client Streaming (Pattern 2)

**목적:** Python이 데이터를 수집하여 Java로 전송 (FastAPI 엔드포인트 트리거)

**흐름:**
```
Python → Java: IngestDataRequest (metadata)
                (domain="recruit", vector_dimension=384)

Python → Java: IngestDataRequest (chunk 1)
Python → Java: IngestDataRequest (chunk 2)
Python → Java: IngestDataRequest (chunk 3)
...
Python → Java: IngestDataRequest (chunk 474)

Java → Python: IngestDataResponse
                (success=true, received_chunks=474)
```

**사용 사례:**
- 사용자 요청 트리거 (`POST /ingest/recruit`)
- 실시간 데이터 업로드
- 도메인별 독립 전송

---

## 3. Proto 파일 상세 설명

### 3.1 전체 구조

```protobuf
syntax = "proto3";
package com.alpha.backend.grpc;

service EmbeddingStreamService {
  // Pattern 1: Server Streaming
  rpc StreamEmbedding(StreamEmbeddingRequest) returns (stream RowChunk);

  // Pattern 2: Client Streaming
  rpc IngestDataStream(stream IngestDataRequest) returns (IngestDataResponse);
}
```

### 3.2 Server Streaming 메시지

#### StreamEmbeddingRequest

```protobuf
message StreamEmbeddingRequest {
  optional string last_processed_uuid = 1;  // Checkpoint UUID
  optional int32 chunk_size = 2;            // 청크 크기 (기본 300)
}
```

**필드 설명:**
- `last_processed_uuid`: 마지막으로 처리된 데이터의 UUID
  - **없으면**: 처음부터 전송
  - **있으면**: 해당 UUID 이후 데이터만 전송
  - **타입**: UUID v7 (시간순 정렬 보장)
- `chunk_size`: 한 번에 전송할 Row 개수
  - **0이면**: 서버 기본값 사용 (300)
  - **범위**: 100~1000 권장

#### RowChunk

```protobuf
message RowChunk {
  repeated RecruitRow rows = 1;  // Row 리스트
}
```

**필드 설명:**
- `rows`: RecruitRow 배열 (1~1000개)
  - **메모리 효율**: 전체 데이터를 한번에 보내지 않음
  - **네트워크 효율**: 적절한 크기로 분할

#### RecruitRow

```protobuf
message RecruitRow {
  string id = 1;                 // UUID v7
  string company_name = 2;       // 회사명
  int32 exp_years = 3;           // 경력 연수
  string english_level = 4;      // 영어 수준
  string primary_keyword = 5;    // 주요 키워드
  repeated float vector = 6;     // Embedding Vector (384차원)
}
```

**필드 설명:**
- `id`: 고유 식별자
  - **포맷**: UUID v7 (예: `"c0ca96e7-85df-50df-a64e-d934cd02a170"`)
  - **생성 위치**: Python 서버
  - **정렬 보장**: 시간순 정렬 가능
- `vector`: Embedding 벡터
  - **차원**: 384 (설정 가능)
  - **타입**: float32 (Python) → float (Proto) → float (Java)
  - **크기**: 384 * 4 bytes = 1.5KB per row

### 3.3 Client Streaming 메시지

#### IngestDataRequest

```protobuf
message IngestDataRequest {
  oneof request_type {
    IngestMetadata metadata = 1;   // 첫 번째 메시지
    bytes data_chunk = 2;           // 이후 메시지들
  }
}
```

**필드 설명:**
- `oneof`: 두 필드 중 하나만 설정 가능
  - **첫 요청**: `metadata` 설정 (도메인 정보)
  - **이후 요청**: `data_chunk` 설정 (실제 데이터)

#### IngestMetadata

```protobuf
message IngestMetadata {
  string domain = 1;           // 도메인 이름
  string file_name = 2;        // 파일 이름
  int32 vector_dimension = 3;  // 벡터 차원
}
```

**필드 설명:**
- `domain`: 데이터 도메인 식별자
  - **예시**: `"recruit"`, `"candidate"`
  - **용도**: Factory에서 적절한 Processor 선택
- `file_name`: 소스 파일 이름
  - **예시**: `"processed_recruitment_data.pkl"`
  - **용도**: 로깅 및 추적
- `vector_dimension`: Embedding 벡터 차원
  - **예시**: `384`
  - **용도**: 벡터 유효성 검증

#### data_chunk (bytes)

**포맷:** JSON 배열을 UTF-8로 인코딩한 바이트 배열

```json
[
  {
    "id": "c0ca96e7-85df-50df-a64e-d934cd02a170",
    "company_name": "MyCointainer",
    "exp_years": 2,
    "english_level": "intermediate",
    "primary_keyword": "Sysadmin",
    "vector": [0.123, 0.456, ..., 0.789]
  },
  ...
]
```

**인코딩 과정 (Python):**
```python
# 1. Pydantic 모델 리스트 → JSON 문자열 리스트
json_chunk = [item.model_dump_json() for item in data_chunk]

# 2. JSON 리스트를 하나의 JSON 배열로 통합
json_array = json.dumps(json_chunk)

# 3. UTF-8 바이트로 인코딩
encoded_chunk = json_array.encode('utf-8')
```

**디코딩 과정 (Java):**
```java
// 1. bytes → UTF-8 문자열
String jsonChunk = new String(chunk, StandardCharsets.UTF_8);

// 2. JSON 파싱 → DTO 리스트
List<RecruitRowDto> dtos = objectMapper.readValue(
    jsonChunk,
    new TypeReference<List<RecruitRowDto>>() {}
);
```

#### IngestDataResponse

```protobuf
message IngestDataResponse {
  bool success = 1;            // 성공 여부
  int32 received_chunks = 2;   // 수신한 청크 개수
  string message = 3;          // 메시지
}
```

**필드 설명:**
- `success`: 전체 스트리밍 성공 여부
- `received_chunks`: 수신한 data_chunk 메시지 개수
  - **예시**: 474 (141,897 rows / 300 per chunk)
- `message`: 상세 메시지
  - **성공 예시**: `"Successfully ingested 474 chunks"`
  - **실패 예시**: `"Failed at chunk 123: Invalid vector dimension"`

---

## 4. Python 측 구현 (제네릭 패턴)

### 4.1 아키텍처 개요

Python은 **Protocol + Generic + Factory** 패턴을 사용하여 도메인별 데이터 처리를 추상화합니다.

```
┌──────────────────────────────────────────┐
│         FastAPI Endpoint Layer           │
│  POST /data/ingest/{domain}              │
└───────────────┬──────────────────────────┘
                │
        ┌───────▼────────┐
        │  Service Layer │
        │  ingestion_    │
        │  service.py    │
        └───────┬────────┘
                │
        ┌───────▼────────────────────┐
        │   get_loader(domain)       │
        │   DataLoader Protocol      │
        │   (Generic Pattern)        │
        └───────┬────────────────────┘
                │
    ┌───────────┴───────────┐
    │                       │
┌───▼───────────┐   ┌──────▼──────────┐
│ PklRecruitL.. │   │ PklCandidateL.. │
│ Loader        │   │ Loader          │
│ [RecruitData] │   │ [CandidateData] │
└───────┬───────┘   └────────┬────────┘
        │                    │
        └────────┬───────────┘
                 │
         ┌───────▼────────┐
         │  gRPC Client   │
         │  Streaming     │
         └────────────────┘
```

### 4.2 제네릭 타입 정의

#### TypeVar와 공변성

```python
from typing import TypeVar, Protocol, List

# 공변성(covariant) 타입 변수 정의
T_Row = TypeVar('T_Row', bound=BaseData, covariant=True)
```

**용어 설명:**
- `TypeVar`: 제네릭 타입 변수
- `bound=BaseData`: BaseData를 상속한 타입만 허용
- `covariant=True`: 공변성 설정
  - `DataLoader[RecruitData]` ⊆ `DataLoader[BaseData]`
  - 하위 타입을 상위 타입 자리에 사용 가능

**공변성 (Covariance) 이란?**

```python
# BaseData (상위 타입)
class BaseData(BaseModel):
    id: str

# RecruitData (하위 타입)
class RecruitData(BaseData):
    company_name: str
    vector: List[float]

# 공변성이 있으면 이것이 가능:
loader_recruit: DataLoader[RecruitData] = PklRecruitLoader()
loader_base: DataLoader[BaseData] = loader_recruit  # ✅ OK
```

**왜 공변성이 필요한가?**
- Factory에서 구체적인 로더를 반환하되, 인터페이스는 추상 타입으로 선언 가능
- 타입 안전성을 유지하면서 유연한 코드 작성

### 4.3 Protocol 기반 인터페이스

```python
class DataLoader(Protocol[T_Row]):
    """
    데이터 로더 프로토콜 (인터페이스)
    Python은 명시적 상속 없이 구조적 서브타이핑(Duck Typing)을 지원
    """
    def load(self, file_path: str) -> List[T_Row]:
        """파일에서 데이터를 로드하여 모델 리스트로 변환"""
        ...
```

**Protocol vs ABC (Abstract Base Class):**

| 특징 | Protocol | ABC |
|-----|---------|-----|
| **상속 필요** | ❌ 불필요 | ✅ 필요 |
| **타입 검사** | 구조적 (Duck Typing) | 명목적 (Explicit) |
| **유연성** | 높음 | 낮음 |
| **Python 스타일** | Pythonic | Java-like |

**Protocol 예시:**
```python
# Protocol 구현 - 명시적 상속 없음
class PklRecruitLoader:  # DataLoader[RecruitData]를 상속하지 않음
    def load(self, file_path: str) -> List[RecruitData]:
        # 구현...
        return data

# 하지만 타입 체커는 Protocol을 만족한다고 인식
loader: DataLoader[RecruitData] = PklRecruitLoader()  # ✅ OK
```

### 4.4 구체적 로더 구현

#### Recruit 도메인 로더

```python
class PklRecruitLoader(DataLoader[RecruitData]):
    """
    Recruit 도메인 pkl 파일 로더
    DataLoader[RecruitData] Protocol을 구현
    """
    def load(self, file_path: str) -> List[RecruitData]:
        # 1. Pandas로 pkl 파일 읽기
        df = pd.read_pickle(file_path)

        # 2. 각 행을 Pydantic 모델로 변환
        return [RecruitData(**row) for row in df.to_dict('records')]
```

**동작 과정:**
1. `pd.read_pickle()`: pkl 파일 → DataFrame
2. `df.to_dict('records')`: DataFrame → 딕셔너리 리스트
   ```python
   [
       {'id': '...', 'company_name': '...', 'vector': [...]},
       ...
   ]
   ```
3. `RecruitData(**row)`: 딕셔너리 → Pydantic 모델
   - 자동 타입 검증
   - 필드 누락/타입 불일치 시 에러

#### Candidate 도메인 로더 (스켈레톤)

```python
class PklCandidateLoader(DataLoader[CandidateData]):
    """
    Candidate 도메인 pkl 파일 로더 (미구현)
    """
    def load(self, file_path: str) -> List[CandidateData]:
        # TODO: 구현 필요
        return []
```

### 4.5 Factory 패턴

```python
# 도메인 → 로더 매핑 레지스트리
_loader_registry: Dict[str, DataLoader] = {
    "recruit": PklRecruitLoader(),
    "candidate": PklCandidateLoader(),
}

def get_loader(domain: str) -> DataLoader:
    """
    도메인 이름으로 적절한 로더를 반환하는 팩토리 함수

    Args:
        domain: 도메인 이름 (예: "recruit")

    Returns:
        DataLoader 인스턴스

    Raises:
        ValueError: 지원하지 않는 도메인
    """
    loader = _loader_registry.get(domain)
    if loader is None:
        raise ValueError(f"지원하지 않는 도메인: '{domain}'")
    return loader
```

**사용 예시:**
```python
# 런타임에 도메인 선택
domain = "recruit"  # 사용자 요청에서 추출
loader = get_loader(domain)  # PklRecruitLoader 반환
data = loader.load("data/recruit.pkl")  # List[RecruitData]
```

### 4.6 gRPC Client Streaming 구현

```python
async def stream_data_to_batch_server(
    domain: str,
    file_name: str,
    data: List[RecruitData],
    chunk_size: int = 100
) -> IngestDataResponse:
    """
    Batch Server로 데이터를 Client Streaming으로 전송
    """
    vector_dimension = len(data[0].vector) if data else 0

    # gRPC 채널 생성
    async with grpc.aio.insecure_channel(BATCH_SERVER_ADDRESS) as channel:
        stub = embedding_stream_pb2_grpc.EmbeddingStreamServiceStub(channel)

        # 비동기 제너레이터로 요청 스트림 생성
        async def request_generator():
            # 1. 첫 번째 요청: 메타데이터
            metadata = embedding_stream_pb2.IngestMetadata(
                domain=domain,
                file_name=file_name,
                vector_dimension=vector_dimension
            )
            yield embedding_stream_pb2.IngestDataRequest(metadata=metadata)

            # 2. 이후 요청들: 데이터 청크
            for i in range(0, len(data), chunk_size):
                chunk_data = data[i:i + chunk_size]

                # Pydantic 모델 → JSON → bytes
                json_chunk = [item.model_dump_json() for item in chunk_data]
                encoded_chunk = json.dumps(json_chunk).encode('utf-8')

                yield embedding_stream_pb2.IngestDataRequest(
                    data_chunk=encoded_chunk
                )

        # gRPC 호출
        response = await stub.IngestDataStream(request_generator())
        return response
```

**핵심 개념:**

1. **비동기 제너레이터 (Async Generator)**
   ```python
   async def request_generator():
       yield request1
       yield request2
       ...
   ```
   - `yield`로 요청을 하나씩 생성
   - gRPC가 Backpressure 처리 (네트워크 속도에 맞춰 전송)

2. **oneof 처리**
   ```python
   # 첫 요청: metadata 설정
   IngestDataRequest(metadata=metadata)

   # 이후 요청: data_chunk 설정
   IngestDataRequest(data_chunk=encoded_chunk)
   ```

3. **JSON 직렬화**
   ```python
   # Pydantic 모델 → JSON 문자열
   json_chunk = [item.model_dump_json() for item in chunk_data]

   # JSON 리스트 → JSON 배열 문자열
   json_array = json.dumps(json_chunk)

   # 문자열 → bytes
   encoded = json_array.encode('utf-8')
   ```

---

## 5. Java 측 구현 (제네릭 패턴)

### 5.1 아키텍처 개요

Java는 **Generic Interface + Factory + Spring Bean** 패턴을 사용합니다.

```
┌────────────────────────────────────────┐
│       gRPC Server Layer                │
│  EmbeddingStreamServiceImpl            │
│  (IngestDataStream RPC)                │
└───────────────┬────────────────────────┘
                │
        ┌───────▼────────┐
        │  Metadata 수신 │
        │  domain 추출   │
        └───────┬────────┘
                │
        ┌───────▼───────────────────┐
        │  DataProcessorFactory     │
        │  getProcessor(domain)     │
        │  (Generic Pattern)        │
        └───────┬───────────────────┘
                │
    ┌───────────┴───────────┐
    │                       │
┌───▼────────────┐  ┌──────▼───────────┐
│ RecruitData    │  │ CandidateData    │
│ Processor      │  │ Processor        │
│ <RecruitMeta>  │  │ <CandidateMeta>  │
└───────┬────────┘  └──────┬───────────┘
        │                  │
        └────────┬─────────┘
                 │
         ┌───────▼────────┐
         │   JPA Repository│
         │   PostgreSQL   │
         └────────────────┘
```

### 5.2 제네릭 인터페이스

```java
public interface DataProcessor<T> {

    /**
     * JSON 청크를 파싱하여 엔티티 리스트로 변환
     *
     * @param jsonChunk JSON 인코딩된 바이트 배열
     * @return 파싱된 엔티티 리스트
     */
    List<T> parseChunk(byte[] jsonChunk);

    /**
     * 엔티티 리스트를 데이터베이스에 저장
     *
     * @param entities 저장할 엔티티 리스트
     */
    void saveToDatabase(List<T> entities);

    /**
     * 지원하는 도메인 이름 반환
     *
     * @return 도메인 이름 (예: "recruit")
     */
    String getDomain();
}
```

**제네릭 타입 `<T>`:**
- Python의 `TypeVar`와 동일한 역할
- 도메인별 엔티티 타입을 파라미터화
- 예: `DataProcessor<MetadataEntity>`, `DataProcessor<CandidateMetadata>`

### 5.3 구체적 프로세서 구현

#### Recruit 도메인 프로세서

```java
@Component
public class RecruitDataProcessor implements DataProcessor<MetadataEntity> {

    private final ObjectMapper objectMapper;
    private final MetadataRepository metadataRepository;
    private final EmbeddingRepository embeddingRepository;

    @Override
    public List<MetadataEntity> parseChunk(byte[] jsonChunk) {
        try {
            // 1. bytes → UTF-8 문자열
            String jsonString = new String(jsonChunk, StandardCharsets.UTF_8);

            // 2. JSON → DTO 리스트
            List<RecruitRowDto> dtos = objectMapper.readValue(
                jsonString,
                new TypeReference<List<RecruitRowDto>>() {}
            );

            // 3. DTO → Entity 변환
            return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }

    @Override
    public void saveToDatabase(List<MetadataEntity> entities) {
        // Metadata 저장
        metadataRepository.saveAll(entities);

        // Embedding 저장 (FK 참조)
        List<EmbeddingEntity> embeddings = entities.stream()
            .map(this::toEmbedding)
            .collect(Collectors.toList());
        embeddingRepository.saveAll(embeddings);
    }

    @Override
    public String getDomain() {
        return "recruit";
    }

    // DTO → Entity 변환
    private MetadataEntity toEntity(RecruitRowDto dto) {
        MetadataEntity entity = new MetadataEntity();
        entity.setUuid(UUID.fromString(dto.getId()));
        entity.setCompanyName(dto.getCompanyName());
        entity.setExpYears(dto.getExpYears());
        entity.setEnglishLevel(dto.getEnglishLevel());
        entity.setPrimaryKeyword(dto.getPrimaryKeyword());
        return entity;
    }

    private EmbeddingEntity toEmbedding(MetadataEntity metadata) {
        EmbeddingEntity embedding = new EmbeddingEntity();
        embedding.setMetadata(metadata);
        embedding.setVector(metadata.getVector());  // DTO에서 임시 저장된 vector
        return embedding;
    }
}
```

**핵심 단계:**

1. **JSON 파싱**
   ```java
   // TypeReference를 사용하여 제네릭 타입 정보 유지
   new TypeReference<List<RecruitRowDto>>() {}
   ```

2. **DTO → Entity 변환**
   - DTO: 네트워크 전송용 데이터 객체
   - Entity: JPA 엔티티 (DB 매핑)

3. **순서 보장 저장**
   ```java
   // 1. Metadata 먼저 (PK)
   metadataRepository.saveAll(entities);

   // 2. Embedding 나중에 (FK 참조)
   embeddingRepository.saveAll(embeddings);
   ```

### 5.4 Factory 패턴

```java
@Component
public class DataProcessorFactory {

    private final Map<String, DataProcessor<?>> processors;

    /**
     * Spring이 모든 DataProcessor 빈을 자동으로 주입
     */
    public DataProcessorFactory(List<DataProcessor<?>> processorList) {
        this.processors = processorList.stream()
            .collect(Collectors.toMap(
                DataProcessor::getDomain,
                Function.identity()
            ));
    }

    /**
     * 도메인 이름으로 적절한 프로세서 반환
     */
    public DataProcessor<?> getProcessor(String domain) {
        DataProcessor<?> processor = processors.get(domain);
        if (processor == null) {
            throw new IllegalArgumentException(
                "지원하지 않는 도메인: " + domain
            );
        }
        return processor;
    }
}
```

**Spring 자동 등록:**

```java
// Spring이 자동으로 다음을 수행:
// 1. @Component가 붙은 모든 DataProcessor 구현체를 찾음
// 2. 각각을 Bean으로 등록
// 3. DataProcessorFactory 생성자에 리스트로 주입
//
// 결과:
// processors = {
//     "recruit": RecruitDataProcessor,
//     "candidate": CandidateDataProcessor
// }
```

**사용 예시:**
```java
String domain = "recruit";  // metadata에서 추출
DataProcessor<?> processor = factory.getProcessor(domain);
List<?> entities = processor.parseChunk(jsonChunk);
processor.saveToDatabase(entities);
```

### 5.5 gRPC Server 구현

```java
@GrpcService
public class EmbeddingStreamServiceImpl
    extends EmbeddingStreamServiceGrpc.EmbeddingStreamServiceImplBase {

    private final DataProcessorFactory processorFactory;

    @Override
    public StreamObserver<IngestDataRequest> ingestDataStream(
        StreamObserver<IngestDataResponse> responseObserver) {

        return new StreamObserver<IngestDataRequest>() {
            private String domain;
            private DataProcessor<?> processor;
            private int receivedChunks = 0;

            @Override
            public void onNext(IngestDataRequest request) {
                try {
                    if (request.hasMetadata()) {
                        // 첫 번째 메시지: 메타데이터
                        IngestMetadata metadata = request.getMetadata();
                        domain = metadata.getDomain();

                        // Factory에서 프로세서 선택
                        processor = processorFactory.getProcessor(domain);

                        log.info("[METADATA] Domain: {}, Vector Dim: {}",
                            domain, metadata.getVectorDimension());

                    } else if (request.hasDataChunk()) {
                        // 이후 메시지: 데이터 청크
                        byte[] chunk = request.getDataChunk().toByteArray();

                        // JSON 파싱 → Entity 변환 → DB 저장
                        List<?> entities = processor.parseChunk(chunk);
                        processor.saveToDatabase(entities);

                        receivedChunks++;
                        log.info("[CHUNK] Received {} rows", entities.size());
                    }

                } catch (Exception e) {
                    log.error("[ERROR] Processing failed", e);
                    responseObserver.onError(
                        Status.INTERNAL
                            .withDescription(e.getMessage())
                            .asException()
                    );
                }
            }

            @Override
            public void onCompleted() {
                // 모든 청크 수신 완료
                IngestDataResponse response = IngestDataResponse.newBuilder()
                    .setSuccess(true)
                    .setReceivedChunks(receivedChunks)
                    .setMessage("Successfully ingested " + receivedChunks + " chunks")
                    .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable t) {
                log.error("[STREAM_ERROR]", t);
            }
        };
    }
}
```

**핵심 흐름:**

1. **첫 메시지 (메타데이터)**
   ```java
   domain = metadata.getDomain();  // "recruit"
   processor = factory.getProcessor(domain);  // RecruitDataProcessor
   ```

2. **이후 메시지들 (데이터 청크)**
   ```java
   byte[] chunk = request.getDataChunk().toByteArray();
   List<?> entities = processor.parseChunk(chunk);
   processor.saveToDatabase(entities);
   ```

3. **완료 시 응답**
   ```java
   IngestDataResponse response = ...;
   responseObserver.onNext(response);
   responseObserver.onCompleted();
   ```

---

## 6. 데이터 플로우

### 6.1 Client Streaming 전체 흐름

```
┌──────────────┐                           ┌─────────────────┐
│   사용자      │                           │  Python Server  │
│              │                           │  (FastAPI)      │
└──────┬───────┘                           └────────┬────────┘
       │                                            │
       │ POST /data/ingest/recruit                  │
       │ ?file_name=processed_recruitment_data.pkl  │
       ├───────────────────────────────────────────►│
       │                                            │
       │                                   ┌────────▼─────────┐
       │                                   │ 1. get_loader()  │
       │                                   │    domain="recruit"│
       │                                   │    → PklRecruitLoader│
       │                                   └────────┬─────────┘
       │                                            │
       │                                   ┌────────▼─────────┐
       │                                   │ 2. loader.load() │
       │                                   │    → 141,897 rows│
       │                                   └────────┬─────────┘
       │                                            │
       │                                   ┌────────▼──────────┐
       │                                   │ 3. gRPC Client    │
       │                                   │    Streaming      │
       │                                   │    시작           │
       │                                   └────────┬──────────┘
       │                                            │
       │                                            │ (1) IngestMetadata
       │                                            ├──────────────────┐
       │                                            │                  │
       │                                   ┌────────▼──────────┐       │
       │                                   │  Batch Server     │       │
       │                                   │  (Port 50051)     │       │
       │                                   │                   │       │
       │                                   │  [METADATA 수신]  │◄──────┤
       │                                   │  domain="recruit" │
       │                                   │  vector_dim=384   │
       │                                   │                   │
       │                                   │  Factory에서      │
       │                                   │  Processor 선택   │
       │                                   │  → RecruitData... │
       │                                   └────────┬──────────┘
       │                                            │
       │                                            │ (2) data_chunk #1
       │                                            ├──────────────────┐
       │                                            │                  │
       │                                   ┌────────▼──────────┐       │
       │                                   │  [CHUNK 처리]     │◄──────┤
       │                                   │  JSON → DTO       │
       │                                   │  DTO → Entity     │
       │                                   │  Entity → DB      │
       │                                   │  (300 rows)       │
       │                                   └────────┬──────────┘
       │                                            │
       │                                            │ (3) data_chunk #2~474
       │                                            │ ... 반복 ...
       │                                            │
       │                                   ┌────────▼──────────┐
       │                                   │  [완료]           │
       │                                   │  IngestData...    │
       │                                   │  success=true     │
       │                                   │  chunks=474       │
       │                                   └────────┬──────────┘
       │                                            │
       │                                            │ Response
       │                                   ┌────────▼──────────┐
       │                                   │  Python Server    │
       │                                   │  응답 수신        │
       │                                   └────────┬──────────┘
       │                                            │
       │ 200 OK                                     │
       │ {success: true, chunks: 474}               │
       │◄───────────────────────────────────────────┤
       │                                            │
```

### 6.2 메시지 시퀀스 상세

```
Python                                  Java Batch Server
  │                                           │
  │ [1] IngestDataRequest                     │
  │     metadata {                            │
  │       domain: "recruit"                   │
  │       file_name: "..."                    │
  │       vector_dimension: 384               │
  │     }                                     │
  ├──────────────────────────────────────────►│
  │                                           │ domain 식별
  │                                           │ Factory에서 Processor 선택
  │                                           │
  │ [2] IngestDataRequest                     │
  │     data_chunk: [                         │
  │       {id: "...", company: "A", ...},     │
  │       {id: "...", company: "B", ...},     │
  │       ... (300 rows)                      │
  │     ]                                     │
  ├──────────────────────────────────────────►│
  │                                           │ JSON 파싱
  │                                           │ DTO 변환
  │                                           │ DB 저장 (Metadata + Embedding)
  │                                           │
  │ [3] IngestDataRequest                     │
  │     data_chunk: [...]  (300 rows)         │
  ├──────────────────────────────────────────►│
  │                                           │
  │ ... (474번 반복)                           │
  │                                           │
  │ [475] stream 종료                          │
  ├──────────────────────────────────────────►│
  │                                           │ onCompleted() 호출
  │                                           │
  │           IngestDataResponse              │
  │           success: true                   │
  │           received_chunks: 474            │
  │◄──────────────────────────────────────────┤
  │                                           │
```

### 6.3 Reactive → Virtual Thread 전환

```
gRPC Stream (Reactive)
  │
  │ Flux<IngestDataRequest>
  │
  ▼
┌──────────────────────────┐
│  EmbeddingStreamServiceImpl │
│  (Reactive Context)      │
└──────────┬───────────────┘
           │
           │ onNext() 호출
           │
           ▼
┌──────────────────────────┐
│  DataProcessor           │
│  parseChunk()            │
│  (Blocking - JSON 파싱)   │
└──────────┬───────────────┘
           │
           │ publishOn(jpaScheduler)
           │ → Virtual Thread Pool로 전환
           │
           ▼
┌──────────────────────────┐
│  Virtual Thread[#123]    │
│  saveToDatabase()        │
│  (Blocking - JPA)        │
└──────────┬───────────────┘
           │
           │
           ▼
┌──────────────────────────┐
│  PostgreSQL              │
│  (pgvector)              │
└──────────────────────────┘
```

**왜 Virtual Thread가 필요한가?**

- gRPC는 Reactive Stream 사용 (Non-blocking)
- JPA는 Blocking API
- 기존 Thread Pool: Blocking 작업 시 Thread 고갈
- Virtual Thread: Blocking 시 자동으로 OS Thread 양보
  - 수천~수만 개 Virtual Thread 동시 실행 가능
  - Context Switching 비용 낮음

---

## 7. 용어 설명

### 7.1 gRPC 관련

| 용어 | 설명 | 예시 |
|-----|------|------|
| **RPC** | Remote Procedure Call - 원격 함수 호출 | `stub.StreamEmbedding(request)` |
| **Streaming** | 데이터를 연속적으로 전송 | `stream RowChunk` |
| **Server Streaming** | 서버가 여러 응답을 스트리밍 | Python → Java (데이터 전송) |
| **Client Streaming** | 클라이언트가 여러 요청을 스트리밍 | Java ← Python (데이터 수신) |
| **Bidirectional Streaming** | 양방향 스트리밍 (미사용) | - |
| **Stub** | gRPC 클라이언트 객체 | `EmbeddingStreamServiceStub` |
| **Servicer** | gRPC 서버 구현 클래스 | `EmbeddingStreamServiceImpl` |
| **Channel** | gRPC 연결 | `grpc.aio.insecure_channel()` |
| **Backpressure** | 수신자 속도에 맞춰 전송 조절 | 자동 처리 |

### 7.2 제네릭 관련

| 용어 | Python | Java | 설명 |
|-----|--------|------|------|
| **제네릭 타입** | `TypeVar` | `<T>` | 타입 파라미터 |
| **공변성** | `covariant=True` | `<? extends T>` | 하위 타입 허용 |
| **반공변성** | `contravariant=True` | `<? super T>` | 상위 타입 허용 |
| **불변성** | (기본값) | (기본값) | 정확한 타입만 허용 |
| **타입 소거** | ❌ (런타임 유지) | ✅ (컴파일 시 제거) | 런타임 타입 정보 |
| **Protocol** | `Protocol[T]` | `interface` | 인터페이스 정의 |
| **구조적 타이핑** | Duck Typing | ❌ | 명시적 상속 불필요 |

### 7.3 데이터 처리 관련

| 용어 | 설명 | 예시 |
|-----|------|------|
| **Chunk** | 데이터 분할 단위 | 300 rows |
| **Checkpoint** | 재시작 지점 | last UUID |
| **Domain** | 데이터 도메인 | "recruit", "candidate" |
| **DTO** | Data Transfer Object | `RecruitRowDto` |
| **Entity** | JPA 엔티티 (DB 매핑) | `MetadataEntity` |
| **Embedding** | 벡터 임베딩 | 384차원 float 배열 |
| **Vector** | 수치 벡터 | `[0.123, 0.456, ...]` |
| **UUID v7** | 시간 기반 UUID | 정렬 가능 |
| **Upsert** | Insert or Update | 중복 시 업데이트 |

### 7.4 Reactive 관련

| 용어 | 설명 | 예시 |
|-----|------|------|
| **Reactive Stream** | 비동기 스트림 처리 | `Flux<T>` |
| **Flux** | 0~N개 데이터 스트림 | `Flux<RowChunk>` |
| **Mono** | 0~1개 데이터 | `Mono<Response>` |
| **publishOn** | Scheduler 전환 | `.publishOn(jpaScheduler)` |
| **Scheduler** | 작업 실행 스레드 관리 | `Schedulers.boundedElastic()` |
| **Virtual Thread** | 경량 스레드 (Java 21+) | 수천 개 동시 실행 |
| **Blocking** | 스레드를 차단하는 작업 | JPA, JDBC |
| **Non-blocking** | 스레드를 차단하지 않음 | WebFlux, gRPC |

---

## 8. 수도 코드

### 8.1 Python: Client Streaming 전체 플로우

```python
# ============================================
# 1. 엔드포인트 - 사용자 요청 수신
# ============================================
@router.post("/data/ingest/{domain}")
async def trigger_ingestion(domain: str, file_name: str):
    """
    사용자가 /data/ingest/recruit?file_name=... 호출
    """
    # 1.1 파일 경로 구성
    file_path = f"{DATA_FOLDER}/{file_name}"

    # 1.2 서비스 레이어 호출
    response = await ingest_data_from_file(domain, file_path)

    # 1.3 응답 반환
    return {"success": response.success, "chunks": response.received_chunks}


# ============================================
# 2. 서비스 레이어 - 비즈니스 로직
# ============================================
async def ingest_data_from_file(domain: str, file_path: str):
    """
    도메인별 데이터 로드 → gRPC 전송
    """
    # 2.1 Factory에서 로더 선택
    loader = get_loader(domain)  # "recruit" → PklRecruitLoader

    # 2.2 데이터 로드
    data: List[RecruitData] = loader.load(file_path)
    # 결과: 141,897개 RecruitData 모델

    # 2.3 gRPC 클라이언트로 전송
    response = await stream_data_to_batch_server(
        domain=domain,
        file_name=file_name,
        data=data
    )

    return response


# ============================================
# 3. gRPC 클라이언트 - 스트리밍 전송
# ============================================
async def stream_data_to_batch_server(
    domain: str,
    file_name: str,
    data: List[RecruitData],
    chunk_size: int = 100
):
    """
    Batch Server로 Client Streaming
    """
    # 3.1 벡터 차원 확인
    vector_dim = len(data[0].vector) if data else 0  # 384

    # 3.2 gRPC 채널 생성
    async with grpc.aio.insecure_channel("localhost:50051") as channel:
        stub = EmbeddingStreamServiceStub(channel)

        # 3.3 비동기 제너레이터
        async def request_generator():
            # [메시지 1] 메타데이터
            yield IngestDataRequest(
                metadata=IngestMetadata(
                    domain=domain,
                    file_name=file_name,
                    vector_dimension=vector_dim
                )
            )

            # [메시지 2~475] 데이터 청크
            for i in range(0, len(data), chunk_size):
                chunk = data[i:i+chunk_size]

                # Pydantic → JSON → bytes
                json_list = [item.model_dump_json() for item in chunk]
                json_str = json.dumps(json_list)
                encoded = json_str.encode('utf-8')

                yield IngestDataRequest(data_chunk=encoded)

        # 3.4 gRPC 호출
        response = await stub.IngestDataStream(request_generator())
        return response


# ============================================
# 4. 로더 - 데이터 로딩
# ============================================
class PklRecruitLoader:
    def load(self, file_path: str) -> List[RecruitData]:
        """
        pkl 파일 → Pydantic 모델 리스트
        """
        # 4.1 Pandas로 pkl 읽기
        df = pd.read_pickle(file_path)

        # 4.2 각 행을 Pydantic 모델로 변환
        return [
            RecruitData(
                id=row['id'],
                company_name=row['Company Name'],
                exp_years=row['Exp Years'],
                english_level=row['English Level'],
                primary_keyword=row['Primary Keyword'],
                vector=row['job_post_vectors'].tolist()
            )
            for _, row in df.iterrows()
        ]


# ============================================
# 5. Factory - 로더 선택
# ============================================
_loader_registry = {
    "recruit": PklRecruitLoader(),
    "candidate": PklCandidateLoader(),
}

def get_loader(domain: str) -> DataLoader:
    """
    도메인 → 로더 매핑
    """
    loader = _loader_registry.get(domain)
    if not loader:
        raise ValueError(f"Unknown domain: {domain}")
    return loader
```

### 8.2 Java: gRPC Server 전체 플로우

```java
// ============================================
// 1. gRPC Server - Client Streaming 수신
// ============================================
@GrpcService
public class EmbeddingStreamServiceImpl
    extends EmbeddingStreamServiceGrpc.EmbeddingStreamServiceImplBase {

    private final DataProcessorFactory factory;

    @Override
    public StreamObserver<IngestDataRequest> ingestDataStream(
        StreamObserver<IngestDataResponse> responseObserver) {

        return new StreamObserver<IngestDataRequest>() {
            String domain;
            DataProcessor<?> processor;
            int chunks = 0;

            @Override
            public void onNext(IngestDataRequest request) {
                if (request.hasMetadata()) {
                    // [메시지 1] 메타데이터
                    handleMetadata(request.getMetadata());
                } else if (request.hasDataChunk()) {
                    // [메시지 2~N] 데이터 청크
                    handleDataChunk(request.getDataChunk());
                    chunks++;
                }
            }

            void handleMetadata(IngestMetadata metadata) {
                // 1.1 도메인 추출
                domain = metadata.getDomain();  // "recruit"

                // 1.2 Factory에서 프로세서 선택
                processor = factory.getProcessor(domain);
                // → RecruitDataProcessor

                log.info("[METADATA] Domain: {}", domain);
            }

            void handleDataChunk(ByteString dataChunk) {
                // 2.1 bytes → Entity 리스트
                byte[] bytes = dataChunk.toByteArray();
                List<?> entities = processor.parseChunk(bytes);

                // 2.2 DB 저장
                processor.saveToDatabase(entities);

                log.info("[CHUNK] Saved {} rows", entities.size());
            }

            @Override
            public void onCompleted() {
                // 3. 응답 반환
                IngestDataResponse response = IngestDataResponse.newBuilder()
                    .setSuccess(true)
                    .setReceivedChunks(chunks)
                    .setMessage("OK")
                    .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }
}


// ============================================
// 2. Factory - 프로세서 선택
// ============================================
@Component
public class DataProcessorFactory {
    Map<String, DataProcessor<?>> processors;

    // Spring이 모든 DataProcessor Bean을 자동 주입
    public DataProcessorFactory(List<DataProcessor<?>> list) {
        processors = list.stream()
            .collect(Collectors.toMap(
                DataProcessor::getDomain,
                p -> p
            ));
        // 결과: {"recruit": RecruitDataProcessor, ...}
    }

    public DataProcessor<?> getProcessor(String domain) {
        DataProcessor<?> p = processors.get(domain);
        if (p == null) throw new IllegalArgumentException("Unknown: " + domain);
        return p;
    }
}


// ============================================
// 3. 프로세서 - JSON 파싱 및 DB 저장
// ============================================
@Component
public class RecruitDataProcessor implements DataProcessor<MetadataEntity> {

    ObjectMapper mapper;
    MetadataRepository metadataRepo;
    EmbeddingRepository embeddingRepo;

    @Override
    public List<MetadataEntity> parseChunk(byte[] jsonChunk) {
        // 3.1 bytes → String
        String json = new String(jsonChunk, UTF_8);

        // 3.2 JSON → DTO 리스트
        List<RecruitRowDto> dtos = mapper.readValue(
            json,
            new TypeReference<List<RecruitRowDto>>() {}
        );

        // 3.3 DTO → Entity 변환
        return dtos.stream()
            .map(dto -> {
                MetadataEntity entity = new MetadataEntity();
                entity.setUuid(UUID.fromString(dto.getId()));
                entity.setCompanyName(dto.getCompanyName());
                entity.setExpYears(dto.getExpYears());
                // ...
                return entity;
            })
            .collect(Collectors.toList());
    }

    @Override
    public void saveToDatabase(List<MetadataEntity> entities) {
        // 4.1 Metadata 저장 (PK)
        metadataRepo.saveAll(entities);

        // 4.2 Embedding 저장 (FK 참조)
        List<EmbeddingEntity> embeddings = entities.stream()
            .map(m -> {
                EmbeddingEntity e = new EmbeddingEntity();
                e.setMetadata(m);
                e.setVector(m.getVector());  // DTO에서 임시 저장
                return e;
            })
            .collect(Collectors.toList());

        embeddingRepo.saveAll(embeddings);
    }

    @Override
    public String getDomain() {
        return "recruit";
    }
}
```

---

## 9. 확장 방법

### 9.1 새로운 도메인 추가 (예: "candidate")

#### Python 측

**1단계: 도메인 모델 정의**
```python
# src/domain/models.py

class CandidateData(BaseData):
    """후보자 도메인 모델"""
    name: str
    skills: List[str]
    experience_years: int
    resume_vector: List[float]  # 384 or 768 차원
```

**2단계: 로더 구현**
```python
# src/infrastructure/loaders.py

class PklCandidateLoader(DataLoader[CandidateData]):
    def load(self, file_path: str) -> List[CandidateData]:
        df = pd.read_pickle(file_path)
        return [CandidateData(**row) for row in df.to_dict('records')]
```

**3단계: 레지스트리 등록**
```python
# src/infrastructure/loaders.py

_loader_registry = {
    "recruit": PklRecruitLoader(),
    "candidate": PklCandidateLoader(),  # 추가
}
```

**4단계: DTO 추가 (선택적)**
```python
# src/domain/models.py

class CandidateRowDto(BaseModel):
    """gRPC 전송용 DTO"""
    id: str
    name: str
    skills: List[str]
    experience_years: int
    resume_vector: List[float]
```

#### Java 측

**1단계: Entity 정의**
```java
// CandidateMetadata.java

@Entity
@Table(name = "candidate_metadata")
public class CandidateMetadata {
    @Id
    private UUID uuid;

    private String name;

    @Convert(converter = StringListConverter.class)
    private List<String> skills;

    private Integer experienceYears;

    // getters/setters
}
```

**2단계: Repository 생성**
```java
public interface CandidateMetadataRepository
    extends JpaRepository<CandidateMetadata, UUID> {
}
```

**3단계: DTO 정의**
```java
// CandidateRowDto.java

public class CandidateRowDto {
    private String id;
    private String name;
    private List<String> skills;
    private Integer experienceYears;
    private List<Float> resumeVector;

    // getters/setters
}
```

**4단계: Processor 구현**
```java
@Component
public class CandidateDataProcessor
    implements DataProcessor<CandidateMetadata> {

    private final ObjectMapper mapper;
    private final CandidateMetadataRepository repo;

    @Override
    public List<CandidateMetadata> parseChunk(byte[] jsonChunk) {
        String json = new String(jsonChunk, UTF_8);
        List<CandidateRowDto> dtos = mapper.readValue(
            json,
            new TypeReference<List<CandidateRowDto>>() {}
        );

        return dtos.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public void saveToDatabase(List<CandidateMetadata> entities) {
        repo.saveAll(entities);
        // Embedding 저장 로직 추가
    }

    @Override
    public String getDomain() {
        return "candidate";
    }

    private CandidateMetadata toEntity(CandidateRowDto dto) {
        CandidateMetadata entity = new CandidateMetadata();
        entity.setUuid(UUID.fromString(dto.getId()));
        entity.setName(dto.getName());
        entity.setSkills(dto.getSkills());
        entity.setExperienceYears(dto.getExperienceYears());
        return entity;
    }
}
```

**5단계: Spring Bean 자동 등록**
```java
// @Component 덕분에 자동 등록
// Factory가 자동으로 인식
// 추가 코드 불필요!
```

### 9.2 Proto 파일 확장 (도메인별 메시지)

현재는 모든 도메인이 `bytes data_chunk`를 사용하지만, 도메인별로 타입을 분리할 수도 있습니다.

```protobuf
message IngestDataRequest {
  oneof request_type {
    IngestMetadata metadata = 1;

    // 도메인별 메시지
    RecruitDataChunk recruit_chunk = 2;
    CandidateDataChunk candidate_chunk = 3;
  }
}

message RecruitDataChunk {
  repeated RecruitRow rows = 1;
}

message CandidateDataChunk {
  repeated CandidateRow rows = 1;
}

message CandidateRow {
  string id = 1;
  string name = 2;
  repeated string skills = 3;
  int32 experience_years = 4;
  repeated float resume_vector = 5;
}
```

**장점:**
- 타입 안전성 증가
- Protobuf 직렬화 (JSON보다 빠름)

**단점:**
- Proto 파일 관리 복잡도 증가
- Python-Java 코드 재생성 필요

---

## 10. 트러블슈팅

### 10.1 일반적인 문제

#### 문제 1: "Connection refused to localhost:50051"

**원인:** Python 서버가 실행되지 않았거나 포트가 다름

**해결:**
```bash
# Python 서버 실행 확인
cd Demo-Python
python src/main.py

# 포트 확인
netstat -ano | findstr 50051

# 설정 확인
# Python: src/config/settings.py → GRPC_SERVER_PORT
# Java: application.yml → grpc.client.python-embedding.address
```

#### 문제 2: "JSON parsing error: Unexpected character"

**원인:** Python-Java 간 JSON 인코딩/디코딩 불일치

**해결:**
```python
# Python: UTF-8 인코딩 확인
encoded = json_str.encode('utf-8')
```

```java
// Java: UTF-8 디코딩 확인
String json = new String(bytes, StandardCharsets.UTF_8);
```

#### 문제 3: "Unknown domain: xxx"

**원인:** Factory 레지스트리에 도메인 미등록

**해결:**
```python
# Python: loaders.py
_loader_registry = {
    "recruit": PklRecruitLoader(),
    "xxx": XxxLoader(),  # 추가
}
```

```java
// Java: @Component 확인
@Component
public class XxxDataProcessor implements DataProcessor<XxxEntity> {
    @Override
    public String getDomain() {
        return "xxx";  // 도메인 이름 확인
    }
}
```

#### 문제 4: "Vector dimension mismatch: expected 384, got 768"

**원인:** Python과 Java의 vector_dimension 설정 불일치

**해결:**
```python
# Python: src/config/settings.py
VECTOR_DIMENSION = 384
```

```yaml
# Java: application.yml
batch:
  embedding:
    vector-dimension: 384
```

```java
// Java: EmbeddingEntity.java
@Column(name = "vector", columnDefinition = "vector(384)")
private List<Float> vector;
```

### 10.2 성능 최적화

#### 최적화 1: Chunk Size 조정

**기본값:** 300 rows

**조정 기준:**
- **네트워크 느림**: Chunk 크기 증가 (500~1000)
- **메모리 부족**: Chunk 크기 감소 (100~200)
- **CPU 많음**: Chunk 크기 증가 + 병렬 처리

```python
# Python
chunk_size = 500  # 조정
```

```yaml
# Java
batch:
  embedding:
    chunk-size: 500
```

#### 최적화 2: Batch Insert

```java
// JPA Batch Insert 활성화
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 300
        order_inserts: true
        order_updates: true
```

#### 최적화 3: Connection Pool 크기

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Virtual Thread 고려
```

### 10.3 디버깅 팁

#### Python 로깅 증가

```python
import logging
logging.basicConfig(level=logging.DEBUG)
```

#### Java 로깅 증가

```yaml
logging:
  level:
    com.alpha.backend: DEBUG
    io.grpc: DEBUG
```

#### gRPC 메시지 덤프

```python
# Python
import grpc

# Verbose 모드
os.environ['GRPC_VERBOSITY'] = 'DEBUG'
os.environ['GRPC_TRACE'] = 'all'
```

```java
// Java
// application.yml
logging:
  level:
    io.grpc.netty: DEBUG
```

---

## 11. 참고 자료

### 11.1 공식 문서

- **gRPC**: https://grpc.io/docs/
- **Protocol Buffers**: https://protobuf.dev/
- **Spring Boot gRPC**: https://yidongnan.github.io/grpc-spring-boot-starter/
- **FastAPI**: https://fastapi.tiangolo.com/
- **Pydantic**: https://docs.pydantic.dev/
- **Project Reactor**: https://projectreactor.io/docs

### 11.2 프로젝트 내부 문서

- **루트 CLAUDE.md**: `/CLAUDE.md`
- **Batch Server CLAUDE.md**: `/Backend/Batch-Server/CLAUDE.md`
- **Demo-Python CLAUDE.md**: `/Demo-Python/CLAUDE.md`
- **Batch 설계서**: `/Backend/Batch-Server/docs/Batch설계서.md`
- **Python 서버 설계서**: `/Demo-Python/docs/Python_서버_설계서.md`

---

**작성일:** 2025-12-12
**버전:** 1.0
**작성자:** Claude Sonnet 4.5
**검토자:** 프로젝트 팀
