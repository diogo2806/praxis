package br.com.iforce.praxis.integrity.persistence.entity;

import br.com.iforce.praxis.integrity.model.IntegrityReviewAuditAction;
import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidate_integrity_review_audit")
public class CandidateIntegrityReviewAuditEntity implements EmpresaAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "review_id", nullable = false, length = 36)
    private String reviewId;

    @Column(name = "candidate_attempt_id", nullable = false, length = 80)
    private String candidateAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private IntegrityReviewAuditAction action;

    @Column(name = "actor_user_id", nullable = false, length = 120)
    private String actorUserId;

    @Column(name = "details", length = 4000)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
