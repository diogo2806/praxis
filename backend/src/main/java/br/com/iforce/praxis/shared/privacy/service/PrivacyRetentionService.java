package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PrivacyRetentionService {

    private static final int BATCH_SIZE = 100;
    private static final List<AttemptStatus> CLOSED_STATUSES = List.of(
            AttemptStatus.COMPLETED,
            AttemptStatus.ABANDONED,
            AttemptStatus.EXPIRED,
            AttemptStatus.FAILED
    );

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final Clock clock;
    private final int retentionDays;

    @Autowired
    public PrivacyRetentionService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            @Value("${praxis.privacy-retention-days:180}") int retentionDays
    ) {
        this(candidateAttemptRepository, auditEventService, Clock.systemUTC(), retentionDays);
    }

    PrivacyRetentionService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            Clock clock,
            int retentionDays
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Transactional
    public int anonymizeExpiredAttemptsForTenant(String tenantId) {
        Instant cutoff = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
        List<CandidateAttemptEntity> candidates = candidateAttemptRepository.findRetentionCandidatesForTenant(
                tenantId,
                CLOSED_STATUSES,
                cutoff,
                PageRequest.of(0, BATCH_SIZE)
        );

        Instant anonymizedAt = Instant.now(clock);
        candidates.forEach(candidate -> anonymize(candidate, anonymizedAt));
        return candidates.size();
    }

    private void anonymize(CandidateAttemptEntity candidate, Instant anonymizedAt) {
        String attemptId = candidate.getId();
        candidate.setCandidateName("Candidato anonimizado");
        candidate.setCandidateEmail("anonimizado+" + attemptId + "@privacy.local");
        candidate.setIdempotencyKey("anonymized:" + attemptId);
        candidate.setResultWebhookUrl(null);
        candidate.setAnonymizedAt(anonymizedAt);

        auditEventService.appendCandidateAttemptEvent(
                candidate.getTenantId(),
                attemptId,
                AuditEventType.ATTEMPT_ANONYMIZED,
                "Dados pessoais da tentativa anonimizados por politica de retencao.",
                "{\"retentionDays\":" + retentionDays + ",\"anonymizedAt\":\"" + anonymizedAt + "\"}"
        );
    }
}
