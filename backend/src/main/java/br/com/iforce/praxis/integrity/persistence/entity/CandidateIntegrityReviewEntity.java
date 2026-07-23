package br.com.iforce.praxis.integrity.persistence.entity;

import br.com.iforce.praxis.integrity.model.IntegrityReviewDecision;
import br.com.iforce.praxis.integrity.model.IntegrityReviewStatus;
import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidate_integrity_reviews")
public class CandidateIntegrityReviewEntity implements EmpresaAwareEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "candidate_attempt_id", nullable = false, unique = true, length = 80)
    private String candidateAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IntegrityReviewStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 50)
    private IntegrityReviewDecision decision;

    @Column(name = "justification", length = 2000)
    private String justification;

    @Column(name = "share_with_company", nullable = false)
    private boolean shareWithCompany;

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "retention_until", nullable = false)
    private Instant retentionUntil;

    @Column(name = "evidence_discarded_at")
    private Instant evidenceDiscardedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
