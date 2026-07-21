package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptEntity;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyEntity;
import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentJourneyAttemptLifecycleServiceTest {

    private static final String EMPRESA_ID = "empresa-1";
    private static final String ATTEMPT_ID = "jatt-1";
    private static final String JOURNEY_ID = "journey-1";

    @Mock
    private AssessmentJourneyAttemptRepository attemptRepository;

    @Mock
    private AssessmentJourneyService journeyService;

    @Mock
    private AssessmentJourneyInvitationService invitationService;

    @Mock
    private CurrentEmpresaService currentEmpresaService;

    @Mock
    private AuditEventService auditEventService;

    @Mock
    private AuditMetadata auditMetadata;

    private AssessmentJourneyAttemptLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new AssessmentJourneyAttemptLifecycleService(
                attemptRepository,
                journeyService,
                invitationService,
                currentEmpresaService,
                auditEventService,
                auditMetadata
        );
        when(currentEmpresaService.requiredEmpresaId()).thenReturn(EMPRESA_ID);
        when(auditMetadata.of(any(Object[].class))).thenReturn("{}");
    }

    @Test
    void extendValidityReactivatesExpiredInvitation() {
        AssessmentJourneyAttemptEntity attempt = attempt(AssessmentJourneyAttemptStatus.EXPIRED);
        Instant previousExpiration = Instant.now().minus(1, ChronoUnit.DAYS);
        attempt.setExpiresAt(previousExpiration);
        when(attemptRepository.findByEmpresaIdAndId(EMPRESA_ID, ATTEMPT_ID))
                .thenReturn(Optional.of(attempt));

        service.extendValidity(ATTEMPT_ID, 7);

        assertThat(attempt.getStatus()).isEqualTo(AssessmentJourneyAttemptStatus.CREATED);
        assertThat(attempt.getExpiresAt()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));
        verify(attemptRepository).save(attempt);
        verify(auditEventService).appendAssessmentJourneyAttemptEvent(
                eq(EMPRESA_ID),
                eq(ATTEMPT_ID),
                eq(AuditEventType.CANDIDATE_LINK_EXTENDED),
                any(),
                eq("{}")
        );
    }

    @Test
    void resendUsesExistingAttemptWithoutCreatingAnotherParticipation() {
        AssessmentJourneyAttemptEntity attempt = attempt(AssessmentJourneyAttemptStatus.CREATED);
        attempt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        AssessmentJourneyEntity journey = new AssessmentJourneyEntity();
        journey.setId(JOURNEY_ID);
        journey.setEmpresaId(EMPRESA_ID);
        journey.setName("Jornada de liderança");
        when(attemptRepository.findByEmpresaIdAndId(EMPRESA_ID, ATTEMPT_ID))
                .thenReturn(Optional.of(attempt));
        when(journeyService.findJourneyForEmpresa(EMPRESA_ID, JOURNEY_ID))
                .thenReturn(Optional.of(journey));

        service.resendInvitation(ATTEMPT_ID);

        verify(invitationService).sendInvitation(
                attempt.getCandidateEmail(),
                attempt.getCandidateName(),
                journey.getName(),
                ATTEMPT_ID
        );
        assertThat(attempt.getInvitationSentAt()).isNotNull();
        verify(attemptRepository).save(attempt);
        verify(auditEventService).appendAssessmentJourneyAttemptEvent(
                eq(EMPRESA_ID),
                eq(ATTEMPT_ID),
                eq(AuditEventType.CANDIDATE_LINK_RESENT),
                any(),
                eq("{}")
        );
    }

    @Test
    void cancelPreservesHistoryAndBlocksFurtherUse() {
        AssessmentJourneyAttemptEntity attempt = attempt(AssessmentJourneyAttemptStatus.IN_PROGRESS);
        attempt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        when(attemptRepository.findByEmpresaIdAndId(EMPRESA_ID, ATTEMPT_ID))
                .thenReturn(Optional.of(attempt));

        service.cancel(ATTEMPT_ID);

        assertThat(attempt.getStatus()).isEqualTo(AssessmentJourneyAttemptStatus.ABANDONED);
        assertThat(attempt.getCanceledAt()).isNotNull();
        verify(attemptRepository).save(attempt);
        verify(auditEventService).appendAssessmentJourneyAttemptEvent(
                eq(EMPRESA_ID),
                eq(ATTEMPT_ID),
                eq(AuditEventType.ASSESSMENT_JOURNEY_ATTEMPT_ABANDONED),
                any(),
                eq("{}")
        );
    }

    private AssessmentJourneyAttemptEntity attempt(AssessmentJourneyAttemptStatus status) {
        AssessmentJourneyAttemptEntity attempt = new AssessmentJourneyAttemptEntity();
        attempt.setId(ATTEMPT_ID);
        attempt.setEmpresaId(EMPRESA_ID);
        attempt.setJourneyId(JOURNEY_ID);
        attempt.setCandidateName("Maria Silva");
        attempt.setCandidateEmail("maria@example.com");
        attempt.setSequenceKey("principal");
        attempt.setStatus(status);
        attempt.setCreatedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        return attempt;
    }
}
