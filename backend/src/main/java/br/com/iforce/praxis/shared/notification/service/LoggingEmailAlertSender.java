package br.com.iforce.praxis.shared.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(EmailAlertSender.class)
public class LoggingEmailAlertSender implements EmailAlertSender {

    @Override
    public void send(EmailAlertMessage message) {
        log.warn(
                "Alerta de e-mail DLQ gerado para tenant={} destinatario={} assunto={}",
                message.tenantId(),
                message.to(),
                message.subject()
        );
    }
}
