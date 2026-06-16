package br.com.iforce.praxis.gupy.delivery.persistence.entity;

import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "result_deliveries",
        uniqueConstraints = @UniqueConstraint(name = "uk_result_delivery_attempt", columnNames = "candidate_attempt_id")
)
public class ResultDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_attempt_id", nullable = false)
    private CandidateAttemptEntity candidateAttempt;

    @Column(name = "webhook_url", nullable = false, length = 1000)
    private String webhookUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ResultDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "last_error", length = 1200)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
