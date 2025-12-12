package com.alpha.backend.runner;

import com.alpha.backend.application.EmbeddingStreamingService;
import com.alpha.backend.application.EmbeddingStreamingService.StreamingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Embedding Streaming Runner
 * 애플리케이션 시작 시 Embedding Streaming 처리 및 DB 저장 테스트 실행
 *
 * 활성화: application.yml에 grpc.test.enabled=true 설정
 *
 * 핵심 플로우:
 * 1. Python gRPC Stream 수신
 * 2. Chunk 단위로 분할
 * 3. Virtual Thread로 전환
 * 4. Blocking JPA로 DB 저장
 * 5. 상세 로깅 (스레드, 청크 사이즈, 마지막 UUID, 마지막 데이터)
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "grpc.test.enabled", havingValue = "true", matchIfMissing = false)
public class EmbeddingStreamRunner implements CommandLineRunner {

    private final EmbeddingStreamingService embeddingStreamingService;

    @Override
    public void run(String... args) throws Exception {
        log.info("\n" + "=".repeat(100));
        log.info("Starting Embedding Streaming and DB Storage Test");
        log.info("=".repeat(100) + "\n");

        try {
            // 옵션 1: 전체 데이터 스트리밍 (처음부터)
            // testFullStreaming();

            // 옵션 2: Checkpoint부터 재시작
            testCheckpointStreaming();

            // 옵션 3: 병렬 스트리밍 (고급)
            // testParallelStreaming();

        } catch (Exception e) {
            log.error("\n" + "=".repeat(100));
            log.error("Embedding Streaming Test Failed!", e);
            log.error("=".repeat(100) + "\n");

            // Python 서버 연결 실패 시 안내 메시지
            if (e.getMessage() != null && e.getMessage().contains("UNAVAILABLE")) {
                log.error("\n" + "!".repeat(100));
                log.error("Python gRPC Server is not available!");
                log.error("Please make sure Python server is running on localhost:50051");
                log.error("Command: cd Demo-Python && python src/grpc_server.py");
                log.error("!".repeat(100) + "\n");
            }

            // 테스트 실패해도 애플리케이션은 계속 실행
            // 운영 환경에서는 이 Runner가 비활성화됨
        }
    }

    /**
     * 전체 데이터 스트리밍 테스트 (처음부터 시작)
     */
    private void testFullStreaming() {
        log.info("[TEST 1] Full Streaming Test (from beginning)");
        log.info("-".repeat(80));

        StreamingResult result = embeddingStreamingService.streamAllData()
                .doOnSubscribe(subscription -> log.info("Streaming started..."))
                .doOnNext(r -> log.info("Streaming completed: {}", r))
                .doOnError(error -> log.error("Streaming error: {}", error.getMessage(), error))
                .block();  // Blocking for test purposes

        if (result != null) {
            printResult(result);
        }
    }

    /**
     * Checkpoint부터 스트리밍 테스트 (재시작)
     */
    private void testCheckpointStreaming() {
        log.info("[TEST 2] Checkpoint Streaming Test (resume from checkpoint)");
        log.info("-".repeat(80));

        StreamingResult result = embeddingStreamingService.streamFromCheckpoint()
                .doOnSubscribe(subscription -> log.info("Streaming from checkpoint started..."))
                .doOnNext(r -> log.info("Streaming completed: {}", r))
                .doOnError(error -> log.error("Streaming error: {}", error.getMessage(), error))
                .block();  // Blocking for test purposes

        if (result != null) {
            printResult(result);
        }
    }

    /**
     * 병렬 스트리밍 테스트 (고급 - 청크 재분할 + 병렬 처리)
     */
    private void testParallelStreaming() {
        log.info("[TEST 3] Parallel Streaming Test (advanced)");
        log.info("-".repeat(80));
        log.info("Configuration: parallelism=4, subChunkSize=50");

        StreamingResult result = embeddingStreamingService.streamWithParallelism(
                        null,   // 처음부터
                        4,      // 병렬도
                        50      // 서브 청크 크기
                )
                .doOnSubscribe(subscription -> log.info("Parallel streaming started..."))
                .doOnNext(r -> log.info("Parallel streaming completed: {}", r))
                .doOnError(error -> log.error("Parallel streaming error: {}", error.getMessage(), error))
                .block();  // Blocking for test purposes

        if (result != null) {
            printResult(result);
        }
    }

    /**
     * 결과 출력
     */
    private void printResult(StreamingResult result) {
        log.info("\n" + "=".repeat(100));
        log.info("Streaming Result Summary");
        log.info("=".repeat(100));
        log.info("Total Chunks Processed: {}", result.totalChunks());
        log.info("Total Rows Processed: {}", result.totalRows());
        log.info("Last Processed UUID: {}", result.lastUuid());
        log.info("Total Processing Time: {} ms ({} seconds)",
                result.elapsedTimeMs(), result.elapsedTimeMs() / 1000.0);

        if (result.totalRows() > 0) {
            double rowsPerSecond = (double) result.totalRows() / (result.elapsedTimeMs() / 1000.0);
            log.info("Processing Speed: {}/sec", String.format("%.2f", rowsPerSecond));
        }

        log.info("=".repeat(100) + "\n");
    }
}
