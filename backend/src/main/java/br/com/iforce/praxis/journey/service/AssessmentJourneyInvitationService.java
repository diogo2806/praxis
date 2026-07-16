package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;
import org.springframework.stereotype.Service;

/**
 * Monta e entrega o convite público de uma tentativa de jornada já persistida.
 */
@Service
public class AssessmentJourneyInvitationService {

    private final AssessmentJourneyInvitationEmailSender invitationEmailSender;
    private final PraxisProperties praxisProperties;

    public AssessmentJourneyInvitationService(
            AssessmentJourneyInvitationEmailSender invitationEmailSender,
            PraxisProperties praxisProperties
    ) {
        this.invitationEmailSender = invitationEmailSender;
        this.praxisProperties = praxisProperties;
    }

    /**
     * Envia o convite correspondente a uma tentativa criada com sucesso.
     *
     * @param attempt tentativa da jornada já persistida
     */
    public void sendInvitation(AssessmentJourneyAttemptResponse attempt) {
        invitationEmailSender.sendInvitation(
                attempt.candidateEmail(),
                attempt.candidateName(),
                attempt.journeyName(),
                journeyAttemptUrl(attempt.id())
        );
    }

    private String journeyAttemptUrl(String attemptId) {
        String baseUrl = praxisProperties.candidatePageBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = praxisProperties.publicBaseUrl();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("A URL pública da página do candidato não foi configurada.");
        }
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return normalizedBaseUrl + "/jornada/" + attemptId;
    }
}
