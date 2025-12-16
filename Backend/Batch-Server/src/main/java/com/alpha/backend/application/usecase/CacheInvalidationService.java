package com.alpha.backend.application.usecase;

import reactor.core.publisher.Mono;

/**
 * 캐시 무효화 서비스 인터페이스
 *
 * API Server에 도메인별 캐시 무효화 요청을 전송하는 서비스
 */
public interface CacheInvalidationService {

    /**
     * 도메인별 캐시 무효화 요청 (Reactive)
     *
     * @param domain 무효화할 도메인 (예: "recruit", "candidate")
     * @return Mono<Boolean> 성공 여부
     */
    Mono<Boolean> invalidateCacheForDomain(String domain);

    /**
     * 도메인별 캐시 무효화 요청 (동기)
     *
     * @param domain 무효화할 도메인
     * @return 성공 여부
     */
    boolean invalidateCacheForDomainSync(String domain);

    /**
     * 여러 도메인의 캐시 일괄 무효화 (Reactive)
     *
     * @param domains 무효화할 도메인 목록
     * @return Mono<Boolean> 모든 무효화 성공 여부
     */
    Mono<Boolean> invalidateMultipleDomains(String... domains);
}
