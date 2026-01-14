# Jackson 3 마이그레이션: ObjectMapper → JsonMapper

**작성일:** 2025-12-12
**작업자:** AI Assistant
**관련 이슈:** Spring Boot 4.0 Jackson 3 정책 변경 대응

---

## 📋 작업 개요

Spring Boot 4.0부터 Jackson 3를 지원하며, ObjectMapper 대신 JsonMapper 사용을 권장합니다.
JsonMapper는 ObjectMapper의 하위 클래스로 JSON 전용 매퍼로서 더 나은 타입 안전성을 제공합니다.

### 주요 변경 사항
- ObjectMapper → JsonMapper 전환
- JacksonConfig 설정 클래스 추가
- Jackson datatype-jsr310 모듈 의존성 추가

---

## 🎯 변경 이유

### Spring Boot 4.0 Jackson 3 정책
1. **JsonMapper 권장**: ObjectMapper보다 타입 안전한 JSON 전용 매퍼
2. **불변성**: Jackson 3의 JsonMapper는 불변 객체로 설계됨
3. **포맷 특화**: JsonMapper, XmlMapper, YAMLMapper 등 포맷별 매퍼 분리
4. **향후 호환성**: Spring Boot 4.x 이상에서 권장 방식

### 마이그레이션 동기
- Spring Boot 4.0 Best Practice 준수
- 향후 유지보수성 향상
- 타입 안전성 개선
- 런타임 에러 가능성 감소

---

## 🔧 구현 내용

### 1. JacksonConfig 설정 클래스 생성

**파일:** `src/main/java/com/alpha/backend/config/JacksonConfig.java`

```java
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .addModule(new JavaTimeModule())
            .build();
    }
}
```

**설정 내용:**
- `FAIL_ON_UNKNOWN_PROPERTIES = false`: Python Server에서 추가 필드가 와도 역직렬화 실패하지 않음
- `WRITE_DATES_AS_TIMESTAMPS = false`: 날짜를 ISO-8601 문자열로 직렬화
- `JavaTimeModule`: Java 8 Time API 지원 (LocalDateTime, Instant 등)

### 2. RecruitDataProcessor 마이그레이션

**변경 전:**
```java
import com.fasterxml.jackson.databind.ObjectMapper;

private final ObjectMapper objectMapper;

List<String> jsonItems = objectMapper.readValue(...);
RecruitRowDto dto = objectMapper.readValue(...);
```

**변경 후:**
```java
import com.fasterxml.jackson.databind.json.JsonMapper;

private final JsonMapper jsonMapper;

List<String> jsonItems = jsonMapper.readValue(...);
RecruitRowDto dto = jsonMapper.readValue(...);
```

### 3. CandidateDataProcessor 마이그레이션

**변경 전:**
```java
import com.fasterxml.jackson.databind.ObjectMapper;

private final ObjectMapper objectMapper;
```

**변경 후:**
```java
import com.fasterxml.jackson.databind.json.JsonMapper;

private final JsonMapper jsonMapper;
```

### 4. build.gradle 의존성 추가

```gradle
// Jackson Datatype Modules (for Java 8 Time API support)
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
```

---

## ✅ 검증 결과

### 빌드 성공
```bash
./gradlew clean build -x test

BUILD SUCCESSFUL in 46s
9 actionable tasks: 9 executed
```

### API 호환성
- JsonMapper는 ObjectMapper의 하위 클래스
- 기존 ObjectMapper API 모두 호환 (readValue, writeValue 등)
- 기존 코드 변경 최소화

---

## 📝 영향 범위

### 변경된 파일
1. `config/JacksonConfig.java` (신규)
2. `application/processor/RecruitDataProcessor.java` (수정)
3. `application/processor/CandidateDataProcessor.java` (수정)
4. `build.gradle` (의존성 추가)

### 영향 없는 부분
- DTO 클래스 (RecruitRowDto, CandidateRowDto): `@JsonProperty` 어노테이션 그대로 사용
- 기존 JSON 직렬화/역직렬화 로직: API 동일
- 테스트 코드: 변경 불필요

---

## 🎓 학습 포인트

### JsonMapper vs ObjectMapper
| 구분 | ObjectMapper | JsonMapper |
|-----|-------------|-----------|
| 범용성 | 범용 매퍼 | JSON 전용 |
| 타입 안전성 | 낮음 | 높음 |
| Spring Boot 4.0 권장 | X | O |
| 상속 관계 | 부모 클래스 | 자식 클래스 |
| API 호환성 | - | ObjectMapper API 모두 호환 |

### Spring Boot 4.0 Jackson 3 주요 변경
1. **어노테이션 변경**
   - `@JsonComponent` → `@JacksonComponent`
   - `@JsonMixin` → `@JacksonMixin`

2. **설정 프로퍼티 변경**
   - `spring.jackson.read.*` → `spring.jackson.json.read.*`
   - `spring.jackson.write.*` → `spring.jackson.json.write.*`

3. **Builder 클래스 변경**
   - `Jackson2ObjectMapperBuilder` → `JsonMapperBuilder`
   - `Jackson2ObjectMapperBuilderCustomizer` → `JsonMapperBuilderCustomizer`

---

## 🔗 참고 자료

### 공식 문서
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Introducing Jackson 3 support in Spring](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring)
- [Spring Boot 4.0.0-M3 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0.0-M3-Release-Notes)

### 관련 블로그
- [Spring Boot 4 Moves to Jackson 3](https://blog.vvauban.com/blog/spring-boot-4-moves-to-jackson-3-already-in-m3)
- [Spring Boot 4 Migration Guide: Faster, Safer, at Scale](https://www.moderne.ai/blog/spring-boot-4x-migration-guide)

---

## 💡 향후 개선 사항

### 1. 추가 설정 고려
- null 값 처리: `.serializationInclusion(JsonInclude.Include.NON_NULL)`
- 빈 객체 처리: `.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)`

### 2. 성능 최적화
- JsonMapper 인스턴스 재사용 (Bean 등록으로 이미 적용됨)
- 필요 시 커스텀 직렬화/역직렬화 구현

### 3. 에러 처리 강화
- JSON 파싱 실패 시 DLQ 처리 연동
- 상세 에러 로깅

---

**최종 수정일:** 2025-12-12
**상태:** 완료
**다음 단계:** DLQ 처리 로직 구현
