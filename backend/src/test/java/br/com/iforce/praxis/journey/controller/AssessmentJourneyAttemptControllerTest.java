package br.com.iforce.praxis.journey.controller;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;
import br.com.iforce.praxis.journey.dto.CreateJourneyAttemptRequest;
import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;
import br.com.iforce.praxis.journey.service.AssessmentJourneyAttemptLifecycleService;
import br.com.iforce.praxis.journey.service.AssessmentJourneyAttemptService;
import br.com.iforce.praxis.journey.service.AssessmentJourneyInvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentJourneyAttemptControllerTest {

    @Mock
    private AssessmentJourneyAttemptService attemptService;

    @Mock
    private AssessmentJourneyAttemptLifecycleService lifecycleService;

    @Mock
    private AssessmentJourneyInvitationService invitationService;

    @Mock
    private AuditEventService auditEventService;

    private AssessmentJourneyAttemptController controller;

    @BeforeEach
    void setUp() {
        controller = new AssessmentJourneyAttemptController(
                attemptService,
                lifecycleService,
                invitationService,
                auditEventService
        );
    }

    @Test
    void createPersistsAttemptSendsInvitationAndRecordsDelivery() {
        CreateJourneyAttemptRequest request = new CreateJourneyAttemptRequest(
                "tss-137911ed",
                "Maria Silva",
                "maria@example.com",
                "principal"
        );
        AssessmentJourneyAttemptResponse attempt = new AssessmentJourneyAttemptResponse(
                "jatt_99d341192c0949c595930403b94a0c76",
                "tss-137911ed",
                "tss",
                "Maria Silva",
                "maria@example.com",
                "principal",
                AssessmentJourneyAttemptStatus.CREATED,
                null,
                null,
                Instant.parse("2026-07-16T13:19:13Z"),
                List.of()
        );
        when(attemptService.createAttempt(request)).thenReturn(attempt);

        ResponseEntity<AssessmentJourneyAttemptResponse> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(attempt);
        verify(invitationService).sendInvitation(attempt);
        verify(lifecycleService).markInvitationSent(attempt.id());
    }
}
