package com.alpha.backend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 3 Configuration
 *
 * Spring Boot 4.0부터 Jackson 3를 사용하며, ObjectMapper 대신 JsonMapper를 권장합니다.
 * JsonMapper는 ObjectMapper의 하위 클래스로 JSON 전용 매퍼입니다.
 *
 * @see <a href="https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring">Spring Jackson 3 Support</a>
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
@Configuration
public class JacksonConfig {

    /**
     * JsonMapper Bean 설정
     *
     * 기능:
     * - 알 수 없는 속성 무시 (FAIL_ON_UNKNOWN_PROPERTIES = false)
     * - 날짜를 ISO-8601 문자열로 직렬화 (WRITE_DATES_AS_TIMESTAMPS = false)
     * - Java 8 Time API 지원 (JavaTimeModule)
     *
     * @return 설정된 JsonMapper 인스턴스
     */
    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                // 알 수 없는 JSON 속성이 있어도 역직렬화 실패하지 않음
                // Python Server에서 추가 필드가 올 경우 대비
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

                // 날짜를 타임스탬프(숫자)가 아닌 ISO-8601 문자열로 직렬화
                // 예: "2025-12-12T10:30:00Z"
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

                // null 값을 가진 필드는 JSON에서 제외하지 않음 (기본값)
                // 필요 시 변경 가능: .serializationInclusion(JsonInclude.Include.NON_NULL)

                // Java 8 날짜/시간 API (LocalDateTime, Instant 등) 지원
                .addModule(new JavaTimeModule())

                .build();
    }
}
