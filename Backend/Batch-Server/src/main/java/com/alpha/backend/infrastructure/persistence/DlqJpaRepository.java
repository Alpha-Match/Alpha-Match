package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.dlq.entity.DlqEntity;
import com.alpha.backend.domain.dlq.repository.DlqRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DLQ JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 */
@Repository
public interface DlqJpaRepository extends JpaRepository<DlqEntity, Long>, DlqRepository {

    /**
     * 특정 도메인의 DLQ 레코드 조회
     */
    @Override
    List<DlqEntity> findByDomain(String domain);

    /**
     * 특정 엔티티 ID에 대한 DLQ 레코드 조회
     */
    @Override
    List<DlqEntity> findByEntityId(UUID entityId);

    /**
     * 특정 기간 내 DLQ 레코드 조회
     */
    @Override
    @Query("SELECT d FROM DlqEntity d WHERE d.createdAt BETWEEN :startDate AND :endDate")
    List<DlqEntity> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * 재처리 대상 DLQ 레코드 조회 (생성일 기준 정렬)
     */
    @Override
    @Query("SELECT d FROM DlqEntity d ORDER BY d.createdAt ASC")
    List<DlqEntity> findAllOrderByCreatedAtAsc();

    /**
     * 특정 도메인의 DLQ 레코드를 생성일 기준으로 조회
     */
    @Override
    @Query("SELECT d FROM DlqEntity d WHERE d.domain = :domain ORDER BY d.createdAt ASC")
    List<DlqEntity> findByDomainOrderByCreatedAtAsc(@Param("domain") String domain);

    /**
     * 특정 도메인 및 엔티티 ID에 대한 DLQ 레코드 조회
     */
    @Override
    @Query("SELECT d FROM DlqEntity d WHERE d.domain = :domain AND d.entityId = :entityId")
    List<DlqEntity> findByDomainAndEntityId(@Param("domain") String domain, @Param("entityId") UUID entityId);

    /**
     * 특정 도메인의 특정 기간 내 DLQ 레코드 조회
     */
    @Override
    @Query("SELECT d FROM DlqEntity d WHERE d.domain = :domain AND d.createdAt BETWEEN :startDate AND :endDate")
    List<DlqEntity> findByDomainAndDateRange(@Param("domain") String domain,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 도메인의 DLQ 레코드 개수 조회
     */
    @Override
    long countByDomain(String domain);
}
