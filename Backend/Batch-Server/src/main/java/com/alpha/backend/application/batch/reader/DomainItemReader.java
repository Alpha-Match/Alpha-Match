package com.alpha.backend.application.batch.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemReader;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Domain ItemReader (추상 클래스)
 *
 * gRPC Streaming으로 수신한 데이터를 개별 Row로 분해하여 반환
 *
 * 메모리 최적화:
 * - Proto 객체를 그대로 Queue에 저장 (역직렬화 지연)
 * - Processor에서 Entity로 변환할 때만 역직렬화
 *
 * Thread-Safe:
 * - BlockingQueue 사용으로 멀티스레드 환경에서 안전
 * - AtomicBoolean으로 스트림 완료 상태 관리
 *
 * @param <T> Proto Row 타입 (RecruitRow, CandidateRow 등)
 */
@Slf4j
public abstract class DomainItemReader<T> implements ItemReader<T> {

    // Proto 객체를 그대로 저장 (메모리 효율적)
    protected final BlockingQueue<T> rowQueue = new LinkedBlockingQueue<>(1000);
    protected final AtomicBoolean streamCompleted = new AtomicBoolean(false);
    protected final AtomicBoolean streamStarted = new AtomicBoolean(false);

    /**
     * Spring Batch가 호출하는 read() 메서드
     *
     * @return Proto Row 객체 (스트림 완료 시 null)
     */
    @Override
    public T read() throws Exception {
        // 첫 read() 호출 시 스트림 시작
        if (streamStarted.compareAndSet(false, true)) {
            startStreaming();
        }

        // Queue에서 row 추출 (blocking)
        while (true) {
            T row = rowQueue.poll();

            if (row != null) {
                return row;
            }

            // Queue가 비어있고 스트림이 완료되었으면 종료
            if (streamCompleted.get() && rowQueue.isEmpty()) {
                log.info("[READER] Stream completed, no more rows to read");
                return null;  // Spring Batch에게 읽기 완료 신호
            }

            // Queue가 비어있지만 스트림이 아직 진행 중이면 대기
            Thread.sleep(10);
        }
    }

    /**
     * gRPC 스트리밍 시작 (하위 클래스에서 구현)
     */
    protected abstract void startStreaming();

    /**
     * 도메인 이름 반환 (로깅용)
     */
    protected abstract String getDomainName();

    /**
     * 현재 Queue 크기 반환 (모니터링용)
     */
    public int getQueueSize() {
        return rowQueue.size();
    }

    /**
     * 스트림 완료 여부 반환 (모니터링용)
     */
    public boolean isStreamCompleted() {
        return streamCompleted.get();
    }
}
