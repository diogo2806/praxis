package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;
import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssessmentJourneyInvitationAspectTest {

    @Mock
    private AssessmentJourneyInvitationService invitationService;

    private AssessmentJourneyInvitationAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new AssessmentJourneyInvitationAspect(invitationService);
    }

    @Test
    void sendsInvitationAfterSuccessfulCreation() {
        AssessmentJourneyAttemptResponse attempt = attempt();

        aspect.sendInvitationAfterAttemptCreation(ResponseEntity.status(HttpStatus.CREATED).body(attempt));

        verify(invitationService).sendInvitation(attempt);
    }

    @Test
    void ignoresResponseWithoutAttemptBody() {
        aspect.sendInvitationAfterAttemptCreation(ResponseEntity.status(HttpStatus.CREATED).build());

        verify(invitationService, never()).sendInvitation(org.mockito.ArgumentMatchers.any());
    }

    private AssessmentJourneyAttemptResponse attempt() {
        return new AssessmentJourneyAttemptResponse(
                "jatt_1",
                "journey-1",
                "Jornada",
                "Maria Silva",
                "maria@example.com",
                "principal",
                AssessmentJourneyAttemptStatus.CREATED,
                null,
                null,
                Instant.parse("2026-07-16T13:19:13Z"),
                List.of()
        );
    }
}
