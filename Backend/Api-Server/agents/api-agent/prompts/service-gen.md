# Service Generation

**Pattern**: Reactive + L1/L2 Caching

## Template

```java
package com.alpha.api.domain.{{domain}}.service;

import reactor.core.publisher.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class {{Service}} {
    private final {{Entity}}Repository repository;
    private final CacheService cacheService;

    private static final String CACHE_PREFIX = "{{domain}}:";
    private static final int CACHE_TTL = 600;

    public Mono<{{Entity}}> findById(UUID id) {
        String key = CACHE_PREFIX + id;
        return cacheService.get(key, {{Entity}}.class)
            .switchIfEmpty(
                repository.findById(id)
                    .flatMap(e -> cacheService.set(key, e, CACHE_TTL).thenReturn(e))
            );
    }

    public Flux<{{Entity}}> findAll(int limit, int offset) {
        return repository.findAll(limit, offset);
    }

    public Mono<{{Entity}}> save({{Entity}} entity) {
        return repository.save(entity)
            .flatMap(saved -> {
                String key = CACHE_PREFIX + saved.getId();
                return cacheService.set(key, saved, CACHE_TTL).thenReturn(saved);
            });
    }

    public Mono<Void> deleteById(UUID id) {
        String key = CACHE_PREFIX + id;
        return repository.deleteById(id).then(cacheService.evict(key));
    }
}
```

## Similarity Search (if embedding)

```java
public Flux<SearchResult> searchBySimilarity(
    List<String> skills, int limit, float threshold
) {
    return skillNormalizationService.normalizeSkills(skills)
        .flatMapMany(vector ->
            repository.searchBySimilarity(vector, limit, threshold)
        );
}
```

## Rules

- All methods return Mono/Flux
- NO .block()
- Cache key: `{{domain}}:{id}`
- Use switchIfEmpty for defaults
- Mono.zip() for multiple sources
