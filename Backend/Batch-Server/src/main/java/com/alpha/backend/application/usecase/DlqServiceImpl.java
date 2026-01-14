package com.alpha.backend.application.usecase;

import com.alpha.backend.domain.dlq.entity.DlqEntity;
import com.alpha.backend.infrastructure.persistence.DlqJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DLQ (Dead Letter Queue) 서비스 구현체
 *
 * 도메인 무관하게 실패한 레코드를 저장하고 재처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DlqServiceImpl implements DlqService {

    private final DlqJpaRepository dlqRepository;

    @Override
    @Transactional
    public DlqEntity saveToDlq(String domain, UUID failedId, String errorMessage, String payloadJson) {
        log.warn("[DLQ_SAVE] Domain: {} | Failed ID: {} | Error: {}",
                domain, failedId, errorMessage);

        DlqEntity dlqEntity = DlqEntity.create(domain, failedId, errorMessage, payloadJson);
        DlqEntity saved = dlqRepository.save(dlqEntity);

        log.info("[DLQ_SAVED] DLQ ID: {} | Domain: {} | Failed ID: {}",
                saved.getId(), domain, failedId);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqEntity> findByDomain(String domain) {
        log.debug("[DLQ_FIND_BY_DOMAIN] Domain: {}", domain);
        List<DlqEntity> results = dlqRepository.findByDomain(domain);
        log.debug("[DLQ_FIND_BY_DOMAIN] Found {} records for domain: {}", results.size(), domain);
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqEntity> findByDomainAndFailedId(String domain, UUID failedId) {
        log.debug("[DLQ_FIND_BY_DOMAIN_AND_ID] Domain: {} | Failed ID: {}", domain, failedId);
        List<DlqEntity> results = dlqRepository.findByDomainAndFailedId(domain, failedId);
        log.debug("[DLQ_FIND_BY_DOMAIN_AND_ID] Found {} records", results.size());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqEntity> findRetryTargets(String domain) {
        log.debug("[DLQ_FIND_RETRY_TARGETS] Domain: {}", domain);
        List<DlqEntity> results = dlqRepository.findByDomainOrderByCreatedAtAsc(domain);
        log.info("[DLQ_FIND_RETRY_TARGETS] Found {} retry targets for domain: {}", results.size(), domain);
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqEntity> findByDateRange(String domain, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("[DLQ_FIND_BY_DATE_RANGE] Domain: {} | Start: {} | End: {}",
                domain, startDate, endDate);
        List<DlqEntity> results = dlqRepository.findByDomainAndDateRange(domain, startDate, endDate);
        log.debug("[DLQ_FIND_BY_DATE_RANGE] Found {} records", results.size());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDomain(String domain) {
        long count = dlqRepository.countByDomain(domain);
        log.debug("[DLQ_COUNT] Domain: {} | Count: {}", domain, count);
        return count;
    }

    @Override
    @Transactional
    public void deleteById(Long dlqId) {
        log.info("[DLQ_DELETE] DLQ ID: {}", dlqId);
        dlqRepository.deleteById(dlqId);
        log.info("[DLQ_DELETED] DLQ ID: {}", dlqId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqEntity> findAll() {
        log.debug("[DLQ_FIND_ALL] Retrieving all DLQ records");
        List<DlqEntity> results = dlqRepository.findAllOrderByCreatedAtAsc();
        log.debug("[DLQ_FIND_ALL] Found {} total records", results.size());
        return results;
    }
}
