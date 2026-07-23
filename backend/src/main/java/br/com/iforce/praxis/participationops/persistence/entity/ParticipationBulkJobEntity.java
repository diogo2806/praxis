package br.com.iforce.praxis.participationops.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "participation_bulk_jobs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participation_bulk_job_idempotency",
                columnNames = {"empresa_id", "idempotency_key"}
        )
)
public class ParticipationBulkJobEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "selection_mode", nullable = false, length = 20)
    private String selectionMode;

    @Column(name = "filter_json", columnDefinition = "TEXT")
    private String filterJson;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "justification", length = 1000)
    private String justification;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "total_items", nullable = false)
    private int totalItems;

    @Column(name = "processed_items", nullable = false)
    private int processedItems;

    @Column(name = "succeeded_items", nullable = false)
    private int succeededItems;

    @Column(name = "skipped_items", nullable = false)
    private int skippedItems;

    @Column(name = "failed_items", nullable = false)
    private int failedItems;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
