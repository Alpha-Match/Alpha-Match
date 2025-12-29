# Reactive 프로그래밍 가이드

**작성일**: 2025-12-23
**대상**: Api-Server
**목적**: Spring WebFlux + R2DBC Reactive 패턴 및 구현 가이드

---

## 📋 개요

Api-Server는 **Spring WebFlux + R2DBC**를 사용하여 완전한 Non-blocking Reactive Stack을 구현합니다. 높은 동시성과 적은 리소스로 많은 요청을 처리할 수 있습니다.

### Reactive Stack

```
GraphQL Query
    ↓
Resolver (Controller) - Mono/Flux
    ↓
Service Layer - Mono/Flux
    ↓
Repository (R2DBC) - Mono/Flux
    ↓
PostgreSQL (Non-blocking I/O)
```

---

## 🎯 Reactive 핵심 개념

### 1. Mono vs Flux

| 타입 | 데이터 개수 | 사용 예시 |
|------|-----------|----------|
| **Mono** | 0 or 1 | findById, save, delete |
| **Flux** | 0 to N | findAll, search, stream |

### 2. Reactive 연산자

```java
// Map: 변환
Mono<String> name = userMono.map(User::getName);

// FlatMap: 비동기 변환
Mono<Order> order = userMono.flatMap(user -> orderService.findByUserId(user.getId()));

// Filter: 필터링
Flux<User> adults = userFlux.filter(user -> user.getAge() >= 18);

// SwitchIfEmpty: 기본값 처리
Mono<User> user = userRepository.findById(id)
    .switchIfEmpty(Mono.error(new NotFoundException()));

// Zip: 여러 Mono 결합
Mono<UserProfile> profile = Mono.zip(
    userMono,
    addressMono,
    (user, address) -> new UserProfile(user, address)
);

// FlatMapMany: Mono → Flux 변환
Flux<Order> orders = userMono.flatMapMany(user -> orderRepository.findByUserId(user.getId()));
```

---

## 🏗️ R2DBC 설정

### 1. R2dbcConfig

```java
@Configuration
@EnableR2dbcRepositories(basePackages = "com.alpha.api.infrastructure.persistence")
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    @Value("${spring.r2dbc.url}")
    private String url;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Override
    public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get(
                ConnectionFactoryOptions.builder()
                        .option(DRIVER, "postgresql")
                        .option(HOST, "localhost")
                        .option(PORT, 5432)
                        .option(USER, username)
                        .option(PASSWORD, password)
                        .option(DATABASE, "alpha_match")
                        .build()
        );
    }

    /**
     * PGvector 타입 변환을 위한 Custom Converter
     */
    @Override
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = List.of(
                new PGvectorReadConverter(),
                new PGvectorWriteConverter()
        );
        return new R2dbcCustomConversions(
                getStoreConversions(),
                converters
        );
    }
}
```

### 2. PGvector Custom Converter

```java
/**
 * PostgreSQL vector → float[]
 */
@ReadingConverter
public class PGvectorReadConverter implements Converter<String, float[]> {

    @Override
    public float[] convert(String source) {
        if (source == null || source.isEmpty()) {
            return new float[0];
        }

        // "[0.1, 0.2, 0.3]" → float[]
        String[] tokens = source
                .replace("[", "")
                .replace("]", "")
                .split(",");

        float[] result = new float[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = Float.parseFloat(tokens[i].trim());
        }
        return result;
    }
}

/**
 * float[] → PostgreSQL vector
 */
@WritingConverter
public class PGvectorWriteConverter implements Converter<float[], String> {

    @Override
    public String convert(float[] source) {
        if (source == null || source.length == 0) {
            return "[]";
        }

        return Arrays.toString(source);
    }
}
```

---

## 📦 Repository 구현 패턴

### 1. Interface (Port)

```java
package com.alpha.api.domain.recruit.repository;

import com.alpha.api.domain.recruit.entity.Recruit;
import com.alpha.api.graphql.type.RecruitSearchResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RecruitRepository {

    Mono<Recruit> findById(UUID id);

    Flux<Recruit> findAll(int limit, int offset);

    Flux<RecruitSearchResult> searchBySimilarity(
            float[] queryVector,
            Integer experienceYears,
            String englishLevel,
            int limit,
            float similarityThreshold
    );

    Mono<Recruit> save(Recruit recruit);

    Mono<Void> deleteById(UUID id);
}
```

### 2. R2DBC Adapter 구현

```java
@Repository
@RequiredArgsConstructor
public class RecruitR2dbcRepository implements RecruitRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Recruit> findById(UUID id) {
        String sql = """
            SELECT recruit_id, position, company_name, experience_years,
                   primary_keyword, english_level, published_at,
                   created_at, updated_at
            FROM recruit
            WHERE recruit_id = :id
            """;

        return databaseClient.sql(sql)
                .bind("id", id)
                .map(this::mapToRecruit)
                .one();
    }

    @Override
    public Flux<Recruit> findAll(int limit, int offset) {
        String sql = """
            SELECT recruit_id, position, company_name, experience_years,
                   primary_keyword, english_level, published_at,
                   created_at, updated_at
            FROM recruit
            ORDER BY published_at DESC
            LIMIT :limit OFFSET :offset
            """;

        return databaseClient.sql(sql)
                .bind("limit", limit)
                .bind("offset", offset)
                .map(this::mapToRecruit)
                .all();
    }

    @Override
    public Flux<RecruitSearchResult> searchBySimilarity(
            float[] queryVector,
            Integer experienceYears,
            String englishLevel,
            int limit,
            float similarityThreshold
    ) {
        String vectorStr = Arrays.toString(queryVector);

        String sql = """
            SELECT r.recruit_id, r.position, r.company_name, r.experience_years,
                   r.primary_keyword, r.english_level, r.published_at,
                   r.created_at, r.updated_at,
                   1 - (re.skills_vector <=> CAST(:queryVector AS vector)) AS similarity
            FROM recruit r
            INNER JOIN recruit_skills_embedding re ON r.recruit_id = re.recruit_id
            WHERE 1 - (re.skills_vector <=> CAST(:queryVector AS vector)) >= :threshold
            """ +
            (experienceYears != null ? " AND r.experience_years <= :experienceYears" : "") +
            (englishLevel != null ? " AND r.english_level = :englishLevel" : "") +
            """
            ORDER BY similarity DESC
            LIMIT :limit
            """;

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql)
                .bind("queryVector", vectorStr)
                .bind("threshold", similarityThreshold)
                .bind("limit", limit);

        if (experienceYears != null) {
            spec = spec.bind("experienceYears", experienceYears);
        }
        if (englishLevel != null) {
            spec = spec.bind("englishLevel", englishLevel);
        }

        return spec.map((row, metadata) -> {
                    Recruit recruit = mapToRecruit(row);
                    Float similarity = row.get("similarity", Float.class);
                    return new RecruitSearchResult(recruit, similarity);
                })
                .all();
    }

    private Recruit mapToRecruit(Row row) {
        Recruit recruit = new Recruit();
        recruit.setRecruitId(row.get("recruit_id", UUID.class));
        recruit.setPosition(row.get("position", String.class));
        recruit.setCompanyName(row.get("company_name", String.class));
        recruit.setExperienceYears(row.get("experience_years", Integer.class));
        recruit.setPrimaryKeyword(row.get("primary_keyword", String.class));
        recruit.setEnglishLevel(row.get("english_level", String.class));
        recruit.setPublishedAt(row.get("published_at", java.time.OffsetDateTime.class));
        recruit.setCreatedAt(row.get("created_at", java.time.OffsetDateTime.class));
        recruit.setUpdatedAt(row.get("updated_at", java.time.OffsetDateTime.class));
        return recruit;
    }
}
```

---

## 🔄 Service Layer 패턴

### RecruitService (Reactive)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RecruitService {

    private final RecruitRepository recruitRepository;
    private final RecruitDescriptionRepository recruitDescriptionRepository;
    private final RecruitSkillRepository recruitSkillRepository;
    private final CacheService cacheService;

    /**
     * ID로 조회 (캐시 우선)
     */
    public Mono<Recruit> findById(UUID id) {
        String cacheKey = "recruit:" + id;

        return cacheService.get(cacheKey, Recruit.class)
                .switchIfEmpty(
                        recruitRepository.findById(id)
                                .flatMap(recruit ->
                                        // 연관 데이터 로딩
                                        enrichRecruitData(recruit)
                                                .flatMap(enriched ->
                                                        cacheService.set(cacheKey, enriched, 600)
                                                                .thenReturn(enriched)
                                                )
                                )
                );
    }

    /**
     * 연관 데이터 로딩 (Description + Skills)
     */
    private Mono<Recruit> enrichRecruitData(Recruit recruit) {
        return Mono.zip(
                recruitDescriptionRepository.findById(recruit.getRecruitId())
                        .defaultIfEmpty(new RecruitDescription()),
                recruitSkillRepository.findAllByRecruitId(recruit.getRecruitId())
                        .map(RecruitSkill::getSkill)
                        .collectList()
        ).map(tuple -> {
            recruit.setDescription(tuple.getT1());
            recruit.setSkills(tuple.getT2());
            return recruit;
        });
    }

    /**
     * 유사도 검색 (스킬 기반)
     */
    public Flux<RecruitSearchResult> searchBySimilarity(
            List<String> skills,
            Integer experienceYears,
            String englishLevel,
            int limit,
            float similarityThreshold
    ) {
        return skillNormalizationService.normalizeSkills(skills)
                .flatMapMany(queryVector ->
                        recruitRepository.searchBySimilarity(
                                queryVector,
                                experienceYears,
                                englishLevel,
                                limit,
                                similarityThreshold
                        )
                )
                .flatMap(result ->
                        enrichRecruitData(result.getRecruit())
                                .map(enriched -> new RecruitSearchResult(enriched, result.getSimilarity()))
                );
    }

    /**
     * 전체 조회 (페이징)
     */
    public Flux<Recruit> findAll(int limit, int offset) {
        return recruitRepository.findAll(limit, offset)
                .flatMap(this::enrichRecruitData);
    }
}
```

---

## ⚠️ Reactive 주의사항

### 1. ❌ Blocking 코드 금지

```java
// ❌ Bad: .block() 사용
public Recruit findById(UUID id) {
    return recruitRepository.findById(id).block(); // Blocking!
}

// ✅ Good: Mono 반환
public Mono<Recruit> findById(UUID id) {
    return recruitRepository.findById(id);
}
```

### 2. ❌ 동기 API 호출 금지

```java
// ❌ Bad: RestTemplate (Blocking)
public Mono<User> getUser(String id) {
    User user = restTemplate.getForObject("/users/" + id, User.class);
    return Mono.just(user);
}

// ✅ Good: WebClient (Non-blocking)
public Mono<User> getUser(String id) {
    return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class);
}
```

### 3. ❌ Thread.sleep() 금지

```java
// ❌ Bad: Thread.sleep (Blocking)
public Mono<String> delayedResult() {
    Thread.sleep(1000);
    return Mono.just("result");
}

// ✅ Good: Mono.delay (Non-blocking)
public Mono<String> delayedResult() {
    return Mono.delay(Duration.ofSeconds(1))
            .thenReturn("result");
}
```

---

## 🧪 Reactive 테스트

### RecruitServiceTest

```java
@ExtendWith(MockitoExtension.class)
class RecruitServiceTest {

    @Mock
    private RecruitRepository recruitRepository;

    @InjectMocks
    private RecruitService recruitService;

    @Test
    void findById_shouldReturnRecruit() {
        // Given
        UUID id = UUID.randomUUID();
        Recruit recruit = new Recruit();
        recruit.setRecruitId(id);

        when(recruitRepository.findById(id))
                .thenReturn(Mono.just(recruit));

        // When
        Mono<Recruit> result = recruitService.findById(id);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(r -> r.getRecruitId().equals(id))
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(recruitRepository.findById(id))
                .thenReturn(Mono.empty());

        // When
        Mono<Recruit> result = recruitService.findById(id);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void findAll_shouldReturnFlux() {
        // Given
        Recruit recruit1 = new Recruit();
        Recruit recruit2 = new Recruit();

        when(recruitRepository.findAll(10, 0))
                .thenReturn(Flux.just(recruit1, recruit2));

        // When
        Flux<Recruit> result = recruitService.findAll(10, 0);

        // Then
        StepVerifier.create(result)
                .expectNext(recruit1)
                .expectNext(recruit2)
                .verifyComplete();
    }
}
```

---

## 📊 성능 최적화

### 1. 병렬 처리 (Parallel Flux)

```java
public Flux<RecruitSearchResult> searchWithParallel(List<String> skills) {
    return Flux.fromIterable(skills)
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap(skill -> skillRepository.findBySkill(skill))
            .sequential();
}
```

### 2. Timeout 설정

```java
public Mono<Recruit> findByIdWithTimeout(UUID id) {
    return recruitRepository.findById(id)
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(TimeoutException.class, e -> {
                log.error("Timeout finding recruit: {}", id);
                return Mono.empty();
            });
}
```

### 3. Retry 전략

```java
public Mono<Recruit> findByIdWithRetry(UUID id) {
    return recruitRepository.findById(id)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(throwable -> throwable instanceof R2dbcException)
            );
}
```

---

## ⚙️ 설정 (application.yml)

```yaml
spring:
  # R2DBC 설정
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/alpha_match
    username: postgres
    password: postgres
    pool:
      initial-size: 10
      max-size: 20
      max-idle-time: 30m
      validation-query: SELECT 1

  # WebFlux 설정
  webflux:
    base-path: /api

# Reactor 설정
reactor:
  bufferSize:
    small: 256
    default: 8192
```

---

## 📚 참고 문서

- **GraphQL API 개발**: `GraphQL_API_개발_가이드.md`
- **캐싱 전략**: `캐싱_전략_가이드.md`
- **Spring Batch 개발**: `/Backend/Batch-Server/docs/Spring_Batch_개발_가이드.md`
- **Project Reactor 공식 문서**: https://projectreactor.io/docs

---

**최종 수정일**: 2025-12-23
