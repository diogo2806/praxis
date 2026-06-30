package br.com.iforce.praxis.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação padrão do envio de e-mail de recuperação de senha.
 *
 * <p>Enquanto não há provedor SMTP configurado, o envio é registrado em log para permitir o fluxo
 * de ponta a ponta em desenvolvimento e homologação. O e-mail do destinatário é mascarado e o link
 * (que contém o token puro) só aparece no log quando {@code praxis.auth.password-reset-log-link}
 * está habilitado — nunca em produção.</p>
 *
 * <p>Mensagem enviada (assunto "Redefinição de senha"):</p>
 * <pre>
 * Olá {nome},
 * Recebemos uma solicitação para redefinir sua senha.
 * Caso tenha sido você, utilize o link abaixo.
 * {LINK}
 * Este link expira em {ttl} horas.
 * Se você não solicitou esta alteração, ignore este e-mail.
 * Equipe Práxis
 * </pre>
 */
@Component
public class LoggingPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetEmailSender.class);

    private final boolean logLink;

    public LoggingPasswordResetEmailSender(
            @org.springframework.beans.factory.annotation.Value(
                    "${praxis.auth.password-reset-log-link:false}") boolean logLink
    ) {
        this.logLink = logLink;
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String userName, String resetLink, int ttlHours) {
        if (logLink) {
            log.info(
                    "E-mail de redefinição de senha (assunto: \"Redefinição de senha\") para {}: link={} expira em {}h.",
                    mask(recipientEmail), resetLink, ttlHours
            );
        } else {
            log.info(
                    "E-mail de redefinição de senha (assunto: \"Redefinição de senha\") enviado para {}; expira em {}h.",
                    mask(recipientEmail), ttlHours
            );
        }
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
