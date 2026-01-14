# GraphQL Resolver Generation

## Schema

```graphql
type {{Entity}} {
  id: ID!
  # fields from table_specification.md
}

type {{Entity}}SearchResult {
  {{entity}}: {{Entity}}!
  similarity: Float!
}

input {{Entity}}SearchInput {
  skills: [String!]!
  limit: Int = 10
  similarityThreshold: Float = 0.7
}

type Query {
  {{entity}}(id: ID!): {{Entity}}
  {{entity}}s(limit: Int, offset: Int): [{{Entity}}!]!
  search{{Entity}}s(input: {{Entity}}SearchInput!): [{{Entity}}SearchResult!]!
}
```

## QueryResolver

```java
package com.alpha.api.graphql.resolver;

import org.springframework.graphql.data.method.annotation.*;
import reactor.core.publisher.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class QueryResolver {
    private final {{Service}} service;

    @QueryMapping
    public Mono<{{Entity}}> {{entity}}(@Argument String id) {
        return service.findById(UUID.fromString(id));
    }

    @QueryMapping
    public Flux<{{Entity}}> {{entity}}s(@Argument Integer limit, @Argument Integer offset) {
        return service.findAll(limit, offset);
    }

    @QueryMapping
    public Flux<SearchResult> search{{Entity}}s(@Argument SearchInput input) {
        return service.searchBySimilarity(input.getSkills(), input.getLimit(), input.getThreshold());
    }
}
```

## Input Type

```java
@Data
public class {{Entity}}SearchInput {
    private List<String> skills;
    private Integer limit = 10;
    private Float similarityThreshold = 0.7f;
}
```

## FieldResolver (Lazy Loading)

```java
@Controller
public class {{Entity}}FieldResolver {
    @SchemaMapping(typeName = "{{Entity}}", field = "description")
    public Mono<Description> description({{Entity}} entity) {
        return descriptionRepo.findById(entity.getId());
    }
}
```

## Rules

- Schema first
- All resolvers return Mono/Flux
- @QueryMapping for queries
- @MutationMapping for mutations
- @SchemaMapping for lazy fields
