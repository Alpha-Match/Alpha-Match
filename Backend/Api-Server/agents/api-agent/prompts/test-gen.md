# Test Generation

## Service Test

```java
@ExtendWith(MockitoExtension.class)
class {{Service}}Test {
    @Mock private {{Entity}}Repository repository;
    @Mock private CacheService cacheService;
    @InjectMocks private {{Service}} service;

    @Test
    void findById_shouldReturnEntity() {
        UUID id = UUID.randomUUID();
        {{Entity}} entity = {{Entity}}.builder().id(id).build();

        when(cacheService.get(any(), eq({{Entity}}.class))).thenReturn(Mono.empty());
        when(repository.findById(id)).thenReturn(Mono.just(entity));
        when(cacheService.set(any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(id))
            .expectNext(entity)
            .verifyComplete();
    }
}
```

## Repository Test (TestContainers)

```java
@DataR2dbcTest
@Testcontainers
class {{Entity}}R2dbcRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired private {{Entity}}R2dbcRepository repository;

    @Test
    void save_and_findById_shouldWork() {
        UUID id = UUID.randomUUID();
        {{Entity}} entity = {{Entity}}.builder().id(id).build();

        StepVerifier.create(repository.save(entity))
            .expectNextMatches(saved -> saved.getId().equals(id))
            .verifyComplete();
    }
}
```

## GraphQL Test

```java
@GraphQlTest
class {{Entity}}GraphQLTest {
    @Autowired private GraphQlTester tester;
    @MockBean private {{Service}} service;

    @Test
    void query_{{entity}}_shouldReturn() {
        UUID id = UUID.randomUUID();
        when(service.findById(any())).thenReturn(Mono.just(entity));

        tester.documentName("{{entity}}")
            .variable("id", id.toString())
            .execute()
            .path("{{entity}}.id").entity(String.class).isEqualTo(id.toString());
    }
}
```

## StepVerifier Methods

- `.expectNext(value)` - exact match
- `.expectNextMatches(predicate)` - condition
- `.expectNextCount(n)` - count
- `.expectError(Exception.class)` - error
- `.verifyComplete()` - complete signal

## Rules

- Use StepVerifier for Reactive
- Mock with Mockito
- TestContainers for integration
- GraphQlTester for GraphQL
