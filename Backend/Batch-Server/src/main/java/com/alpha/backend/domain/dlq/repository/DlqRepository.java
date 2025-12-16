package com.alpha.backend.domain.dlq.repository;

import com.alpha.backend.domain.dlq.entity.DlqEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DLQ Repository (Domain Interface)
 * 처리 실패한 레코드를 관리하는 도메인 레포지토리 인터페이스
 *
 * Clean Architecture: Domain 계층의 추상화 (Port)
 * 실제 구현은 Infrastructure 계층에서 담당
 */
public interface DlqRepository {

    /**
     * 특정 도메인의 DLQ 레코드 조회
     */
    List<DlqEntity> findByDomain(String domain);

    /**
     * 특정 엔티티 ID에 대한 DLQ 레코드 조회
     */
    List<DlqEntity> findByEntityId(UUID entityId);

    /**
     * 특정 기간 내 DLQ 레코드 조회
     */
    List<DlqEntity> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 재처리 대상 DLQ 레코드 조회 (생성일 기준 정렬)
     */
    List<DlqEntity> findAllOrderByCreatedAtAsc();

    /**
     * 특정 도메인의 DLQ 레코드를 생성일 기준으로 조회
     */
    List<DlqEntity> findByDomainOrderByCreatedAtAsc(String domain);

    /**
     * 특정 도메인 및 엔티티 ID에 대한 DLQ 레코드 조회
     */
    List<DlqEntity> findByDomainAndEntityId(String domain, UUID entityId);

    /**
     * 특정 도메인의 특정 기간 내 DLQ 레코드 조회
     */
    List<DlqEntity> findByDomainAndDateRange(String domain, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 도메인의 DLQ 레코드 개수 조회
     */
    long countByDomain(String domain);
}
