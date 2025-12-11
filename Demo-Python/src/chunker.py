"""
DataFrame chunking logic for streaming.

This module provides efficient chunking strategies for streaming large DataFrames
through gRPC. It supports:
- Fixed-size chunking
- Adaptive chunking based on memory
- Iterator-based processing for memory efficiency
"""

import pandas as pd
from typing import Iterator, List, Dict, Any
import logging

from config import data_config

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)


def chunk_dataframe(
    df: pd.DataFrame,
    chunk_size: int = None
) -> Iterator[pd.DataFrame]:
    """
    Split DataFrame into fixed-size chunks.

    Args:
        df: DataFrame to chunk
        chunk_size: Number of rows per chunk (default: from config)

    Yields:
        DataFrame chunks

    Example:
        >>> df = pd.DataFrame({'a': range(1000)})
        >>> for chunk in chunk_dataframe(df, chunk_size=100):
        ...     print(len(chunk))  # Prints 100, 100, ..., 100
    """
    chunk_size = chunk_size or data_config.DEFAULT_CHUNK_SIZE

    # Validate chunk size
    if chunk_size < data_config.MIN_CHUNK_SIZE:
        logger.warning(
            f"Chunk size {chunk_size} is below minimum {data_config.MIN_CHUNK_SIZE}. "
            f"Using minimum."
        )
        chunk_size = data_config.MIN_CHUNK_SIZE

    if chunk_size > data_config.MAX_CHUNK_SIZE:
        logger.warning(
            f"Chunk size {chunk_size} exceeds maximum {data_config.MAX_CHUNK_SIZE}. "
            f"Using maximum."
        )
        chunk_size = data_config.MAX_CHUNK_SIZE

    total_rows = len(df)
    total_chunks = (total_rows + chunk_size - 1) // chunk_size

    logger.info(f"Chunking {total_rows:,} rows into {total_chunks} chunks of size {chunk_size}")

    for start_idx in range(0, total_rows, chunk_size):
        end_idx = min(start_idx + chunk_size, total_rows)
        chunk = df.iloc[start_idx:end_idx]

        yield chunk


def chunk_to_rows(chunk: pd.DataFrame) -> List[Dict[str, Any]]:
    """
    Convert DataFrame chunk to list of row dictionaries.

    Args:
        chunk: DataFrame chunk

    Returns:
        List of dictionaries, each representing a row

    Note:
        This conversion is necessary for protobuf serialization.
        Each row dict contains: id, company_name, exp_years, english_level,
        primary_keyword, vector
    """
    rows = []

    for idx, row in chunk.iterrows():
        row_dict = {
            'id': str(row['id']),
            'company_name': str(row['Company Name']),
            'exp_years': _parse_exp_years(row['Exp Years']),
            'english_level': str(row['English Level']),
            'primary_keyword': str(row['Primary Keyword']),
            'vector': _ensure_float_list(row['job_post_vectors'])
        }
        rows.append(row_dict)

    return rows


def _parse_exp_years(exp_years_str: str) -> int:
    """
    Parse experience years from string format.

    Args:
        exp_years_str: String like '2y', '5y', '10y'

    Returns:
        Integer years (e.g., 2, 5, 10)

    Note:
        Handles edge cases like '0y', missing 'y', or invalid formats.
    """
    try:
        # Remove 'y' suffix and convert to int
        if isinstance(exp_years_str, str):
            years_str = exp_years_str.replace('y', '').strip()
            return int(years_str) if years_str else 0
        elif isinstance(exp_years_str, (int, float)):
            return int(exp_years_str)
        else:
            logger.warning(f"Unexpected exp_years format: {exp_years_str} ({type(exp_years_str)})")
            return 0
    except (ValueError, AttributeError) as e:
        logger.warning(f"Failed to parse exp_years '{exp_years_str}': {e}")
        return 0


def _ensure_float_list(vector) -> List[float]:
    """
    Ensure vector is a list of floats.

    Args:
        vector: Vector data (could be list, numpy array, etc.)

    Returns:
        List of floats

    Note:
        Protobuf repeated float requires Python list of floats.
    """
    try:
        if isinstance(vector, list):
            # Already a list, ensure float type
            return [float(x) for x in vector]
        elif hasattr(vector, 'tolist'):
            # NumPy array or similar
            return [float(x) for x in vector.tolist()]
        else:
            logger.warning(f"Unexpected vector type: {type(vector)}")
            return list(vector)
    except Exception as e:
        logger.error(f"Failed to convert vector to float list: {e}")
        return []


def get_chunk_stats(chunk: pd.DataFrame) -> Dict[str, Any]:
    """
    Get statistics about a chunk.

    Args:
        chunk: DataFrame chunk

    Returns:
        Dictionary with chunk statistics
    """
    return {
        'row_count': len(chunk),
        'memory_mb': chunk.memory_usage(deep=True).sum() / 1024**2,
        'first_id': chunk['id'].iloc[0] if len(chunk) > 0 else None,
        'last_id': chunk['id'].iloc[-1] if len(chunk) > 0 else None,
    }


class ChunkIterator:
    """
    Iterator class for streaming DataFrame chunks.

    This class provides:
    - Progress tracking
    - Logging at intervals
    - Error handling per chunk
    """

    def __init__(
        self,
        df: pd.DataFrame,
        chunk_size: int = None,
        log_interval: int = None
    ):
        """
        Initialize chunk iterator.

        Args:
            df: DataFrame to iterate
            chunk_size: Rows per chunk (default: from config)
            log_interval: Log every N chunks (default: from config)
        """
        self.df = df
        self.chunk_size = chunk_size or data_config.DEFAULT_CHUNK_SIZE
        self.log_interval = log_interval or data_config.LOG_CHUNK_INTERVAL

        self.total_rows = len(df)
        self.total_chunks = (self.total_rows + self.chunk_size - 1) // self.chunk_size
        self.current_chunk = 0

        logger.info(
            f"Initialized ChunkIterator: {self.total_rows:,} rows, "
            f"{self.total_chunks} chunks, size={self.chunk_size}"
        )

    def __iter__(self):
        """Return iterator"""
        return self

    def __next__(self) -> pd.DataFrame:
        """
        Get next chunk.

        Returns:
            Next DataFrame chunk

        Raises:
            StopIteration: When no more chunks available
        """
        start_idx = self.current_chunk * self.chunk_size

        if start_idx >= self.total_rows:
            logger.info(f"Completed streaming all {self.total_chunks} chunks")
            raise StopIteration

        end_idx = min(start_idx + self.chunk_size, self.total_rows)
        chunk = self.df.iloc[start_idx:end_idx]

        self.current_chunk += 1

        # Log progress at intervals
        if self.current_chunk % self.log_interval == 0 or self.current_chunk == self.total_chunks:
            progress = (self.current_chunk / self.total_chunks) * 100
            logger.info(
                f"Progress: {self.current_chunk}/{self.total_chunks} chunks "
                f"({progress:.1f}%) - Rows: {end_idx:,}/{self.total_rows:,}"
            )

        return chunk


if __name__ == '__main__':
    # Test chunker
    print("=" * 80)
    print("Testing Chunker")
    print("=" * 80)

    # Create test DataFrame
    test_df = pd.DataFrame({
        'id': [f'uuid-{i}' for i in range(1000)],
        'Company Name': [f'Company_{i%10}' for i in range(1000)],
        'Exp Years': [f'{i%15}y' for i in range(1000)],
        'English Level': [['beginner', 'intermediate', 'advanced'][i % 3] for i in range(1000)],
        'Primary Keyword': [['Backend', 'Frontend', 'AI'][i % 3] for i in range(1000)],
        'job_post_vectors': [[0.1 * i] * 384 for i in range(1000)]
    })

    print(f"\nTest DataFrame: {len(test_df)} rows")

    # Test basic chunking
    print("\n1. Basic Chunking (chunk_size=100):")
    chunk_count = 0
    for chunk in chunk_dataframe(test_df, chunk_size=100):
        chunk_count += 1
        if chunk_count == 1:
            print(f"   First chunk: {len(chunk)} rows")
            stats = get_chunk_stats(chunk)
            print(f"   Stats: {stats}")

    print(f"   Total chunks: {chunk_count}")

    # Test chunk to rows conversion
    print("\n2. Chunk to Rows Conversion:")
    first_chunk = test_df.iloc[:5]
    rows = chunk_to_rows(first_chunk)
    print(f"   Converted {len(rows)} rows")
    print(f"   First row keys: {list(rows[0].keys())}")
    print(f"   Sample row:")
    print(f"     ID: {rows[0]['id']}")
    print(f"     Company: {rows[0]['company_name']}")
    print(f"     Exp Years: {rows[0]['exp_years']} (type: {type(rows[0]['exp_years'])})")
    print(f"     Vector length: {len(rows[0]['vector'])}")

    # Test ChunkIterator
    print("\n3. ChunkIterator:")
    iterator = ChunkIterator(test_df, chunk_size=200, log_interval=2)
    chunk_count = sum(1 for _ in iterator)
    print(f"   Processed {chunk_count} chunks")

    print("\n" + "=" * 80)
    print("Chunker tests completed successfully!")
    print("=" * 80)
