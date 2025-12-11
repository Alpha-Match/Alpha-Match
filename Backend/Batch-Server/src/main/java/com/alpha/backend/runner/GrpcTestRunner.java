package com.alpha.backend.runner;

import com.alpha.backend.application.GrpcStreamTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * gRPC 통신 테스트 Runner
 * 애플리케이션 시작 시 gRPC 연결 및 스트리밍 테스트 실행
 *
 * 활성화: application.yml에 grpc.test.enabled=true 설정
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "grpc.test.enabled", havingValue = "true", matchIfMissing = false)
public class GrpcTestRunner implements CommandLineRunner {

    private final GrpcStreamTestService grpcStreamTestService;

    @Override
    public void run(String... args) throws Exception {
        log.info("\n" + "=".repeat(100));
        log.info("Starting gRPC Connection and Streaming Test");
        log.info("=".repeat(100) + "\n");

        try {
            // 1. 연결 테스트
            log.info("[STEP 1] Testing gRPC Connection...");
            grpcStreamTestService.testConnection();
            Thread.sleep(1000); // 잠깐 대기

            // 2. 전체 스트리밍 테스트
            log.info("\n[STEP 2] Testing Full Streaming...");
            grpcStreamTestService.testFullStream();

            log.info("\n" + "=".repeat(100));
            log.info("All gRPC Tests Completed Successfully!");
            log.info("=".repeat(100) + "\n");

        } catch (Exception e) {
            log.error("\n" + "=".repeat(100));
            log.error("gRPC Test Failed!", e);
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
}
