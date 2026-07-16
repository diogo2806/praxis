package br.com.iforce.praxis.journey.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envia o convite da jornada por e-mail quando o provedor está habilitado.
 * Em desenvolvimento e homologação, registra somente uma evidência segura no log.
 */
@Component
public class LoggingAssessmentJourneyInvitationEmailSender implements AssessmentJourneyInvitationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingAssessmentJourneyInvitationEmailSender.class);

    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final String from;
    private final boolean logLink;

    public LoggingAssessmentJourneyInvitationEmailSender(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${praxis.email.enabled:false}") boolean emailEnabled,
            @Value("${praxis.email.from:no-reply@praxis.local}") String from,
            @Value("${praxis.journey.invitation-log-link:false}") boolean logLink
    ) {
        this.mailSender = mailSender.getIfAvailable();
        this.emailEnabled = emailEnabled;
        this.from = from;
        this.logLink = logLink;
    }

    @Override
    public void sendInvitation(
            String recipientEmail,
            String candidateName,
            String journeyName,
            String invitationUrl
    ) {
        if (emailEnabled && mailSender != null) {
            MimeMessage message = mailSender.createMimeMessage();
            try {
                MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
                helper.setFrom(from);
                helper.setTo(recipientEmail);
                helper.setSubject("Convite para jornada de avaliação - Práxis");
                helper.setText(text(candidateName, journeyName, invitationUrl), false);
                mailSender.send(message);
                return;
            } catch (MessagingException exception) {
                throw new IllegalStateException("Não foi possível montar o convite da jornada.", exception);
            }
        }

        if (logLink) {
            log.info(
                    "Convite da jornada '{}' registrado para {}: url={}.",
                    journeyName,
                    mask(recipientEmail),
                    invitationUrl
            );
        } else {
            log.info("Convite da jornada '{}' registrado para {}.", journeyName, mask(recipientEmail));
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
