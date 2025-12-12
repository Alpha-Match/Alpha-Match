"""
Configuration module for Demo-Python gRPC server.

This module centralizes all configuration parameters for:
- gRPC server settings
- Data loading and streaming settings
- File paths and performance tuning
"""

import os
from dataclasses import dataclass
from pathlib import Path


@dataclass
class ServerConfig:
    """gRPC Server configuration"""
    HOST: str = '[::]:50051'  # Listen on all interfaces, port 50051
    MAX_WORKERS: int = 10      # ThreadPoolExecutor max workers
    MAX_MESSAGE_LENGTH: int = 100 * 1024 * 1024  # 100MB max message size


@dataclass
class DataConfig:
    """Data loading and processing configuration"""
    # File paths
    BASE_DIR: Path = Path(__file__).parent.parent
    DATA_DIR: Path = BASE_DIR / 'data'
    PKL_FILE: Path = DATA_DIR / 'processed_recruitment_data.pkl'

    # Streaming settings
    DEFAULT_CHUNK_SIZE: int = 300  # Default rows per chunk
    MIN_CHUNK_SIZE: int = 100      # Minimum chunk size
    MAX_CHUNK_SIZE: int = 1000     # Maximum chunk size

    # Vector settings
    VECTOR_DIMENSION: int = 384    # Actual dimension from data inspection

    # Data optimization
    OPTIMIZE_DTYPES: bool = True   # Enable dtype optimization
    USE_CATEGORY: bool = True      # Use category dtype for strings

    # Logging
    LOG_CHUNK_INTERVAL: int = 10   # Log every N chunks


@dataclass
class PerformanceConfig:
    """Performance tuning configuration"""
    # Memory management
    CHUNK_READ_SIZE: int = 1000    # Rows to read at once from DataFrame

    # Retry settings
    MAX_RETRY_ATTEMPTS: int = 3
    RETRY_DELAY_SECONDS: float = 1.0

    # Timeout settings
    REQUEST_TIMEOUT_SECONDS: int = 300  # 5 minutes


# Global config instances
server_config = ServerConfig()
data_config = DataConfig()
performance_config = PerformanceConfig()


def validate_config() -> bool:
    """
    Validate configuration settings.

    Returns:
        bool: True if all configurations are valid

    Raises:
        ValueError: If configuration is invalid
        FileNotFoundError: If data file doesn't exist
    """
    # Check data file exists
    if not data_config.PKL_FILE.exists():
        raise FileNotFoundError(
            f"Data file not found: {data_config.PKL_FILE}\n"
            f"Expected location: {data_config.PKL_FILE.absolute()}"
        )

    # Validate chunk size
    if not (data_config.MIN_CHUNK_SIZE <= data_config.DEFAULT_CHUNK_SIZE <= data_config.MAX_CHUNK_SIZE):
        raise ValueError(
            f"Invalid chunk size configuration: "
            f"MIN({data_config.MIN_CHUNK_SIZE}) <= DEFAULT({data_config.DEFAULT_CHUNK_SIZE}) <= MAX({data_config.MAX_CHUNK_SIZE})"
        )

    # Validate vector dimension
    if data_config.VECTOR_DIMENSION <= 0:
        raise ValueError(f"Invalid vector dimension: {data_config.VECTOR_DIMENSION}")

    print(f"[Config] Configuration validated successfully")
    print(f"[Config] Data file: {data_config.PKL_FILE}")
    print(f"[Config] Server: {server_config.HOST}")
    print(f"[Config] Default chunk size: {data_config.DEFAULT_CHUNK_SIZE}")
    print(f"[Config] Vector dimension: {data_config.VECTOR_DIMENSION}")

    return True


if __name__ == '__main__':
    # Test configuration
    try:
        validate_config()
        print("\nAll configurations are valid!")
    except Exception as e:
        print(f"Configuration error: {e}")
        raise
