# Entity Generation

**Source**: `/Backend/docs/table_specification.md` (필수)

## Template

```java
package com.alpha.api.domain.{{domain}}.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("{{table_name}}")
public class {{EntityName}} {
    @Id
    @Column("{{id_column}}")
    private UUID {{idField}};

    // Business fields from table_specification.md

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
```

## Type Mapping

| PostgreSQL | Java | Note |
|------------|------|------|
| UUID | UUID | PK |
| TEXT/VARCHAR | String | |
| INTEGER | Integer | nullable |
| BIGINT | Long | |
| TIMESTAMPTZ | OffsetDateTime | |
| VECTOR(n) | float[] | custom converter |
| TEXT[] | String[] | |

## Rules

- Read table_specification.md FIRST
- Map all columns exactly
- PGvector → float[]
- Include created_at, updated_at
- Use @Builder for construction

## Reference

`/Backend/Batch-Server/domain/{{domain}}/entity/` for patterns
