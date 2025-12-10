package com.alpha.backend.grpc;

import com.alpha.backend.grpc.proto.CacheInvalidateRequest;
import com.alpha.backend.grpc.proto.CacheInvalidateResponse;
import com.alpha.backend.grpc.proto.CacheServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cache Invalidate gRPC Client
 * API Server에 캐시 무효화 요청을 전송하는 클라이언트
 */
@Component
@Slf4j
public class CacheInvalidateGrpcClient {

    private final ManagedChannel channel;
    private final CacheServiceGrpc.CacheServiceBlockingStub blockingStub;
    private final AtomicBoolean invalidating = new AtomicBoolean(false);

    public CacheInvalidateGrpcClient(@Qualifier("apiCacheChannel") ManagedChannel channel) {
        this.channel = channel;
        this.blockingStub = CacheServiceGrpc.newBlockingStub(channel);
        log.info("CacheInvalidateGrpcClient initialized");
    }

    /**
     * 캐시 무효화 요청 (Reactive)
     * Race Condition 방지를 위한 AtomicBoolean 사용
     *
     * @param target 무효화할 캐시 대상 (예: "recruit")
     * @return Mono<Boolean> 성공 여부
     */
    public Mono<Boolean> invalidateCache(String target) {
        return Mono.defer(() -> {
            // 중복 호출 방지
            if (!invalidating.compareAndSet(false, true)) {
                log.warn("Cache invalidation already in progress, skipping");
                return Mono.just(false);
            }

            try {
                log.info("Sending cache invalidation request for target: {}", target);

                CacheInvalidateRequest request = CacheInvalidateRequest.newBuilder()
                        .setTarget(target)
                        .build();

                CacheInvalidateResponse response = blockingStub
                        .withDeadlineAfter(10, TimeUnit.SECONDS)
                        .invalidateCache(request);

                log.info("Cache invalidation response - success: {}, message: {}",
                        response.getSuccess(), response.getMessage());

                return Mono.just(response.getSuccess());

            } catch (StatusRuntimeException e) {
                log.error("gRPC error during cache invalidation: {}", e.getStatus(), e);
                return Mono.error(e);
            } catch (Exception e) {
                log.error("Unexpected error during cache invalidation", e);
                return Mono.error(e);
            } finally {
                invalidating.set(false);
            }
        })
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(5))
                .doBeforeRetry(retrySignal ->
                        log.warn("Retrying cache invalidation (attempt: {}): {}",
                                retrySignal.totalRetries() + 1,
                                retrySignal.failure().getMessage())
                )
        )
        .onErrorResume(e -> {
            log.error("Failed to invalidate cache after retries: {}", e.getMessage());
            return Mono.just(false);
        });
    }

    /**
     * 안전한 캐시 무효화 (동기 방식)
     * AtomicBoolean을 사용하여 중복 호출 방지
     */
    public boolean invalidateSafely(String target) {
        if (invalidating.compareAndSet(false, true)) {
            try {
                log.info("Safely invalidating cache for target: {}", target);

                CacheInvalidateRequest request = CacheInvalidateRequest.newBuilder()
                        .setTarget(target)
                        .build();

                CacheInvalidateResponse response = blockingStub
                        .withDeadlineAfter(10, TimeUnit.SECONDS)
                        .invalidateCache(request);

                log.info("Cache invalidation completed - success: {}", response.getSuccess());
                return response.getSuccess();

            } catch (Exception e) {
                log.error("Error during safe cache invalidation", e);
                return false;
            } finally {
                invalidating.set(false);
            }
        } else {
            log.warn("Cache invalidation already in progress, skipping");
            return false;
        }
    }

    /**
     * Channel 종료
     */
    public void shutdown() {
        try {
            log.info("Shutting down CacheInvalidateGrpcClient channel");
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error shutting down channel", e);
            Thread.currentThread().interrupt();
        }
    }
}
