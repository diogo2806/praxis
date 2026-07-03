package br.com.iforce.praxis.engagement.service;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;


import java.time.Instant;


/**
 * Dispara o envio automático dos relatórios mensais de engajamento.
 *
 * <p>Roda na frequência configurada ({@code praxis.engagement.report-cron}, por padrão às 07:00 do
 * primeiro dia de cada mês) e delega todo o cálculo e envio ao {@link EngagementReportService}.
 * Segue o mesmo padrão dos demais agendamentos da plataforma e pode ser desligado por
 * configuração ({@code praxis.engagement.report-enabled=false}).</p>
 */
@Component
@ConditionalOnProperty(name = "praxis.engagement.report-enabled", havingValue = "true", matchIfMissing = true)
public class EngagementReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(EngagementReportScheduler.class);

    private final EngagementReportService engagementReportService;

    public EngagementReportScheduler(EngagementReportService engagementReportService) {
        this.engagementReportService = engagementReportService;
    }

    @Scheduled(cron = "${praxis.engagement.report-cron:0 0 7 1 * *}")
    public void sendMonthlyEngagementReports() {
        try {
            int sent = engagementReportService.sendMonthlyReports(Instant.now());
            if (sent > 0) {
                log.info("Relatórios de engajamento: {} relatório(s) mensal(is) enviado(s).", sent);
            }
        } catch (RuntimeException exception) {
            log.warn("Falha ao enviar os relatórios mensais de engajamento: {}", exception.getMessage(), exception);
        }
    }
}
