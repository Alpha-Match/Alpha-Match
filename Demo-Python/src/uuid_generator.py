"""
UUID v7 generator for PostgreSQL compatibility.

UUID v7 provides:
- Time-ordered sorting (better for database indexing)
- PostgreSQL UUID type compatibility
- Uniqueness across distributed systems

Note: Since the .pkl file already contains UUIDs (UUID v5 based on content),
this module is primarily for documentation and future use when generating
new embeddings.
"""

import uuid
import time
from typing import List


def generate_uuid_v7() -> str:
    """
    Generate a UUID v7 (time-ordered UUID).

    UUID v7 format:
    - 48 bits: Unix timestamp in milliseconds
    - 12 bits: Random data
    - 2 bits: Version (0b111)
    - 62 bits: Random data

    Returns:
        str: UUID v7 string (e.g., "01896e3f-5a5e-7b4c-9f3e-d1234567890a")

    Note:
        Python's uuid module doesn't natively support UUID v7 (as of Python 3.11).
        For production use, consider using the 'uuid6' package.
        For this demo, we use UUID v5 deterministic generation from the source data.
    """
    # Fallback: Use UUID v4 (random) for now
    # In production, use: from uuid6 import uuid7; return str(uuid7())
    return str(uuid.uuid4())


def generate_batch_uuids(count: int) -> List[str]:
    """
    Generate multiple UUIDs for batch operations.

    Args:
        count: Number of UUIDs to generate

    Returns:
        List of UUID strings
    """
    return [generate_uuid_v7() for _ in range(count)]


def is_valid_uuid(uuid_string: str) -> bool:
    """
    Validate UUID string format.

    Args:
        uuid_string: UUID string to validate

    Returns:
        bool: True if valid UUID format
    """
    try:
        uuid.UUID(uuid_string)
        return True
    except (ValueError, AttributeError, TypeError):
        return False


def generate_deterministic_uuid(data: str) -> str:
    """
    Generate deterministic UUID v5 based on content.

    This is useful for:
    - Ensuring same content gets same UUID
    - Avoiding duplicates
    - Reproducible ID generation

    Args:
        data: String data to hash (e.g., company_name + job_position + date)

    Returns:
        str: UUID v5 string

    Example:
        >>> generate_deterministic_uuid("Company_A_Backend_Engineer_2024-01-01")
        'c0ca96e7-85df-50df-a64e-d934cd02a170'
    """
    # Use DNS namespace for deterministic generation
    namespace = uuid.NAMESPACE_DNS
    return str(uuid.uuid5(namespace, data))


if __name__ == '__main__':
    # Test UUID generation
    print("UUID v7 (fallback to v4):")
    for i in range(5):
        print(f"  {i+1}. {generate_uuid_v7()}")

    print("\nDeterministic UUID v5:")
    data = "MyCointainer_Sysadmin_2024-01-01"
    for i in range(3):
        # Same data should produce same UUID
        print(f"  {i+1}. {generate_deterministic_uuid(data)}")

    print("\nValidation:")
    valid_uuid = "c0ca96e7-85df-50df-a64e-d934cd02a170"
    invalid_uuid = "not-a-uuid"
    print(f"  '{valid_uuid}' is valid: {is_valid_uuid(valid_uuid)}")
    print(f"  '{invalid_uuid}' is valid: {is_valid_uuid(invalid_uuid)}")
