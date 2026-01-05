# Quick Start: Preprocessing Usage

## Load and Process Candidate Data

```python
from src.infrastructure.loaders import get_loader_auto
from src.domain.models import CandidateData

# Create loader (auto-detects format from file extension)
loader = get_loader_auto(
    domain="candidate",
    file_path="data/candidate_v1.pkl",
    chunk_size=300  # Optional, default 1000
)

# Process chunks
for chunk in loader.load_chunks("data/candidate_v1.pkl"):
    # chunk is List[CandidateData]
    for record in chunk:
        print(f"ID: {record.candidate_id}")
        print(f"Category: {record.position_category}")
        print(f"Experience: {record.experience_years} years")
        print(f"Skills: {record.skills}")
        print(f"Vector dim: {len(record.skills_vector)}")
```

## Load and Process Recruit Data (v2 or v1)

```python
from src.infrastructure.loaders import get_loader_auto
from src.domain.models import RecruitData

# Works with both v1 and v2 files
loader = get_loader_auto(
    domain="recruit",
    file_path="data/recruitment_v2.pkl"
)

for chunk in loader.load_chunks("data/recruitment_v2.pkl"):
    # chunk is List[RecruitData]
    for record in chunk:
        print(f"ID: {record.id}")
        print(f"Position: {record.position}")
        print(f"Company: {record.company_name}")
        print(f"Skills: {record.skills}")
```

## Expected Output

### Candidate (candidate_v1.pkl)
- Total records: 118,741 (filtered from 123,464)
- Filtered: 4,723 rows (empty skills/null vectors)
- Vector dimension: 384d
- All fields validated by Pydantic

### Recruit v2 (recruitment_v2.pkl)
- Total records: 93,554 (filtered from 93,675)
- Filtered: 121 rows (empty skills)
- Vector dimension: 384d
- db_id column automatically removed

### Recruit v1 (recruitment_v1.pkl)
- Total records: 87,488 (filtered from 93,675)
- Filtered: 6,187 rows (empty skills/null vectors)
- Vector dimension: 384d
- Fully compatible with same preprocessing logic

## Testing

Run validation tests:
```bash
cd Demo-Python
python test_preprocessing.py
```

Expected output:
```
PASS: Candidate v1
PASS: Recruit v2
PASS: Recruit v1

*** ALL TESTS PASSED ***
```

## Common Issues

### Issue: ModuleNotFoundError: No module named 'numpy._core.numeric'
**Solution**: Upgrade dependencies
```bash
pip install --upgrade "numpy>=1.26.2,<3.0.0" "pandas>=2.2.0"
```

### Issue: Validation error for position_category
**Solution**: Already handled! NaN values automatically filled with 'Unknown'

### Issue: Vector dimension mismatch
**Solution**: Automatic validation in Pydantic model (384d enforced)

## Performance Tips

1. **Adjust chunk size**:
   - Smaller (100-300): Less memory, slower processing
   - Larger (1000-5000): More memory, faster processing

2. **Use appropriate format**:
   - pkl: Fast loading, best for small files
   - parquet: Best for large files (future use)
   - csv: Human-readable but slower

3. **Monitor memory**:
   ```python
   import sys
   chunk_size_mb = sys.getsizeof(chunk) / 1024 / 1024
   print(f"Chunk size: {chunk_size_mb:.2f} MB")
   ```

## Next Steps

1. Integrate with FastAPI endpoints (`src/api/ingest.py`)
2. Configure gRPC client for Batch Server streaming
3. Run end-to-end pipeline test (Python → Java → PostgreSQL)

## Documentation

- Full implementation details: `docs/preprocessing_implementation_summary.md`
- Column mapping analysis: `docs/column_mapping_analysis.md`
- Domain models: `src/domain/models.py`
- Loader implementation: `src/infrastructure/loaders.py`
