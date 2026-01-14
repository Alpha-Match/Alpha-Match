# GraphQL API 개발 가이드

**작성일**: 2025-12-23
**대상**: Api-Server
**목적**: Spring for GraphQL 기반 API 설계 및 구현 패턴

---

## 📋 개요

Api-Server는 **Spring for GraphQL + Spring WebFlux**를 사용하여 Reactive GraphQL API를 제공합니다. Frontend에서 유연한 쿼리를 통해 필요한 데이터만 요청할 수 있습니다.

### 핵심 아키텍처

```
Frontend (GraphQL Query)
    ↓
QueryResolver (Controller)
    ↓
Service Layer (Mono/Flux)
    ↓
Cache Check (L1 → L2)
    ├─ Hit: Return Cached Data
    └─ Miss: ↓
         R2DBC Repository
         ↓
         PostgreSQL (pgvector)
```

---

## 🎯 GraphQL Schema 설계

### 1. Schema 정의 (schema.graphqls)

**위치**: `src/main/resources/graphql/schema.graphqls`

```graphql
# ===== Recruit Domain =====

type Recruit {
  id: ID!
  position: String!
  companyName: String!
  experienceYears: Int
  primaryKeyword: String
  englishLevel: String
  publishedAt: String!
  description: RecruitDescription
  skills: [String!]
  createdAt: String!
  updatedAt: String!
}

type RecruitDescription {
  recruitId: ID!
  longDescription: String!
  descriptionLang: String
}

type RecruitSearchResult {
  recruit: Recruit!
  similarity: Float!
}

# ===== Candidate Domain =====

type Candidate {
  id: ID!
  positionCategory: String!
  experienceYears: Int
  originalResume: String!
  description: CandidateDescription
  skills: [String!]
  createdAt: String!
  updatedAt: String!
}

type CandidateDescription {
  candidateId: ID!
  originalResume: String!
  resumeLang: String
}

type CandidateSearchResult {
  candidate: Candidate!
  similarity: Float!
}

# ===== Input Types =====

input RecruitSearchInput {
  skills: [String!]!
  experienceYears: Int
  englishLevel: String
  limit: Int = 10
  similarityThreshold: Float = 0.7
}

input CandidateSearchInput {
  skills: [String!]!
  experienceYears: Int
  positionCategory: String
  limit: Int = 10
  similarityThreshold: Float = 0.7
}

# ===== Queries =====

type Query {
  # Recruit 조회
  recruit(id: ID!): Recruit
  recruits(limit: Int = 10, offset: Int = 0): [Recruit!]!
  searchRecruits(input: RecruitSearchInput!): [RecruitSearchResult!]!

  # Candidate 조회
  candidate(id: ID!): Candidate
  candidates(limit: Int = 10, offset: Int = 0): [Candidate!]!
  searchCandidates(input: CandidateSearchInput!): [CandidateSearchResult!]!
}

# ===== Mutations =====

type Mutation {
  invalidateCache(target: String!): Boolean!
}
```

---

## 🔧 Resolver 구현 패턴

### QueryResolver

```java
@Controller
@RequiredArgsConstructor
@Slf4j
public class QueryResolver {

    private final RecruitService recruitService;

    @QueryMapping
    public Mono<Recruit> recruit(@Argument String id) {
        log.info("GraphQL Query: recruit(id={})", id);
        return recruitService.findById(UUID.fromString(id));
    }

    @QueryMapping
    public Flux<Recruit> recruits(
            @Argument Integer limit,
            @Argument Integer offset
    ) {
        return recruitService.findAll(limit, offset);
    }

    @QueryMapping
    public Flux<RecruitSearchResult> searchRecruits(
            @Argument RecruitSearchInput input
    ) {
        return recruitService.searchBySimilarity(
                input.getSkills(),
                input.getExperienceYears(),
                input.getEnglishLevel(),
                input.getLimit(),
                input.getSimilarityThreshold()
        );
    }
}
```

---

## 📦 Service Layer 패턴

### RecruitService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RecruitService {

    private final RecruitRepository recruitRepository;
    private final SkillNormalizationService skillNormalizationService;
    private final CacheService cacheService;

    public Mono<Recruit> findById(UUID id) {
        String cacheKey = "recruit:" + id;

        return cacheService.get(cacheKey, Recruit.class)
                .switchIfEmpty(
                        recruitRepository.findById(id)
                                .flatMap(recruit ->
                                        cacheService.set(cacheKey, recruit, 600)
                                                .thenReturn(recruit)
                                )
                );
    }

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
                );
    }
}
```

---

## 📡 Repository 패턴 (R2DBC)

### RecruitR2dbcRepository

```java
@Repository
@RequiredArgsConstructor
public class RecruitR2dbcRepository implements RecruitRepository {

    private final DatabaseClient databaseClient;

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
            SELECT r.*,
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
}
```

---

## 🧪 GraphQL 테스트

### GraphiQL에서 테스트

**URL**: http://localhost:8080/graphiql

#### Query 예시: 스킬 기반 검색

```graphql
query {
  searchRecruits(
    input: {
      skills: ["Java", "Spring Boot", "PostgreSQL"]
      experienceYears: 5
      limit: 10
      similarityThreshold: 0.7
    }
  ) {
    similarity
    recruit {
      id
      position
      companyName
      skills
    }
  }
}
```

---

## 📊 성능 최적화

### 1. @SchemaMapping으로 Lazy Loading

```java
@Controller
public class RecruitFieldResolver {

    @SchemaMapping(typeName = "Recruit", field = "description")
    public Mono<RecruitDescription> description(Recruit recruit) {
        return recruitDescriptionRepository.findById(recruit.getRecruitId());
    }

    @SchemaMapping(typeName = "Recruit", field = "skills")
    public Flux<String> skills(Recruit recruit) {
        return recruitSkillRepository.findAllByRecruitId(recruit.getRecruitId())
                .map(RecruitSkill::getSkill);
    }
}
```

### 2. DataLoader 패턴 (N+1 문제 해결)

```java
@Component
public class RecruitDataLoader implements BatchLoaderWithContext<UUID, Recruit> {

    private final RecruitRepository recruitRepository;

    @Override
    public Mono<Map<UUID, Recruit>> load(List<UUID> keys, BatchLoaderEnvironment environment) {
        return recruitRepository.findAllByIds(keys)
                .collectMap(Recruit::getRecruitId);
    }
}
```

---

## 📚 참고 문서

- **캐싱 전략**: `캐싱_전략_가이드.md`
- **Reactive 프로그래밍**: `Reactive_프로그래밍_가이드.md`
- **테이블 명세서**: `/Backend/docs/table_specification.md`

---

**최종 수정일**: 2025-12-23
