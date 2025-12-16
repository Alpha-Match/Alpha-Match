package com.alpha.backend.application.usecase;

import com.alpha.backend.domain.dlq.entity.DlqEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DLQ (Dead Letter Queue) 서비스 인터페이스
 *
 * 도메인 무관하게 실패한 레코드를 저장하고 재처리하는 공통 인터페이스
 */
public interface DlqService {

    /**
     * 실패한 레코드를 DLQ에 저장
     *
     * @param domain 도메인 이름 (예: "recruit", "candidate")
     * @param entityId 실패한 엔티티의 UUID
     * @param errorMessage 에러 메시지
     * @param payloadJson 원본 데이터 JSON
     * @return 저장된 DLQ 엔티티
     */
    DlqEntity saveToDlq(String domain, UUID entityId, String errorMessage, String payloadJson);

    /**
     * 특정 도메인의 모든 DLQ 레코드 조회
     *
     * @param domain 도메인 이름
     * @return DLQ 레코드 리스트
     */
    List<DlqEntity> findByDomain(String domain);

    /**
     * 특정 도메인 + 엔티티 ID에 대한 DLQ 레코드 조회
     *
     * @param domain 도메인 이름
     * @param entityId 엔티티 UUID
     * @return DLQ 레코드 리스트
     */
    List<DlqEntity> findByDomainAndEntityId(String domain, UUID entityId);

    /**
     * 특정 도메인의 재처리 대상 DLQ 레코드 조회 (생성일 오름차순)
     *
     * @param domain 도메인 이름
     * @return DLQ 레코드 리스트
     */
    List<DlqEntity> findRetryTargets(String domain);

    /**
     * 특정 기간 내 DLQ 레코드 조회 (도메인별)
     *
     * @param domain 도메인 이름
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return DLQ 레코드 리스트
     */
    List<DlqEntity> findByDateRange(String domain, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 도메인의 DLQ 레코드 개수 조회
     *
     * @param domain 도메인 이름
     * @return DLQ 레코드 개수
     */
    long countByDomain(String domain);

    /**
     * DLQ 레코드 삭제
     *
     * @param dlqId DLQ 엔티티 ID
     */
    void deleteById(Long dlqId);

    /**
     * 모든 DLQ 레코드 조회 (전체 도메인)
     *
     * @return 전체 DLQ 레코드 리스트
     */
    List<DlqEntity> findAll();
}
