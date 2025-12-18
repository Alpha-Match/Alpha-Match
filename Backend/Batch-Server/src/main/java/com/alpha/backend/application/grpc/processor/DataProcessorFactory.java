package com.alpha.backend.application.grpc.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DataProcessor Factory
 * <p>
 * 도메인 이름으로 적절한 Processor를 선택하는 Factory 패턴 구현
 * Spring이 모든 DataProcessor 구현체를 자동으로 주입하여 Map으로 관리
 */
@Slf4j
@Component
public class DataProcessorFactory {

    private final Map<String, DataProcessor> processors;

    /**
     * Spring이 모든 DataProcessor 빈을 자동으로 주입
     * <p>
     * @Component가 붙은 모든 DataProcessor 구현체가 자동으로 등록됨:
     * - RecruitDataProcessor → "recruit"
     * - CandidateDataProcessor → "candidate"
     */
    public DataProcessorFactory(List<DataProcessor> processorList) {
        this.processors = processorList.stream()
                .collect(Collectors.toMap(
                        DataProcessor::getDomain,
                        Function.identity()
                ));

        log.info("Registered {} data processors: {}",
                processors.size(),
                processors.keySet());
    }

    /**
     * 도메인 이름으로 적절한 프로세서 반환
     *
     * @param domain 도메인 이름 (예: "recruit", "candidate")
     * @return 해당 도메인의 DataProcessor
     * @throws IllegalArgumentException 지원하지 않는 도메인인 경우
     */
    public DataProcessor getProcessor(String domain) {
        DataProcessor processor = processors.get(domain);

        if (processor == null) {
            log.error("Unsupported domain: {}", domain);
            throw new IllegalArgumentException("지원하지 않는 도메인: " + domain);
        }

        log.debug("Selected processor for domain: {} -> {}", domain, processor.getClass().getSimpleName());
        return processor;
    }

    /**
     * 등록된 모든 도메인 목록 반환
     */
    public List<String> getSupportedDomains() {
        return List.copyOf(processors.keySet());
    }
}
