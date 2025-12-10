package com.alpha.backend.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC 클라이언트 설정
 * Python AI Server 및 API Server와의 gRPC 통신을 위한 채널 구성
 */
@Configuration
@Slf4j
public class GrpcClientConfig {

    @Value("${grpc.client.python-embedding.address:static://localhost:50051}")
    private String pythonEmbeddingAddress;

    @Value("${grpc.client.api-cache.address:static://localhost:50052}")
    private String apiCacheAddress;

    @Value("${grpc.client.python-embedding.max-inbound-message-size:104857600}")
    private int maxInboundMessageSize;

    /**
     * Python AI Server 연결용 gRPC 채널
     * Embedding Stream을 수신하기 위한 채널
     */
    @Bean(name = "pythonEmbeddingChannel")
    public ManagedChannel pythonEmbeddingChannel() {
        String host = extractHost(pythonEmbeddingAddress);
        int port = extractPort(pythonEmbeddingAddress, 50051);

        log.info("Creating gRPC channel for Python Embedding Server: {}:{}", host, port);

        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .maxInboundMessageSize(maxInboundMessageSize)
                .build();
    }

    /**
     * API Server 연결용 gRPC 채널
     * Cache Invalidation 요청을 전송하기 위한 채널
     */
    @Bean(name = "apiCacheChannel")
    public ManagedChannel apiCacheChannel() {
        String host = extractHost(apiCacheAddress);
        int port = extractPort(apiCacheAddress, 50052);

        log.info("Creating gRPC channel for API Cache Server: {}:{}", host, port);

        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    /**
     * 주소에서 호스트 추출
     */
    private String extractHost(String address) {
        // "static://localhost:50051" 형식에서 "localhost" 추출
        String cleaned = address.replace("static://", "");
        return cleaned.contains(":") ? cleaned.substring(0, cleaned.indexOf(":")) : cleaned;
    }

    /**
     * 주소에서 포트 추출
     */
    private int extractPort(String address, int defaultPort) {
        String cleaned = address.replace("static://", "");
        if (cleaned.contains(":")) {
            try {
                return Integer.parseInt(cleaned.substring(cleaned.indexOf(":") + 1));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse port from address: {}, using default: {}", address, defaultPort);
                return defaultPort;
            }
        }
        return defaultPort;
    }
}
