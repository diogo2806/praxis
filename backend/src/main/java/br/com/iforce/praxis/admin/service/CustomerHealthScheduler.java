package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.EmpresaHealthResponse;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;


import java.time.Instant;

import java.util.List;


/**
 * Vigia de retenção — roda todo dia e avisa o Customer Success sobre clientes em risco.
 *
 * <p>Na visão do processo, é a peça que institui a cultura proativa: sem ninguém precisar abrir o
 * painel, o sistema compara diariamente o uso recente de cada cliente ativo com o período anterior
 * (via {@link CustomerHealthService}) e, quando encontra quedas relevantes, dispara o alerta para
 * a equipe de sucesso do cliente intervir <em>antes</em> de o cliente decidir cancelar. Segue o
 * mesmo padrão dos demais agendamentos da plataforma e pode ser desligado por configuração.</p>
 */
@Component
@ConditionalOnProperty(name = "praxis.retention.customer-health-enabled", havingValue = "true", matchIfMissing = true)
public class CustomerHealthScheduler {

    private static final Logger log = LoggerFactory.getLogger(CustomerHealthScheduler.class);

    private final CustomerHealthService customerHealthService;
    private final CustomerSuccessAlertSender alertSender;
    private final String csTeamEmail;

    public CustomerHealthScheduler(
            CustomerHealthService customerHealthService,
            CustomerSuccessAlertSender alertSender,
            @Value("${praxis.retention.cs-team-email:}") String csTeamEmail
    ) {
        this.customerHealthService = customerHealthService;
        this.alertSender = alertSender;
        this.csTeamEmail = csTeamEmail;
    }

    /**
     * Varredura diária de saúde dos clientes: monta a fila de atuação e alerta o Customer Success.
     *
     * <p>Roda na frequência configurada ({@code praxis.retention.customer-health-cron}, por padrão
     * às 08:00). Se nenhum cliente estiver em risco, nada é enviado — o silêncio também é
     * informação. Falhas são registradas sem interromper o agendamento.</p>
     */
    @Scheduled(cron = "${praxis.retention.customer-health-cron:0 0 8 * * *}")
    public void alertCustomerSuccessAboutAtRiskClients() {
        try {
            List<EmpresaHealthResponse> atRisk = customerHealthService.atRiskEmpresas(Instant.now());
            if (atRisk.isEmpty()) {
                log.debug("Saúde do cliente: nenhum cliente ativo em risco nesta varredura.");
                return;
            }
            log.info("Saúde do cliente: {} cliente(s) ativo(s) em risco; alertando Customer Success.", atRisk.size());
            alertSender.sendAtRiskDigest(csTeamEmail, atRisk, Instant.now());
        } catch (RuntimeException exception) {
            log.warn("Falha ao executar a varredura de saúde do cliente: {}", exception.getMessage(), exception);
        }
    }
}
