package br.com.iforce.praxis.integrity.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.integrity.model.IntegrityReviewAuditAction;
import br.com.iforce.praxis.integrity.model.IntegritySessionStatus;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityReviewAuditEntity;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityReviewEntity;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegritySessionEntity;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityReviewAuditRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityReviewRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegritySessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class IntegrityEvidenceRetentionService {

    private static final int BATCH_SIZE = 100;
    private static final String SYSTEM_ACTOR = "SYSTEM_EVIDENCE_RETENTION";
    private static final List<IntegritySessionStatus> DISCARDABLE_STATUSES = List.of(
            IntegritySessionStatus.CLOSED,
            IntegritySessionStatus.EXPIRED
    );

    private final CandidateIntegritySessionRepository sessionRepository;
    private final CandidateIntegrityReviewRepository reviewRepository;
    private final CandidateIntegrityReviewAuditRepository reviewAuditRepository;
    private final AuditEventService auditEventService;
    private final Clock clock;
    private final int retentionDays;

    public IntegrityEvidenceRetentionService(
            CandidateIntegritySessionRepository sessionRepository,
            CandidateIntegrityReviewRepository reviewRepository,
            CandidateIntegrityReviewAuditRepository reviewAuditRepository,
            AuditEventService auditEventService,
            @Value("${praxis.integrity.evidence-retention-days:180}") int retentionDays
    ) {
        this(
                sessionRepository,
                reviewRepository,
                reviewAuditRepository,
                auditEventService,
                Clock.systemUTC(),
                retentionDays
        );
    }

    IntegrityEvidenceRetentionService(
            CandidateIntegritySessionRepository sessionRepository,
            CandidateIntegrityReviewRepository reviewRepository,
            CandidateIntegrityReviewAuditRepository reviewAuditRepository,
            AuditEventService auditEventService,
            Clock clock,
            int retentionDays
    ) {
        this.sessionRepository = sessionRepository;
        this.reviewRepository = reviewRepository;
        this.reviewAuditRepository = reviewAuditRepository;
        this.auditEventService = auditEventService;
        this.clock = clock;
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Transactional
    public int discardExpiredEvidenceForEmpresa(String empresaId) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(retentionDays, ChronoUnit.DAYS);
        int discardedSessions = 0;
        Page<CandidateIntegritySessionEntity> page;

        do {
            page = sessionRepository.findByEmpresaIdAndStatusInAndClosedAtBeforeOrderByClosedAtAsc(
                    empresaId,
                    DISCARDABLE_STATUSES,
                    cutoff,
                    PageRequest.of(0, BATCH_SIZE)
            );
            if (page.isEmpty()) {
                break;
            }
            Set<String> affectedAttempts = new LinkedHashSet<>();
            for (CandidateIntegritySessionEntity session : page.getContent()) {
                affectedAttempts.add(session.getCandidateAttemptId());
            }
            discardedSessions += page.getNumberOfElements();
            sessionRepository.deleteAll(page.getContent());
            sessionRepository.flush();

            for (String attemptId : affectedAttempts) {
                if (!sessionRepository.existsByEmpresaIdAndCandidateAttemptId(empresaId, attemptId)) {
                    markEvidenceDiscarded(empresaId, attemptId, now);
                }
            }
        } while (page.hasContent());

        return discardedSessions;
    }

    private void markEvidenceDiscarded(String empresaId, String attemptId, Instant discardedAt) {
        CandidateIntegrityReviewEntity review = reviewRepository
                .findByEmpresaIdAndCandidateAttemptId(empresaId, attemptId)
                .orElse(null);
        if (review == null || review.getEvidenceDiscardedAt() != null) {
            return;
        }
        review.setEvidenceDiscardedAt(discardedAt);
        review.setUpdatedAt(discardedAt);
        reviewRepository.save(review);

        CandidateIntegrityReviewAuditEntity audit = new CandidateIntegrityReviewAuditEntity();
        audit.setEmpresaId(empresaId);
        audit.setReviewId(review.getId());
        audit.setCandidateAttemptId(attemptId);
        audit.setAction(IntegrityReviewAuditAction.EVIDENCE_DISCARDED);
        audit.setActorUserId(SYSTEM_ACTOR);
        audit.setDetails("{\"retentionDays\":" + retentionDays + ",\"discardedAt\":\"" + discardedAt + "\"}");
        audit.setCreatedAt(discardedAt);
        reviewAuditRepository.save(audit);

        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                attemptId,
                AuditEventType.INTEGRITY_EVIDENCE_DISCARDED,
                "Evidências técnicas descartadas conforme a política de retenção.",
                audit.getDetails()
        );
    }
}
