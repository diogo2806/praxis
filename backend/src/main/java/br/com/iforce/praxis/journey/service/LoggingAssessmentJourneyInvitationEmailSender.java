package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.shared.notification.service.EmailDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Envia o convite da jornada por e-mail. O fallback em log só é permitido quando habilitado
 * explicitamente para desenvolvimento ou homologação.
 */
@Component
public class LoggingAssessmentJourneyInvitationEmailSender implements AssessmentJourneyInvitationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingAssessmentJourneyInvitationEmailSender.class);

    private final EmailDeliveryService emailDeliveryService;
    private final boolean logLink;

    public LoggingAssessmentJourneyInvitationEmailSender(
            EmailDeliveryService emailDeliveryService,
            @Value("${praxis.journey.invitation-log-link:false}") boolean logLink
    ) {
        this.emailDeliveryService = emailDeliveryService;
        this.logLink = logLink;
    }

    @Override
    public void sendInvitation(
            String recipientEmail,
            String candidateName,
            String journeyName,
            String invitationUrl
    ) {
        boolean delivered = emailDeliveryService.sendPlainText(
                recipientEmail,
                "Convite para jornada de avaliação - Práxis",
                text(candidateName, journeyName, invitationUrl)
        );
        if (delivered) {
            return;
        }

        if (logLink) {
            log.warn(
                    "Fallback de console para convite da jornada '{}', destinatário {}: url={}.",
                    journeyName,
                    mask(recipientEmail),
                    invitationUrl
            );
        } else {
            log.warn("Fallback de console para convite da jornada '{}', destinatário {}.",
                    journeyName, mask(recipientEmail));
        }
    }

    private String text(String candidateName, String journeyName, String invitationUrl) {
        String greeting = candidateName == null || candidateName.isBlank()
                ? "Olá"
                : "Olá, " + candidateName.trim();
        String normalizedJourneyName = journeyName == null || journeyName.isBlank()
                ? "sua jornada de avaliação"
                : journeyName.trim();

        return greeting + "\n\n"
                + "Você recebeu um convite para realizar a jornada de avaliação \""
                + normalizedJourneyName
                + "\" no Práxis.\n"
                + "Use o endereço abaixo para iniciar e acompanhar as etapas na ordem definida:\n\n"
                + invitationUrl + "\n\n"
                + "Caso você não reconheça este convite, ignore esta mensagem.\n\n"
                + "Equipe Práxis";
    }

    private static String mask(String value) {
        if (value == null) {
            return "***";
        }
        int at = value.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at);
    }
}
