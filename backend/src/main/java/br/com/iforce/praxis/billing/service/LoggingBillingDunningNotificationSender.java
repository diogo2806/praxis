package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.billing.dto.DunningNotice;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;


/**
 * Implementação padrão do canal de cobrança educativa (dunning).
 *
 * <p>Enquanto não há provedor de e-mail/SMS configurado, o toque é registrado em log para permitir
 * o fluxo de cobrança de ponta a ponta em desenvolvimento e homologação. O e-mail do destinatário é
 * mascarado e o telefone tem apenas os últimos dígitos preservados; nenhuma credencial financeira é
 * registrada. A mensagem é sempre educativa — orienta a regularizar antes da suspensão dura.</p>
 */
@Component
public class LoggingBillingDunningNotificationSender implements BillingDunningNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingBillingDunningNotificationSender.class);

    @Override
    public void sendRetryNotice(DunningNotice notice) {
        log.info(
                "Régua de cobrança ({}): toque educativo de retry para cliente {} — e-mail {}, SMS {}, situação {}, carência até {}.",
                notice.stage(),
                notice.empresaId(),
                maskEmail(notice.corporateEmail()),
                maskPhone(notice.phone()),
                notice.status(),
                notice.graceUntil()
        );
    }

    private static String maskEmail(String value) {
        if (value == null || value.isBlank()) {
            return "(sem e-mail)";
        }
        int at = value.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at);
    }

    private static String maskPhone(String value) {
        if (value == null || value.isBlank()) {
            return "(sem telefone)";
        }
        if (value.length() <= 4) {
            return "***";
        }
        return "***" + value.substring(value.length() - 4);
    }
}
