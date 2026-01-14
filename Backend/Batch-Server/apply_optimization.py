#!/usr/bin/env python3
"""
Apply Index Optimization
- Remove redundant IVFFlat indexes
- Keep only HNSW indexes
"""

import psycopg2
import time

# Database connection parameters
DB_PARAMS = {
    'host': 'localhost',
    'port': 5433,
    'database': 'alpha_match',
    'user': 'postgres',
    'password': 'season@heaven!2'
}

def connect_db():
    """Connect to PostgreSQL database"""
    try:
        conn = psycopg2.connect(**DB_PARAMS)
        conn.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)
        return conn
    except Exception as e:
        print(f"Error connecting to database: {e}")
        return None

def drop_index(conn, index_name):
    """Drop index CONCURRENTLY"""
    cursor = conn.cursor()

    try:
        print(f"  Dropping index: {index_name}...")
        start = time.time()

        # DROP INDEX CONCURRENTLY
        cursor.execute(f"DROP INDEX CONCURRENTLY IF EXISTS {index_name};")

        end = time.time()
        print(f"  [OK] Dropped {index_name} in {end - start:.2f}s")
        return True
    except Exception as e:
        print(f"  [ERROR] Error dropping {index_name}: {e}")
        return False
    finally:
        cursor.close()

def main():
    print("=" * 80)
    print("Index Optimization - Remove IVFFlat Indexes")
    print("=" * 80)
    print()

    # Connect to database
    conn = connect_db()
    if not conn:
        return

    try:
        # Drop IVFFlat indexes
        indexes_to_drop = [
            'idx_recruit_skills_vector',      # 701 MB
            'idx_skill_vector',                # 2.5 MB
            'idx_candidate_skills_vector',     # 1.6 MB
        ]

        print("Dropping redundant IVFFlat indexes...")
        print("-" * 80)

        success_count = 0
        for index in indexes_to_drop:
            if drop_index(conn, index):
                success_count += 1

        print()
        print("=" * 80)
        print(f"Optimization completed: {success_count}/{len(indexes_to_drop)} indexes removed")
        print("=" * 80)
        print()
        print("Expected benefits:")
        print("  - Space saved: ~705 MB")
        print("  - Ingestion performance: ~22% faster")
        print("  - Search performance: Same or better (HNSW only)")
        print()

    except Exception as e:
        print(f"Error during optimization: {e}")
        import traceback
        traceback.print_exc()

    finally:
        conn.close()

if __name__ == "__main__":
    main()
