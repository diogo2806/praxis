package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;
import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssessmentJourneyInvitationServiceTest {

    @Mock
    private AssessmentJourneyInvitationEmailSender invitationEmailSender;

    private AssessmentJourneyInvitationService service;

    @BeforeEach
    void setUp() {
        PraxisProperties properties = new PraxisProperties(
                "https://api.praxis.iforce.com.br",
                "https://praxis.iforce.com.br/",
                168,
                24,
                720,
                70,
                15,
                0.001
        );
        service = new AssessmentJourneyInvitationService(invitationEmailSender, properties);
    }

    @Test
    void sendsInvitationWithPublicJourneyUrl() {
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

        service.sendInvitation(attempt);

        verify(invitationEmailSender).sendInvitation(
                "maria@example.com",
                "Maria Silva",
                "tss",
                "https://praxis.iforce.com.br/jornada/jatt_99d341192c0949c595930403b94a0c76"
        );
    }
}
