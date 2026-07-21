package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptEntity;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyEntity;
import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AssessmentJourneyAttemptLifecycleService {

    private static final int MAX_EXTENSION_DAYS = 365;

    private final AssessmentJourneyAttemptRepository attemptRepository;
    private final AssessmentJourneyService journeyService;
    private final AssessmentJourneyInvitationService invitationService;
    private final CurrentEmpresaService currentEmpresaService;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;

    public AssessmentJourneyAttemptLifecycleService(
            AssessmentJourneyAttemptRepository attemptRepository,
            AssessmentJourneyService journeyService,
            AssessmentJourneyInvitationService invitationService,
            CurrentEmpresaService currentEmpresaService,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata
    ) {
        this.attemptRepository = attemptRepository;
        this.journeyService = journeyService;
        this.invitationService = invitationService;
        this.currentEmpresaService = currentEmpresaService;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
    }

    @Transactional
    public void markInvitationSent(String attemptId) {
        AssessmentJourneyAttemptEntity attempt = findAttempt(attemptId);
        attempt.setInvitationSentAt(Instant.now());
        attemptRepository.save(attempt);
    }

    @Transactional
    public void refreshStatus(String attemptId) {
        expireIfNeeded(findAttempt(attemptId), Instant.now());
    }

    @Transactional
    public void assertUsable(String attemptId) {
        AssessmentJourneyAttemptEntity attempt = findAttempt(attemptId);
        expireIfNeeded(attempt, Instant.now());
        if (attempt.getStatus() == AssessmentJourneyAttemptStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.GONE, "O convite desta jornada expirou.");
        }
        if (attempt.getStatus() == AssessmentJourneyAttemptStatus.ABANDONED) {
            throw new ResponseStatusException(HttpStatus.GONE, "Esta participação foi cancelada.");
        }
    }

    @Transactional
    public void resendInvitation(String attemptId) {
        AssessmentJourneyAttemptEntity attempt = findAttempt(attemptId);
        Instant now = Instant.now();
        expireIfNeeded(attempt, now);
        if (attempt.getStatus() == AssessmentJourneyAttemptStatus.EXPIRED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O convite expirou. Amplie a validade antes de reenviar."
            );
        }
        assertManageable(attempt);

        AssessmentJourneyEntity journey = journeyService
                .findJourneyForEmpresa(attempt.getEmpresaId(), attempt.getJourneyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jornada não encontrada."));
        invitationService.sendInvitation(
                attempt.getCandidateEmail(),
                attempt.getCandidateName(),
                journey.getName(),
                attempt.getId()
        );
        attempt.setInvitationSentAt(now);
        attemptRepository.save(attempt);
        auditEventService.appendAssessmentJourneyAttemptEvent(
                attempt.getEmpresaId(),
                attempt.getId(),
                AuditEventType.CANDIDATE_LINK_RESENT,
                "Convite da jornada reenviado sem criar uma nova participação.",
                auditMetadata.of(
                        "journeyId", attempt.getJourneyId(),
                        "candidateEmail", attempt.getCandidateEmail(),
                        "expiresAt", attempt.getExpiresAt()
                )
        );
    }

    @Transactional
    public void extendValidity(String attemptId, int additionalDays) {
        if (additionalDays < 1 || additionalDays > MAX_EXTENSION_DAYS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A ampliação deve estar entre 1 e 365 dias."
            );
        }

        AssessmentJourneyAttemptEntity attempt = findAttempt(attemptId);
        assertManageable(attempt);
        Instant now = Instant.now();
        Instant previousExpiration = attempt.getExpiresAt();
        boolean reactivated = attempt.getStatus() == AssessmentJourneyAttemptStatus.EXPIRED
                || previousExpiration == null
                || !previousExpiration.isAfter(now);
        Instant base = previousExpiration != null && previousExpiration.isAfter(now)
                ? previousExpiration
                : now;
        attempt.setExpiresAt(base.plusSeconds(additionalDays * 24L * 60L * 60L));
        if (reactivated) {
            attempt.setStatus(attempt.getStartedAt() == null
                    ? AssessmentJourneyAttemptStatus.CREATED
                    : AssessmentJourneyAttemptStatus.IN_PROGRESS);
        }
        attemptRepository.save(attempt);
        auditEventService.appendAssessmentJourneyAttemptEvent(
                attempt.getEmpresaId(),
                attempt.getId(),
                AuditEventType.CANDIDATE_LINK_EXTENDED,
                "Validade do convite da jornada ampliada.",
                auditMetadata.of(
                        "journeyId", attempt.getJourneyId(),
                        "additionalDays", additionalDays,
                        "previousExpiration", previousExpiration,
                        "newExpiration", attempt.getExpiresAt(),
                        "reactivated", reactivated
                )
        );
    }

    @Transactional
    public void cancel(String attemptId) {
        AssessmentJourneyAttemptEntity attempt = findAttempt(attemptId);
        if (attempt.getStatus() == AssessmentJourneyAttemptStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Uma jornada concluída não pode ser cancelada.");
        }
        if (attempt.getStatus() == AssessmentJourneyAttemptStatus.ABANDONED) {
            return;
        }
        Instant now = Instant.now();
        attempt.setStatus(AssessmentJourneyAttemptStatus.ABANDONED);
        attempt.setCanceledAt(now);
        attemptRepository.save(attempt);
        auditEventService.appendAssessmentJourneyAttemptEvent(
                attempt.getEmpresaId(),
                attempt.getId(),
                AuditEventType.ASSESSMENT_JOURNEY_ATTEMPT_ABANDONED,
                "Participação da jornada cancelada pela empresa.",
                auditMetadata.of(
                        "journeyId", attempt.getJourneyId(),
                        "candidateEmail", attempt.getCandidateEmail(),
                        "canceledAt", now
                )
        );
    }

    private void assertManageable(AssessmentJourneyAttemptEntity attempt) {
        if (attempt.getStatus() == AssessmentJourneyAttemptStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A participação já foi concluída.");
        }
        if (attempt.getStatus() == AssessmentJourneyAttemptStatus.ABANDONED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A participação foi cancelada.");
        }
    }

    private void expireIfNeeded(AssessmentJourneyAttemptEntity attempt, Instant now) {
        if ((attempt.getStatus() == AssessmentJourneyAttemptStatus.CREATED
                || attempt.getStatus() == AssessmentJourneyAttemptStatus.IN_PROGRESS)
                && attempt.getExpiresAt() != null
                && !attempt.getExpiresAt().isAfter(now)) {
            attempt.setStatus(AssessmentJourneyAttemptStatus.EXPIRED);
            attemptRepository.save(attempt);
            auditEventService.appendAssessmentJourneyAttemptEvent(
                    attempt.getEmpresaId(),
                    attempt.getId(),
                    AuditEventType.ASSESSMENT_JOURNEY_ATTEMPT_EXPIRED,
                    "Convite da jornada expirado.",
                    auditMetadata.of(
                            "journeyId", attempt.getJourneyId(),
                            "expiresAt", attempt.getExpiresAt()
                    )
            );
        }
    }

    private AssessmentJourneyAttemptEntity findAttempt(String attemptId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return attemptRepository.findByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação da jornada não encontrada."));
    }
}
