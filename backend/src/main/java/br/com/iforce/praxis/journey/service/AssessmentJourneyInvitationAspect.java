package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Entrega o convite somente depois que o endpoint conclui a criação da tentativa.
 * Nesse ponto, a transação do serviço de jornada já foi finalizada.
 */
@Aspect
@Component
public class AssessmentJourneyInvitationAspect {

    private final AssessmentJourneyInvitationService invitationService;

    public AssessmentJourneyInvitationAspect(AssessmentJourneyInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @AfterReturning(
            pointcut = "execution(* br.com.iforce.praxis.journey.controller.AssessmentJourneyAttemptController.create(..))",
            returning = "responseEntity"
    )
    public void sendInvitationAfterAttemptCreation(ResponseEntity<?> responseEntity) {
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            return;
        }
        Object responseBody = responseEntity.getBody();
        if (responseBody instanceof AssessmentJourneyAttemptResponse attempt) {
            invitationService.sendInvitation(attempt);
        }
    }
}
