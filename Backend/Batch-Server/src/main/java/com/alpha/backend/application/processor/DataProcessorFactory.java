package com.alpha.backend.application.processor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 데이터 프로세서 팩토리
 *
 * Python 서버의 get_loader() 팩토리 함수와 대응
 * 도메인 이름에 따라 적절한 프로세서를 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataProcessorFactory {

    private final List<DataProcessor<?>> processors;
    private final Map<String, DataProcessor<?>> processorRegistry = new HashMap<>();

    /**
     * Spring Bean 초기화 시 모든 프로세서를 레지스트리에 등록
     */
    @PostConstruct
    public void init() {
        for (DataProcessor<?> processor : processors) {
            String domain = processor.getDomain();
            processorRegistry.put(domain, processor);
            log.info("[PROCESSOR_REGISTRY] Registered processor for domain: {}", domain);
        }

        if (processorRegistry.isEmpty()) {
            log.warn("[PROCESSOR_REGISTRY] No processors registered!");
        } else {
            log.info("[PROCESSOR_REGISTRY] Total registered processors: {} | Domains: {}",
                    processorRegistry.size(), processorRegistry.keySet());
        }
    }

    /**
     * 도메인 이름으로 프로세서 조회
     *
     * @param domain 도메인 이름 (예: "recruit", "candidate")
     * @return 해당 도메인의 프로세서
     * @throws IllegalArgumentException 지원하지 않는 도메인인 경우
     */
    public DataProcessor<?> getProcessor(String domain) {
        DataProcessor<?> processor = processorRegistry.get(domain);

        if (processor == null) {
            log.error("[PROCESSOR_NOT_FOUND] Domain: {} | Available domains: {}",
                    domain, processorRegistry.keySet());
            throw new IllegalArgumentException(
                    String.format("지원하지 않는 도메인입니다: '%s'. 사용 가능한 도메인: %s",
                            domain, processorRegistry.keySet())
            );
        }

        log.debug("[PROCESSOR_FOUND] Domain: {} | Processor: {}",
                domain, processor.getClass().getSimpleName());

        return processor;
    }

    /**
     * 등록된 모든 도메인 목록 조회
     *
     * @return 도메인 이름 집합
     */
    public java.util.Set<String> getSupportedDomains() {
        return processorRegistry.keySet();
    }
}
