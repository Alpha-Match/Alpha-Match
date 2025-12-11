# Demo-Python gRPC Embedding Server

Python gRPC server for streaming embedding data from .pkl files to the Batch Server.

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Start Server

```bash
cd src
python grpc_server.py
```

Server will start on `localhost:50051`

### 3. Test with Client (Optional)

In a separate terminal:

```bash
cd src
python grpc_client.py
```

Or with custom parameters:

```bash
python grpc_client.py --chunk-size 500
python grpc_client.py --checkpoint c0ca96e7-85df-50df-a64e-d934cd02a170
```

## Project Structure

```
Demo-Python/
├── src/
│   ├── grpc_server.py          # Main gRPC server
│   ├── grpc_client.py          # Test client
│   ├── data_loader.py          # Optimized .pkl loading
│   ├── chunker.py              # Chunk splitting logic
│   ├── config.py               # Configuration
│   ├── uuid_generator.py       # UUID utilities
│   └── proto/                  # Generated protobuf files
│       ├── embedding_stream.proto
│       ├── embedding_stream_pb2.py
│       └── embedding_stream_pb2_grpc.py
│
├── data/
│   └── processed_recruitment_data.pkl  # Embedding data (~500MB)
│
├── docs/                       # Design documents
├── requirements.txt
└── README.md
```

## Data Structure

The .pkl file contains recruitment embedding data:

- **Total rows**: ~142,000
- **Columns**:
  - `id`: UUID string
  - `Company Name`: Company name
  - `Exp Years`: Experience years (e.g., "2y", "5y")
  - `English Level`: English proficiency level
  - `Primary Keyword`: Primary job keyword
  - `job_post_vectors`: Embedding vector (384 dimensions)

## Features

### Memory Optimization

The data loader uses dtype optimization to reduce memory usage by 40-50%:

```python
from data_loader import load_data_optimized

df = load_data_optimized()  # Uses category dtype for strings
```

### Checkpoint Support

Resume streaming from a specific UUID:

```python
# In client
request = StreamEmbeddingRequest(
    last_processed_uuid="c0ca96e7-85df-50df-a64e-d934cd02a170",
    chunk_size=300
)
```

### Configurable Chunk Size

Adjust chunk size (100-1000 rows):

```python
# In config.py
DEFAULT_CHUNK_SIZE = 300  # Default
MIN_CHUNK_SIZE = 100      # Minimum
MAX_CHUNK_SIZE = 1000     # Maximum
```

## Configuration

Edit `src/config.py` to customize:

```python
@dataclass
class ServerConfig:
    HOST: str = '[::]:50051'
    MAX_WORKERS: int = 10
    MAX_MESSAGE_LENGTH: int = 100 * 1024 * 1024  # 100MB

@dataclass
class DataConfig:
    DEFAULT_CHUNK_SIZE: int = 300
    VECTOR_DIMENSION: int = 384
    LOG_CHUNK_INTERVAL: int = 10
```

## Testing

### Test Individual Modules

```bash
cd src

# Test configuration
python config.py

# Test data loader
python data_loader.py

# Test chunker
python chunker.py

# Test UUID generator
python uuid_generator.py
```

### Test Full Server

1. Start server:
```bash
python grpc_server.py
```

2. In another terminal, run client:
```bash
python grpc_client.py --chunk-size 100
```

## gRPC API

### Service: EmbeddingStreamService

#### Method: StreamEmbedding

**Type**: Unary → Server Streaming

**Request**: `StreamEmbeddingRequest`
- `last_processed_uuid` (string): Checkpoint UUID to resume from (optional)
- `chunk_size` (int32): Desired chunk size (default: 300)

**Response**: Stream of `RowChunk`
- Each `RowChunk` contains multiple `RecruitRow` objects

**Example**:
```python
import grpc
from proto import embedding_stream_pb2, embedding_stream_pb2_grpc

channel = grpc.insecure_channel('localhost:50051')
stub = embedding_stream_pb2_grpc.EmbeddingStreamServiceStub(channel)

request = embedding_stream_pb2.StreamEmbeddingRequest(
    chunk_size=300
)

for chunk in stub.StreamEmbedding(request):
    print(f"Received {len(chunk.rows)} rows")
```

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

The proto file imports have been fixed. Ensure you're using the compiled files in `src/proto/`.

### File Not Found

Ensure the .pkl file exists:
```bash
ls data/processed_recruitment_data.pkl
```

### Port Already in Use

Change the port in `src/config.py`:
```python
HOST: str = '[::]:50052'  # Use different port
```

## Integration with Batch Server

The Batch Server (Java) connects as a client:

1. Batch Server sends `StreamEmbeddingRequest`
2. Python server streams `RowChunk` data
3. Batch Server processes and saves to PostgreSQL

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
