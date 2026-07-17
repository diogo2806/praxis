package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.shared.notification.service.EmailDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Entrega mensagens de acesso por e-mail. O fallback em log só é permitido quando habilitado
 * explicitamente para desenvolvimento ou homologação.
 */
@Component
public class LoggingPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetEmailSender.class);

    private final EmailDeliveryService emailDeliveryService;
    private final boolean logLink;

    public LoggingPasswordResetEmailSender(
            EmailDeliveryService emailDeliveryService,
            @Value("${praxis.auth.password-reset-log-link:false}") boolean logLink
    ) {
        this.emailDeliveryService = emailDeliveryService;
        this.logLink = logLink;
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String userName, String resetLink, int ttlHours) {
        sendAccessMessage(
                recipientEmail,
                "Redefinição de senha - Práxis",
                "Recebemos uma solicitação para redefinir sua senha.",
                "Use o endereço abaixo para criar uma nova senha:",
                resetLink,
                ttlHours,
                userName
        );
    }

    @Override
    public void sendTeamInviteEmail(String recipientEmail, String userName, String inviteUrl, int ttlHours) {
        sendAccessMessage(
                recipientEmail,
                "Convite para acessar o Práxis",
                "Você foi convidado para acessar o Práxis.",
                "Use o endereço abaixo para definir sua senha e entrar:",
                inviteUrl,
                ttlHours,
                userName
        );
    }

    private void sendAccessMessage(
            String recipientEmail,
            String subject,
            String intro,
            String instruction,
            String url,
            int ttlHours,
            String userName
    ) {
        boolean delivered = emailDeliveryService.sendPlainText(
                recipientEmail,
                subject,
                text(userName, intro, instruction, url, ttlHours)
        );
        if (delivered) {
            return;
        }

        if (logLink) {
            log.warn("Fallback de console para '{}', destinatário {}: url={} expira em {}h.",
                    subject, mask(recipientEmail), url, ttlHours);
        } else {
            log.warn("Fallback de console para '{}', destinatário {}; expira em {}h.",
                    subject, mask(recipientEmail), ttlHours);
        }
    }

    private String text(String userName, String intro, String instruction, String url, int ttlHours) {
        String greeting = userName == null || userName.isBlank() ? "Olá" : "Olá, " + userName.trim();
        return greeting + "\n\n"
                + intro + "\n"
                + instruction + "\n\n"
                + url + "\n\n"
                + "Este acesso expira em " + ttlHours + " hora" + (ttlHours == 1 ? "" : "s") + ".\n"
                + "Se você não reconhece esta solicitação, ignore esta mensagem.\n\n"
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
