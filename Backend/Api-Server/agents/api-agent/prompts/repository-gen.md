# Repository Generation

**Pattern**: Port (domain) + Adapter (infrastructure)

## Port Interface

```java
package com.alpha.api.domain.{{domain}}.repository;

import reactor.core.publisher.*;
import java.util.UUID;

public interface {{Entity}}Repository {
    Mono<{{Entity}}> findById(UUID id);
    Flux<{{Entity}}> findAll(int limit, int offset);
    Mono<{{Entity}}> save({{Entity}} entity);
    Mono<Void> deleteById(UUID id);
}
```

## R2DBC Adapter

```java
package com.alpha.api.infrastructure.persistence;

import org.springframework.r2dbc.core.DatabaseClient;

@Repository
@RequiredArgsConstructor
public class {{Entity}}R2dbcRepository implements {{Entity}}Repository {
    private final DatabaseClient databaseClient;

    @Override
    public Mono<{{Entity}}> findById(UUID id) {
        return databaseClient.sql("SELECT * FROM {{table}} WHERE {{id_col}} = :id")
            .bind("id", id)
            .map(this::mapRow)
            .one();
    }

    // Implement other methods

    private {{Entity}} mapRow(Row row) {
        return {{Entity}}.builder()
            .id(row.get("{{id_col}}", UUID.class))
            // Map columns
            .build();
    }
}
```

## Similarity Search (if embedding table)

```java
Flux<SearchResult> searchBySimilarity(float[] vector, int limit, float threshold) {
    String sql = """
        SELECT *, 1 - (skills_vector <=> CAST(:vec AS vector)) AS similarity
        FROM {{table}}
        WHERE 1 - (skills_vector <=> CAST(:vec AS vector)) >= :threshold
        ORDER BY similarity DESC LIMIT :limit
        """;

    return databaseClient.sql(sql)
        .bind("vec", Arrays.toString(vector))
        .bind("threshold", threshold)
        .bind("limit", limit)
        .map(row -> new SearchResult(mapRow(row), row.get("similarity", Float.class)))
        .all();
}
```

## Rules

- Port in domain/{{domain}}/repository/
- Adapter in infrastructure/persistence/
- All methods return Mono/Flux
- pgvector: CAST(:vector AS vector)
- Similarity: <-> (L2) or <=> (cosine)
