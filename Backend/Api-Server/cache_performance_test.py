#!/usr/bin/env python3
"""
Caffeine Cache Performance Test
- L1 Cache (Caffeine) 성능 측정
- getSkillCategories 엔드포인트 대상
- 캐시 히트/미스 비교
"""

import requests
import time
import statistics
import json

GRAPHQL_URL = "http://localhost:8088/graphql"

SKILL_CATEGORIES_QUERY = """
query {
  getSkillCategories {
    category
    skills
  }
}
"""

def measure_request():
    """단일 요청 시간 측정"""
    start = time.time()
    response = requests.post(
        GRAPHQL_URL,
        json={"query": SKILL_CATEGORIES_QUERY},
        headers={"Content-Type": "application/json"},
        timeout=5
    )
    end = time.time()

    if response.status_code != 200:
        raise Exception(f"Request failed: {response.status_code}")

    return (end - start) * 1000  # ms

def run_test(num_requests=50, test_name="Test"):
    """성능 테스트 실행"""
    print(f"\n{'='*60}")
    print(f"Test: {test_name} ({num_requests} requests)")
    print(f"{'='*60}")

    response_times = []

    for i in range(num_requests):
        try:
            elapsed_ms = measure_request()
            response_times.append(elapsed_ms)

            if (i + 1) % 10 == 0:
                print(f"  Progress: {i + 1}/{num_requests} - Last: {elapsed_ms:.2f}ms")
        except Exception as e:
            print(f"  Request {i + 1} failed: {e}")
            continue

    if not response_times:
        print("  All requests failed!")
        return None

    # 통계 계산
    avg = statistics.mean(response_times)
    median = statistics.median(response_times)
    min_time = min(response_times)
    max_time = max(response_times)
    stdev = statistics.stdev(response_times) if len(response_times) > 1 else 0
    p95 = sorted(response_times)[int(len(response_times) * 0.95)]
    p99 = sorted(response_times)[int(len(response_times) * 0.99)]

    print(f"\nResults:")
    print(f"  Average:   {avg:.2f} ms")
    print(f"  Median:    {median:.2f} ms")
    print(f"  Min:       {min_time:.2f} ms")
    print(f"  Max:       {max_time:.2f} ms")
    print(f"  StdDev:    {stdev:.2f} ms")
    print(f"  P95:       {p95:.2f} ms")
    print(f"  P99:       {p99:.2f} ms")
    print(f"  Total:     {sum(response_times):.2f} ms")
    print(f"  RPS:       {num_requests / (sum(response_times) / 1000):.2f}")

    return {
        "avg": avg,
        "median": median,
        "min": min_time,
        "max": max_time,
        "stdev": stdev,
        "p95": p95,
        "p99": p99,
        "rps": num_requests / (sum(response_times) / 1000)
    }

def main():
    print("\n" + "="*60)
    print("Caffeine Cache Performance Test")
    print("="*60)

    # Phase 1: Cold Start (첫 요청 - DB 조회)
    print("\nPhase 1: Cold Start (DB Query)")
    print("Starting test in 3 seconds...")
    time.sleep(3)

    cold_result = run_test(num_requests=1, test_name="Cold Start")

    # Phase 2: Warm Cache (10초 TTL 내 반복 요청)
    print("\nPhase 2: Warm Cache (L1 Caffeine Hit)")
    print("10초 TTL 내에 연속 요청합니다.")
    time.sleep(1)  # 1초 대기

    warm_result = run_test(num_requests=50, test_name="Warm Cache")

    # Phase 3: Cache Expiry (10초 대기 후)
    print("\nPhase 3: Cache Expiry (TTL 만료)")
    print("10초 대기 후 캐시가 만료됩니다...")
    time.sleep(11)  # 11초 대기 (TTL 10초 + 여유)

    expired_result = run_test(num_requests=1, test_name="Cache Expired")

    # 결과 비교
    print("\n\n" + "="*60)
    print("Performance Comparison")
    print("="*60)

    if cold_result and warm_result and expired_result:
        print(f"\n1. Cold Start (DB):        {cold_result['avg']:.2f} ms")
        print(f"2. Warm Cache (L1 Hit):   {warm_result['avg']:.2f} ms")
        print(f"3. Cache Expired (DB):    {expired_result['avg']:.2f} ms")

        speedup = cold_result['avg'] / warm_result['avg']
        print(f"\nCache Speedup: {speedup:.1f}x faster")
        print(f"   ({cold_result['avg']:.2f} ms -> {warm_result['avg']:.2f} ms)")

        # 결과 저장
        results = {
            "test_date": time.strftime("%Y-%m-%d %H:%M:%S"),
            "cold_start": cold_result,
            "warm_cache": warm_result,
            "cache_expired": expired_result,
            "speedup": speedup
        }

        with open("cache_test_results.json", "w") as f:
            json.dump(results, f, indent=2)

        print(f"\nResults saved to: cache_test_results.json")

    print("\n" + "="*60)
    print("Test completed!")
    print("="*60 + "\n")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\nTest interrupted by user")
    except Exception as e:
        print(f"\n\nTest failed: {e}")
        import traceback
        traceback.print_exc()
