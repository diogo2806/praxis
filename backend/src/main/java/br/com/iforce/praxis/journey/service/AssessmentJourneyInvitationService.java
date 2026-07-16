package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;
import br.com.iforce.praxis.journey.dto.CreateJourneyAttemptRequest;
import org.springframework.stereotype.Service;

/**
 * Cria a tentativa da jornada e entrega o convite ao candidato após a persistência.
 */
@Service
public class AssessmentJourneyInvitationService {

    private final AssessmentJourneyAttemptService attemptService;
    private final AssessmentJourneyInvitationEmailSender invitationEmailSender;
    private final PraxisProperties praxisProperties;

    public AssessmentJourneyInvitationService(
            AssessmentJourneyAttemptService attemptService,
            AssessmentJourneyInvitationEmailSender invitationEmailSender,
            PraxisProperties praxisProperties
    ) {
        this.attemptService = attemptService;
        this.invitationEmailSender = invitationEmailSender;
        this.praxisProperties = praxisProperties;
    }

    /**
     * Persiste a tentativa e, após o retorno transacional do serviço de jornada, envia o convite.
     *
     * @param request dados da jornada e do candidato
     * @return tentativa criada
     */
    public AssessmentJourneyAttemptResponse createAttemptAndSendInvitation(CreateJourneyAttemptRequest request) {
        AssessmentJourneyAttemptResponse response = attemptService.createAttempt(request);
        invitationEmailSender.sendInvitation(
                response.candidateEmail(),
                response.candidateName(),
                response.journeyName(),
                journeyAttemptUrl(response.id())
        );
        return response;
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
