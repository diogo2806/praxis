package br.com.iforce.praxis.shared.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Canal único para mensagens transacionais por e-mail.
 *
 * <p>O fallback em log é opt-in e destinado somente a desenvolvimento ou homologação. Sem um
 * provedor real e sem a liberação explícita do fallback, a entrega falha de forma visível para que
 * produção nunca aceite silenciosamente uma notificação que não será recebida.</p>
 */
@Service
public class EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final boolean consoleFallbackEnabled;
    private final String from;

    public EmailDeliveryService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${praxis.email.enabled:false}") boolean emailEnabled,
            @Value("${praxis.notifications.console-fallback-enabled:false}") boolean consoleFallbackEnabled,
            @Value("${praxis.email.from:no-reply@praxis.local}") String from
    ) {
        this.mailSender = mailSender.getIfAvailable();
        this.emailEnabled = emailEnabled;
        this.consoleFallbackEnabled = consoleFallbackEnabled;
        this.from = from;
    }

    /**
     * Envia uma mensagem de texto simples.
     *
     * @return {@code true} quando a mensagem foi entregue ao provedor; {@code false} somente quando
     *         o fallback em log foi explicitamente habilitado
     */
    public boolean sendPlainText(String recipient, String subject, String body) {
        if (emailEnabled && mailSender != null) {
            MimeMessage message = mailSender.createMimeMessage();
            try {
                MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
                helper.setFrom(from);
                helper.setTo(recipient);
                helper.setSubject(subject);
                helper.setText(body, false);
                mailSender.send(message);
                return true;
            } catch (MessagingException exception) {
                throw new IllegalStateException("Não foi possível montar a mensagem transacional.", exception);
            }
        }

        if (consoleFallbackEnabled) {
            return false;
        }

        throw new IllegalStateException(
                "Canal real de e-mail não configurado. Configure praxis.email.enabled e o provedor SMTP "
                        + "ou habilite explicitamente o fallback de console apenas fora de produção."
        );
    }
}
