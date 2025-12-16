package com.alpha.backend.application.usecase;

import com.alpha.backend.infrastructure.grpc.client.CacheInvalidateGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 캐시 무효화 서비스 구현체
 *
 * API Server에 도메인별 캐시 무효화 요청을 전송하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationServiceImpl implements CacheInvalidationService {

    private final CacheInvalidateGrpcClient cacheInvalidateGrpcClient;

    @Override
    public Mono<Boolean> invalidateCacheForDomain(String domain) {
        log.info("[CACHE_INVALIDATE_REQUEST] Domain: {}", domain);

        return cacheInvalidateGrpcClient.invalidateCache(domain)
                .doOnSuccess(success -> {
                    if (success) {
                        log.info("[CACHE_INVALIDATE_SUCCESS] Domain: {}", domain);
                    } else {
                        log.warn("[CACHE_INVALIDATE_FAILED] Domain: {}", domain);
                    }
                })
                .doOnError(error ->
                        log.error("[CACHE_INVALIDATE_ERROR] Domain: {} | Error: {}",
                                domain, error.getMessage(), error)
                );
    }

    @Override
    public boolean invalidateCacheForDomainSync(String domain) {
        log.info("[CACHE_INVALIDATE_SYNC_REQUEST] Domain: {}", domain);

        try {
            boolean result = cacheInvalidateGrpcClient.invalidateSafely(domain);

            if (result) {
                log.info("[CACHE_INVALIDATE_SYNC_SUCCESS] Domain: {}", domain);
            } else {
                log.warn("[CACHE_INVALIDATE_SYNC_FAILED] Domain: {}", domain);
            }

            return result;

        } catch (Exception e) {
            log.error("[CACHE_INVALIDATE_SYNC_ERROR] Domain: {} | Error: {}",
                    domain, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Mono<Boolean> invalidateMultipleDomains(String... domains) {
        if (domains == null || domains.length == 0) {
            log.warn("[CACHE_INVALIDATE_MULTIPLE] No domains provided");
            return Mono.just(false);
        }

        log.info("[CACHE_INVALIDATE_MULTIPLE] Invalidating {} domains: {}",
                domains.length, String.join(", ", domains));

        return Flux.fromArray(domains)
                .flatMap(this::invalidateCacheForDomain)
                .reduce(true, (acc, result) -> acc && result)
                .doOnSuccess(allSuccess -> {
                    if (allSuccess) {
                        log.info("[CACHE_INVALIDATE_MULTIPLE_SUCCESS] All domains invalidated successfully");
                    } else {
                        log.warn("[CACHE_INVALIDATE_MULTIPLE_PARTIAL] Some domains failed to invalidate");
                    }
                })
                .doOnError(error ->
                        log.error("[CACHE_INVALIDATE_MULTIPLE_ERROR] Error: {}", error.getMessage(), error)
                );
    }
}
