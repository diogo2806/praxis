package br.com.iforce.praxis.auth.service;

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
 * Entrega mensagens de acesso por e-mail quando há provedor configurado; caso contrário,
 * registra uma evidência segura em log para desenvolvimento e homologação.
 */
@Component
public class LoggingPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetEmailSender.class);

    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final String from;
    private final boolean logLink;

    public LoggingPasswordResetEmailSender(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${praxis.email.enabled:false}") boolean emailEnabled,
            @Value("${praxis.email.from:no-reply@praxis.local}") String from,
            @Value("${praxis.auth.password-reset-log-link:false}") boolean logLink
    ) {
        this.mailSender = mailSender.getIfAvailable();
        this.emailEnabled = emailEnabled;
        this.from = from;
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
        if (emailEnabled && mailSender != null) {
            MimeMessage message = mailSender.createMimeMessage();
            try {
                MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
                helper.setFrom(from);
                helper.setTo(recipientEmail);
                helper.setSubject(subject);
                helper.setText(text(userName, intro, instruction, url, ttlHours), false);
                mailSender.send(message);
                return;
            } catch (MessagingException exception) {
                throw new IllegalStateException("Não foi possível montar a mensagem transacional.", exception);
            }
        }
        if (logLink) {
            log.info("Mensagem '{}' para {}: url={} expira em {}h.", subject, mask(recipientEmail), url, ttlHours);
        } else {
            log.info("Mensagem '{}' registrada para {}; expira em {}h.", subject, mask(recipientEmail), ttlHours);
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
