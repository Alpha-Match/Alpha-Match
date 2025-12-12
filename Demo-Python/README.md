# Demo-Python FastAPI + gRPC Server

Python server for streaming embedding data from .pkl files to the Java Batch Server via gRPC.

## Architecture

This server provides:
- **FastAPI HTTP API** for data ingestion requests
- **gRPC Client** for streaming data to Java Batch Server
- **Domain-specific data loaders** (recruit, headhunter)

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Start Server

```bash
python src/main.py
```

Or on Windows:
```bash
start_server.bat
```

Server will start on `http://localhost:8000`

### 3. Ingest Data

Trigger data ingestion via HTTP API:

```bash
# Ingest recruit data
curl -X POST "http://localhost:8000/data/ingest/recruit?file_name=processed_recruitment_data.pkl"

# Ingest headhunter data
curl -X POST "http://localhost:8000/data/ingest/headhunter?file_name=processed_headhunter_data.pkl"
```

### 4. Health Check

```bash
curl http://localhost:8000/health
```

## Project Structure

```
Demo-Python/
├── src/
│   ├── main.py                          # FastAPI application entry point
│   ├── api/
│   │   └── endpoints.py                 # FastAPI endpoints
│   ├── services/
│   │   └── ingestion_service.py         # Data ingestion business logic
│   ├── infrastructure/
│   │   ├── loaders.py                   # Domain-specific data loaders
│   │   └── grpc_clients.py              # gRPC client for Batch Server
│   ├── domain/
│   │   ├── models.py                    # Domain models
│   │   └── utils.py                     # UUID utilities
│   ├── config/
│   │   └── settings.py                  # Configuration
│   └── proto/                           # Generated protobuf files
│       ├── embedding_stream.proto
│       ├── embedding_stream_pb2.py
│       └── embedding_stream_pb2_grpc.py
│
├── data/
│   ├── processed_recruitment_data.pkl   # Recruit embedding data (~500MB)
│   └── processed_headhunter_data.pkl    # Headhunter embedding data
│
├── docs/                                # Design documents
├── requirements.txt
└── README.md
```

## Data Structure

### Recruit Domain (.pkl file)
- **Total rows**: ~142,000
- **Columns**:
  - `Company Name`: Company name
  - `Exp Years`: Experience years (e.g., "2y", "5y")
  - `English Level`: English proficiency level
  - `Primary Keyword`: Primary job keyword
  - `job_post_vectors`: Embedding vector (384 dimensions)

### Headhunter Domain (.pkl file)
Similar structure with headhunter-specific fields.

## API Endpoints

### POST /data/ingest/{domain}

Trigger data ingestion for a specific domain.

**Path Parameters:**
- `domain`: Domain name (`recruit` or `headhunter`)

**Query Parameters:**
- `file_name`: Name of the .pkl file (optional, defaults to domain-specific file)
- `chunk_size`: Chunk size for streaming (optional, default: 300)

**Example:**
```bash
curl -X POST "http://localhost:8000/data/ingest/recruit?file_name=processed_recruitment_data.pkl&chunk_size=500"
```

**Response:**
```json
{
  "status": "success",
  "domain": "recruit",
  "message": "Successfully sent 141897 rows in 474 chunks",
  "stats": {
    "total_rows": 141897,
    "total_chunks": 474,
    "chunk_size": 300,
    "file_name": "processed_recruitment_data.pkl"
  }
}
```

### GET /health

Health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "timestamp": "2025-12-12T10:30:00",
  "version": "1.0.0"
}
```

## Features

### Memory Optimization

The data loaders use dtype optimization to reduce memory usage by 40-50%:

```python
from infrastructure.loaders import load_recruit_data

df = load_recruit_data()  # Uses category dtype for strings
```

### Configurable Chunk Size

Adjust chunk size (100-1000 rows):

```bash
curl -X POST "http://localhost:8000/data/ingest/recruit?chunk_size=500"
```

### Domain-Specific Loaders

Each domain has its own loader with optimized schema:

```python
# Recruit domain
df = load_recruit_data(file_name="processed_recruitment_data.pkl")

# Headhunter domain
df = load_headhunter_data(file_name="processed_headhunter_data.pkl")
```

## Configuration

Edit `src/config/settings.py` to customize:

```python
@dataclass
class Settings:
    # FastAPI
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # gRPC Batch Server
    BATCH_SERVER_HOST: str = "localhost"
    BATCH_SERVER_PORT: int = 50052

    # Data
    DATA_DIR: Path = Path(__file__).parent.parent.parent / "data"
    DEFAULT_CHUNK_SIZE: int = 300
    VECTOR_DIMENSION: int = 384
```

## Testing

### Test Data Ingestion

1. Start the server:
```bash
python src/main.py
```

2. In another terminal, trigger ingestion:
```bash
curl -X POST "http://localhost:8000/data/ingest/recruit"
```

3. Check Batch Server logs to verify data reception.

### Manual Testing with Different Chunk Sizes

```bash
# Small chunks (faster streaming)
curl -X POST "http://localhost:8000/data/ingest/recruit?chunk_size=100"

# Large chunks (fewer network calls)
curl -X POST "http://localhost:8000/data/ingest/recruit?chunk_size=1000"
```

## gRPC Communication

### Proto Definition

```protobuf
service EmbeddingStreamService {
  rpc IngestDataStream(stream RowChunk) returns (StreamResponse);
}

message RowChunk {
  string domain = 1;
  repeated RecruitRow rows = 2;
}

message RecruitRow {
  string id = 1;
  string company_name = 2;
  int32 exp_years = 3;
  string english_level = 4;
  string primary_keyword = 5;
  repeated float vector = 6;
}
```

### Port Configuration

- **FastAPI HTTP**: 8000
- **Java Batch Server gRPC**: 50052 (client connects to this)

## Performance

### Memory Usage

- **Basic loading**: ~471 MB
- **Optimized loading**: ~447 MB (5.3% savings)
- **Streaming**: Low memory (processes chunks)

### Throughput

- **Chunk size 100**: ~14 chunks/sec
- **Chunk size 300**: ~10 chunks/sec (optimal)
- **Chunk size 1000**: ~5 chunks/sec

## Troubleshooting

### Import Errors

If you see `ModuleNotFoundError: No module named 'embedding_stream_pb2'`:

The proto files should be in `src/proto/`. Ensure they are generated correctly.

### File Not Found

Ensure the .pkl file exists:
```bash
ls data/processed_recruitment_data.pkl
```

### Connection Refused (gRPC)

Ensure Java Batch Server is running on port 50052:
```bash
# Check if Batch Server is running
netstat -an | grep 50052
```

### Port Already in Use (FastAPI)

Change the port in `src/config/settings.py`:
```python
PORT: int = 8001  # Use different port
```

## Integration with Batch Server

The Java Batch Server acts as a gRPC server:

1. Python server receives HTTP POST to `/data/ingest/{domain}`
2. Python server loads .pkl file and chunks data
3. Python server connects to Batch Server as gRPC client
4. Python server streams `RowChunk` data via `IngestDataStream`
5. Batch Server processes and saves to PostgreSQL with pgvector

See `Backend/Batch-Server/CLAUDE.md` for Batch Server details.

## Documentation

- **Design Documents**: `docs/`
- **Server Instructions**: `CLAUDE.md`
- **Batch Server**: `../Backend/Batch-Server/CLAUDE.md`
- **Project Root**: `../CLAUDE.md`

## License

Internal project - Alpha-Match

## Author

Created: 2025-12-11
Updated: 2025-12-12 (Refactored to FastAPI + gRPC Client)
