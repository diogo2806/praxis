package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.EmpresaHealthResponse;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;


import java.time.Instant;

import java.util.List;


/**
 * Implementação padrão do alerta de Customer Success.
 *
 * <p>Enquanto não há provedor de e-mail/mensageria configurado, o alerta é registrado em log para
 * permitir o fluxo de retenção de ponta a ponta em desenvolvimento e homologação. Cada cliente em
 * risco é listado com a queda de uso medida, para que o time de sucesso do cliente priorize a
 * intervenção. Nenhum dado sensível de candidato é registrado — apenas identificação comercial do
 * cliente e a métrica de queda.</p>
 */
@Component
public class LoggingCustomerSuccessAlertSender implements CustomerSuccessAlertSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingCustomerSuccessAlertSender.class);

    @Override
    public void sendAtRiskDigest(String csTeamEmail, List<EmpresaHealthResponse> atRisk, Instant generatedAt) {
        if (atRisk == null || atRisk.isEmpty()) {
            return;
        }
        String destination = csTeamEmail == null || csTeamEmail.isBlank() ? "(canal de CS não configurado)" : csTeamEmail;
        log.info("Alerta de retenção para Customer Success ({}): {} cliente(s) em risco em {}.",
                destination, atRisk.size(), generatedAt);
        for (EmpresaHealthResponse health : atRisk) {
            log.info("  Cliente em risco: {} ({}) — concluídas {}→{} no período (queda de {}%, score {}).",
                    health.name(),
                    health.empresaId(),
                    health.completedPreviousPeriod(),
                    health.completedCurrentPeriod(),
                    formatPercent(health.dropPercent()),
                    health.healthScore());
        }
    }

    /** Converte a fração de queda (0.42) em percentual inteiro legível (42). Uso interno. */
    private static String formatPercent(Double dropPercent) {
        if (dropPercent == null) {
            return "?";
        }
        return String.valueOf(Math.round(dropPercent * 100));
    }
}
