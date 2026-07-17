package br.com.iforce.praxis.gupy.persistence.entity;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.ReliabilityLevel;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidate_attempts")
public class CandidateAttemptEntity implements EmpresaAwareEntity {

    private static final long DEFAULT_LINK_TTL_HOURS = 168L;

    @Id
    @Column(name = "id", nullable = false, length = 80)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "company_id", nullable = false, length = 120)
    private String companyId;

    @Column(name = "result_id", nullable = false, unique = true, length = 80)
    private String resultId;

    @Column(name = "simulation_id", nullable = false, length = 120)
    private String simulationId;

    @Column(name = "simulation_version_id")
    private Long simulationVersionId;

    @Column(name = "simulation_version_number")
    private Integer simulationVersionNumber;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 300)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Column(name = "request_fingerprint_version")
    private Integer requestFingerprintVersion;

    @Column(name = "candidate_name", nullable = false, length = 160)
    private String candidateName;

    @Column(name = "candidate_email", nullable = false, length = 180)
    private String candidateEmail;

    @Column(name = "gupy_job_id")
    private Long gupyJobId;

    @Column(name = "callback_url", length = 1000)
    private String callbackUrl;

    @Column(name = "result_webhook_url", length = 1000)
    private String resultWebhookUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AttemptStatus status;

    @Column(name = "score")
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 60)
    private ResultDecision decision;

    @Column(name = "human_review_required", nullable = false)
    private boolean humanReviewRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "reliability_level", nullable = false, length = 40)
    private ReliabilityLevel reliabilityLevel;

    @Column(name = "accommodation_time_multiplier", nullable = false, precision = 3, scale = 2)
    private BigDecimal accommodationTimeMultiplier = BigDecimal.ONE;

    @Column(name = "company_result_string", nullable = false, length = 4000)
    private String companyResultString;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "candidate_token_issued_at", nullable = false)
    private Instant candidateTokenIssuedAt;

    @Column(name = "candidate_token_expires_at", nullable = false)
    private Instant candidateTokenExpiresAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "anonymized_at")
    private Instant anonymizedAt;

    @OneToMany(mappedBy = "candidateAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AttemptAnswerEntity> answers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidateAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AttemptNodeServeEntity> nodeServes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidateAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResultItemEntity> resultItems = new LinkedHashSet<>();

    @PrePersist
    void initializeCandidateTokenWindow() {
        if (createdAt == null) {
            throw new IllegalStateException("A data de criação da tentativa é obrigatória.");
        }
        if (candidateTokenIssuedAt == null) {
            candidateTokenIssuedAt = createdAt;
        }
        if (candidateTokenExpiresAt == null) {
            candidateTokenExpiresAt = candidateTokenIssuedAt.plusSeconds(DEFAULT_LINK_TTL_HOURS * 60L * 60L);
        }
    }
}
