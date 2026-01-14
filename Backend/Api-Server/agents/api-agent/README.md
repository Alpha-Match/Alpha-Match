# api-agent

**Version**: 1.0.0 (Token-Optimized)
**Purpose**: AI agent for Api-Server development
**Pattern**: Schema-Driven + Clean Architecture

---

## Usage

### Create Domain
```
"Create complete domain for recruit"
→ Entity + Repository + Service + GraphQL + Tests
```

### Add Query
```
"Add similarity search query"
→ Service + Schema + Resolver + Test
```

### Add Mutation
```
"Add create recruit mutation"
→ Input + Resolver + Test
```

---

## Structure

```
agents/api-agent/
├── .agent-config.json
├── prompts/          # 5 templates (~400 tokens each)
│   ├── entity-gen.md
│   ├── repository-gen.md
│   ├── service-gen.md
│   ├── resolver-gen.md
│   └── test-gen.md
└── workflows/        # 3 workflows (~300 tokens each)
    ├── create-domain.yaml
    ├── add-query.yaml
    └── add-mutation.yaml
```

---

## Capabilities

✅ Schema-driven (table_specification.md)
✅ Clean Architecture (Port-Adapter)
✅ Reactive patterns (Mono/Flux)
✅ Multi-layer caching (Caffeine + Redis)
✅ pgvector similarity search
✅ **Token-optimized for AI agents**

---

## Documentation Priority

1. `/Backend/docs/table_specification.md` (MUST)
2. `/Backend/Api-Server/docs/*.md`
3. `/Backend/Batch-Server/domain/` (reference)

---

## Constraints

- Read-only mode (planning only)
- table_specification.md required
- Reactive patterns enforced
- Vector dimension: 384
- PostgreSQL: 15

---

**Last updated**: 2025-12-23 (Token-optimized)
