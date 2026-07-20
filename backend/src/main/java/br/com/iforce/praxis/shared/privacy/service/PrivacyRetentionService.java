package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.privacy.persistence.entity.DataSubjectRequestEntity;
import br.com.iforce.praxis.shared.privacy.persistence.entity.HumanReviewRequestEntity;
import br.com.iforce.praxis.shared.privacy.persistence.repository.DataSubjectRequestRepository;
import br.com.iforce.praxis.shared.privacy.persistence.repository.HumanReviewRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Conduz a retenção e o descarte seguro de dados pessoais dos candidatos. */
@Service
public class PrivacyRetentionService {

    private static final int BATCH_SIZE = 100;
    private static final List<AttemptStatus> RETAINED_STATUSES = List.of(AttemptStatus.values());

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final DataSubjectRequestRepository dataSubjectRequestRepository;
    private final HumanReviewRequestRepository humanReviewRequestRepository;
    private final Clock clock;
    private final int retentionDays;

    @Autowired
    public PrivacyRetentionService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            DataSubjectRequestRepository dataSubjectRequestRepository,
            HumanReviewRequestRepository humanReviewRequestRepository,
            @Value("${praxis.privacy-retention-days:180}") int retentionDays
    ) {
        this(
                candidateAttemptRepository,
                auditEventService,
                dataSubjectRequestRepository,
                humanReviewRequestRepository,
                Clock.systemUTC(),
                retentionDays
        );
    }

    PrivacyRetentionService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            DataSubjectRequestRepository dataSubjectRequestRepository,
            HumanReviewRequestRepository humanReviewRequestRepository,
            Clock clock,
            int retentionDays
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.dataSubjectRequestRepository = dataSubjectRequestRepository;
        this.humanReviewRequestRepository = humanReviewRequestRepository;
        this.clock = clock;
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Transactional
    public int anonymizeExpiredAttemptsForEmpresa(String empresaId) {
        Instant cutoff = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
        List<CandidateAttemptEntity> candidates = candidateAttemptRepository.findRetentionCandidatesForEmpresa(
                empresaId,
                RETAINED_STATUSES,
                cutoff,
                PageRequest.of(0, BATCH_SIZE)
        );

        Instant anonymizedAt = Instant.now(clock);
        candidates.forEach(candidate -> anonymize(
                candidate,
                anonymizedAt,
                "Dados pessoais da tentativa anonimizados por política de retenção.",
                "{\"retentionDays\":" + retentionDays + ",\"anonymizedAt\":\"" + anonymizedAt + "\"}"
        ));
        return candidates.size();
    }

    @Transactional
    public void anonymizeAttemptNow(String empresaId, String attemptId, String actorUserId, String reason) {
        CandidateAttemptEntity candidate = candidateAttemptRepository.findByEmpresaIdAndIdForUpdate(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        if (candidate.getAnonymizedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tentativa já tratada anteriormente.");
        }
        Instant anonymizedAt = Instant.now(clock);
        String safeReason = reason == null || reason.isBlank()
                ? "Solicitação operacional LGPD"
                : reason.trim().replace("\"", "'");
        anonymize(
                candidate,
                anonymizedAt,
                "Dados pessoais da tentativa anonimizados por solicitação operacional.",
                "{\"actorUserId\":\"" + actorUserId + "\",\"reason\":\"" + safeReason
                        + "\",\"anonymizedAt\":\"" + anonymizedAt + "\"}"
        );
    }

    private void anonymize(CandidateAttemptEntity candidate, Instant anonymizedAt, String message, String metadata) {
        String attemptId = candidate.getId();
        candidate.setCandidateName("Candidato anonimizado");
        candidate.setCandidateEmail("anonimizado+" + attemptId + "@privacy.local");
        candidate.setCompanyId("anonymized");
        candidate.setResultId(shortIdentifier("anon-result:", attemptId));
        candidate.setIdempotencyKey("anonymized:" + attemptId);
        candidate.setRequestFingerprint(null);
        candidate.setRequestFingerprintVersion(null);
        candidate.setGupyJobId(null);
        candidate.setCallbackUrl(null);
        candidate.setResultWebhookUrl(null);
        candidate.setScore(null);
        candidate.setDecision(ResultDecision.NO_RECOMMENDATION);
        candidate.setHumanReviewRequired(false);
        candidate.setHumanReviewCompletedAt(null);
        candidate.setHumanReviewedBy(null);
        candidate.setHumanReviewResolution(null);
        candidate.setAccommodationTimeMultiplier(BigDecimal.ONE);
        candidate.setCompanyResultString("Dados individuais removidos pela política de retenção.");
        candidate.setCandidateTokenExpiresAt(anonymizedAt);
        candidate.setAnonymizedAt(anonymizedAt);
        candidate.getAnswers().clear();
        candidate.getNodeServes().clear();
        candidate.getResultItems().clear();
        redactWorkflowContent(attemptId, anonymizedAt);

        auditEventService.appendCandidateAttemptEvent(
                candidate.getEmpresaId(),
                attemptId,
                AuditEventType.ATTEMPT_ANONYMIZED,
                message,
                metadata
        );
    }

    private void redactWorkflowContent(String attemptId, Instant updatedAt) {
        List<DataSubjectRequestEntity> dataRequests = dataSubjectRequestRepository.findByAttemptId(attemptId);
        for (DataSubjectRequestEntity request : dataRequests) {
            request.setContact(null);
            request.setDetails(null);
            request.setResolution(request.getResolution() == null ? null : "Conteúdo removido após anonimização.");
            request.setDenialReason(request.getDenialReason() == null ? null : "Conteúdo removido após anonimização.");
            request.setUpdatedAt(updatedAt);
        }
        dataSubjectRequestRepository.saveAll(dataRequests);

        List<HumanReviewRequestEntity> reviewRequests = humanReviewRequestRepository.findByAttemptId(attemptId);
        for (HumanReviewRequestEntity request : reviewRequests) {
            request.setReason(null);
            request.setResolution(request.getResolution() == null ? null : "Conteúdo removido após anonimização.");
            request.setUpdatedAt(updatedAt);
        }
        humanReviewRequestRepository.saveAll(reviewRequests);
    }

    private String shortIdentifier(String prefix, String value) {
        String combined = prefix + value;
        return combined.length() <= 80 ? combined : combined.substring(0, 80);
    }
}
