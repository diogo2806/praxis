package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.EmpresaHealthResponse;
import br.com.iforce.praxis.shared.notification.service.EmailDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Entrega o alerta de Customer Success por e-mail. O fallback em log só é permitido quando
 * habilitado explicitamente para desenvolvimento ou homologação.
 */
@Component
public class LoggingCustomerSuccessAlertSender implements CustomerSuccessAlertSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingCustomerSuccessAlertSender.class);

    private final EmailDeliveryService emailDeliveryService;

    public LoggingCustomerSuccessAlertSender(EmailDeliveryService emailDeliveryService) {
        this.emailDeliveryService = emailDeliveryService;
    }

    @Override
    public void sendAtRiskDigest(String csTeamEmail, List<EmpresaHealthResponse> atRisk, Instant generatedAt) {
        if (atRisk == null || atRisk.isEmpty()) {
            return;
        }
        if (csTeamEmail == null || csTeamEmail.isBlank()) {
            throw new IllegalStateException("E-mail do time de Customer Success não configurado.");
        }

        boolean delivered = emailDeliveryService.sendPlainText(
                csTeamEmail,
                "Clientes com queda de utilização - Práxis",
                body(atRisk, generatedAt)
        );
        if (delivered) {
            return;
        }

        log.warn("Fallback de console para alerta de retenção: {} cliente(s) em risco em {}.",
                atRisk.size(), generatedAt);
        for (EmpresaHealthResponse health : atRisk) {
            log.warn("Cliente em risco: {} ({}) — concluídas {}→{} no período, queda de {}%, score {}.",
                    health.name(),
                    health.empresaId(),
                    health.completedPreviousPeriod(),
                    health.completedCurrentPeriod(),
                    formatPercent(health.dropPercent()),
                    health.healthScore());
        }
    }

    private String body(List<EmpresaHealthResponse> atRisk, Instant generatedAt) {
        StringBuilder body = new StringBuilder();
        body.append("A varredura de saúde da Práxis identificou ")
                .append(atRisk.size())
                .append(" cliente(s) com queda relevante de utilização em ")
                .append(generatedAt)
                .append(".\n\n");

        for (EmpresaHealthResponse health : atRisk) {
            body.append("• ")
                    .append(health.name())
                    .append(" (")
                    .append(health.empresaId())
                    .append("): ")
                    .append(health.completedPreviousPeriod())
                    .append(" → ")
                    .append(health.completedCurrentPeriod())
                    .append(" avaliações; queda de ")
                    .append(formatPercent(health.dropPercent()))
                    .append("%; score ")
                    .append(health.healthScore())
                    .append(".\n");
        }

        body.append("\nAcesse o painel administrativo para priorizar a atuação de Customer Success.");
        return body.toString();
    }

    private static String formatPercent(Double dropPercent) {
        if (dropPercent == null) {
            return "?";
        }
        return String.valueOf(Math.round(dropPercent * 100));
    }
}
