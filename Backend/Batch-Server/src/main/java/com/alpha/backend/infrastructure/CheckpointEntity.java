package com.alpha.backend.infrastructure;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Batch Checkpoint Entity
 * 배치 처리의 체크포인트를 저장하는 엔티티
 */
@Entity
@Table(name = "embedding_batch_checkpoint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckpointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "last_processed_uuid", columnDefinition = "UUID")
    private UUID lastProcessedUuid;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
