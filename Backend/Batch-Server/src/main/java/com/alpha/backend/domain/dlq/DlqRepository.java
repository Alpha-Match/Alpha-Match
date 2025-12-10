package com.alpha.backend.domain.dlq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DLQ (Dead Letter Queue) Repository
 * 처리 실패한 레코드를 관리하는 레포지토리
 */
@Repository
public interface DlqRepository extends JpaRepository<DlqEntity, Long> {

    /**
     * 특정 Recruit ID에 대한 DLQ 레코드 조회
     */
    List<DlqEntity> findByRecruitId(UUID recruitId);

    /**
     * 특정 기간 내 DLQ 레코드 조회
     */
    @Query("SELECT d FROM DlqEntity d WHERE d.createdAt BETWEEN :startDate AND :endDate")
    List<DlqEntity> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * 재처리 대상 DLQ 레코드 조회 (생성일 기준 정렬)
     */
    @Query("SELECT d FROM DlqEntity d ORDER BY d.createdAt ASC")
    List<DlqEntity> findAllOrderByCreatedAtAsc();
}
