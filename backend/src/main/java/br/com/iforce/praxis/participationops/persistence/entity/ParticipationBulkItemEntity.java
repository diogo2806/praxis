package br.com.iforce.praxis.participationops.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "participation_bulk_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participation_bulk_item",
                columnNames = {"job_id", "participation_type", "participation_id"}
        )
)
public class ParticipationBulkItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "participation_type", nullable = false, length = 20)
    private String participationType;

    @Column(name = "participation_id", nullable = false, length = 120)
    private String participationId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "processed_at")
    private Instant processedAt;
}
