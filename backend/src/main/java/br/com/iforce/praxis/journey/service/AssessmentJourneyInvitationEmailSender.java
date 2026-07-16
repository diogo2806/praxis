package br.com.iforce.praxis.journey.service;

/**
 * Porta de entrega do convite de uma jornada de avaliação ao candidato.
 */
public interface AssessmentJourneyInvitationEmailSender {

    /**
     * Envia o link público da jornada para o candidato.
     *
     * @param recipientEmail e-mail do candidato
     * @param candidateName nome do candidato
     * @param journeyName nome da jornada
     * @param invitationUrl link público da tentativa da jornada
     */
    void sendInvitation(
            String recipientEmail,
            String candidateName,
            String journeyName,
            String invitationUrl
    );
}
