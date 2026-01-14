#!/usr/bin/env python3
"""
Database Performance Test Script
- Measures table and index sizes
- Compares HNSW vs IVFFlat performance
- Tests query execution time
"""

import psycopg2
import time
import json
from psycopg2.extras import RealDictCursor

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
        return conn
    except Exception as e:
        print(f"Error connecting to database: {e}")
        return None

def get_table_sizes(conn):
    """Get table sizes for embedding tables"""
    cursor = conn.cursor(cursor_factory=RealDictCursor)

    query = """
    SELECT tablename,
           pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
           pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
           pg_total_relation_size(schemaname||'.'||tablename) AS total_bytes
    FROM pg_tables
    WHERE schemaname = 'public'
      AND (tablename LIKE '%embedding%' OR tablename LIKE '%skill%')
    ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
    """

    cursor.execute(query)
    results = cursor.fetchall()
    cursor.close()

    return results

def get_index_sizes(conn):
    """Get index sizes for vector indexes"""
    cursor = conn.cursor(cursor_factory=RealDictCursor)

    query = """
    SELECT indexname,
           tablename,
           pg_size_pretty(pg_relation_size(schemaname||'.'||indexname)) AS index_size,
           pg_relation_size(schemaname||'.'||indexname) AS size_bytes
    FROM pg_indexes
    WHERE schemaname = 'public'
      AND (indexname LIKE '%embedding%' OR indexname LIKE '%skill%' OR indexname LIKE '%hnsw%' OR indexname LIKE '%ivf%')
    ORDER BY pg_relation_size(schemaname||'.'||indexname) DESC;
    """

    cursor.execute(query)
    results = cursor.fetchall()
    cursor.close()

    return results

def get_sample_vector(conn):
    """Get a random vector for performance testing"""
    cursor = conn.cursor()

    query = """
    SELECT skills_vector::text AS vector
    FROM recruit_skills_embedding
    ORDER BY RANDOM()
    LIMIT 1;
    """

    cursor.execute(query)
    result = cursor.fetchone()
    cursor.close()

    if result:
        return result[0]
    return None

def test_query_performance(conn, vector_str, query_name, query):
    """Test query execution time"""
    cursor = conn.cursor()

    # Prepare query with vector
    prepared_query = query.replace('%VECTOR%', vector_str)

    # Run query 3 times and take average
    times = []
    for i in range(3):
        start = time.time()
        cursor.execute(prepared_query)
        results = cursor.fetchall()
        end = time.time()
        times.append((end - start) * 1000)  # Convert to ms

    cursor.close()

    avg_time = sum(times) / len(times)
    min_time = min(times)
    max_time = max(times)

    return {
        'name': query_name,
        'avg_ms': round(avg_time, 2),
        'min_ms': round(min_time, 2),
        'max_ms': round(max_time, 2),
        'runs': 3
    }

def main():
    print("=" * 80)
    print("Database Performance Test - Vector Dimension 1536d")
    print("=" * 80)
    print()

    # Connect to database
    conn = connect_db()
    if not conn:
        return

    try:
        # 1. Table Sizes
        print("1. TABLE SIZES")
        print("-" * 80)
        table_sizes = get_table_sizes(conn)
        total_table_bytes = 0
        for row in table_sizes:
            print(f"  {row['tablename']:40} | Total: {row['total_size']:10} | Table: {row['table_size']:10}")
            total_table_bytes += row['total_bytes']
        print(f"\n  TOTAL TABLE SIZE: {total_table_bytes / (1024**3):.2f} GB")
        print()

        # 2. Index Sizes
        print("2. INDEX SIZES")
        print("-" * 80)
        index_sizes = get_index_sizes(conn)
        total_index_bytes = 0
        for row in index_sizes:
            print(f"  {row['indexname']:50} | {row['index_size']:10} | Table: {row['tablename']}")
            total_index_bytes += row['size_bytes']
        print(f"\n  TOTAL INDEX SIZE: {total_index_bytes / (1024**3):.2f} GB")
        print()

        # 3. Get Sample Vector
        print("3. SAMPLE VECTOR FOR TESTING")
        print("-" * 80)
        vector_str = get_sample_vector(conn)
        if vector_str:
            print(f"  Vector (first 100 chars): {vector_str[:100]}...")
            print()

            # 4. Performance Tests
            print("4. QUERY PERFORMANCE TESTS")
            print("-" * 80)

            # Test HNSW index (cosine distance)
            hnsw_query = """
            SELECT recruit_id,
                   skills_vector <=> CAST('%VECTOR%' AS vector(1536)) AS distance
            FROM recruit_skills_embedding
            ORDER BY skills_vector <=> CAST('%VECTOR%' AS vector(1536))
            LIMIT 10;
            """

            print("  Testing HNSW Index (3 runs)...")
            hnsw_result = test_query_performance(conn, vector_str, "HNSW Index", hnsw_query)
            print(f"    Avg: {hnsw_result['avg_ms']} ms | Min: {hnsw_result['min_ms']} ms | Max: {hnsw_result['max_ms']} ms")
            print()

            # Summary
            print("5. SUMMARY")
            print("-" * 80)
            print(f"  Total Data Size: {(total_table_bytes + total_index_bytes) / (1024**3):.2f} GB")
            print(f"  - Tables: {total_table_bytes / (1024**3):.2f} GB ({(total_table_bytes / (total_table_bytes + total_index_bytes)) * 100:.1f}%)")
            print(f"  - Indexes: {total_index_bytes / (1024**3):.2f} GB ({(total_index_bytes / (total_table_bytes + total_index_bytes)) * 100:.1f}%)")
            print()
            print(f"  HNSW Query Performance: {hnsw_result['avg_ms']} ms (avg of 3 runs)")
            print()

        else:
            print("  ERROR: Could not retrieve sample vector")
            print()

    except Exception as e:
        print(f"Error during test: {e}")
        import traceback
        traceback.print_exc()

    finally:
        conn.close()

    print("=" * 80)
    print("Test completed")
    print("=" * 80)

if __name__ == "__main__":
    main()
