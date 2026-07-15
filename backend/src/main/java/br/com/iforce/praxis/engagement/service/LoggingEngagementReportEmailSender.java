package br.com.iforce.praxis.engagement.service;

import br.com.iforce.praxis.engagement.dto.EngagementReportSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Implementação padrão do envio do relatório mensal de engajamento.
 *
 * <p>Enquanto não há provedor SMTP configurado, o conteúdo é registrado em log para permitir o
 * fluxo de ponta a ponta em desenvolvimento e homologação. O e-mail do destinatário é mascarado.</p>
 */
@Component
public class LoggingEngagementReportEmailSender implements EngagementReportEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEngagementReportEmailSender.class);

    @Override
    public void sendMonthlyReport(String recipientEmail, String empresaName, EngagementReportSummary summary) {
        if (!summary.timeSavingEstimateEnabled()) {
            log.info(
                    "Relatório mensal de engajamento (assunto: \"Seu mês na Práxis\") enviado para {} — {}: "
                            + "período {} a {}, {} avaliações concluídas; estimativa de tempo poupado desativada.",
                    mask(recipientEmail),
                    empresaName,
                    summary.periodStart(),
                    summary.periodEnd(),
                    summary.completedEvaluations()
            );
            return;
        }

        log.info(
                "Relatório mensal de engajamento (assunto: \"Seu mês na Práxis\") enviado para {} — {}: "
                        + "período {} a {}, {} avaliações concluídas; estimativa de ~{}h potencialmente poupadas "
                        + "(fórmula: {}; parâmetro: {}h por avaliação concluída; fonte metodológica: {}). {}",
                mask(recipientEmail),
                empresaName,
                summary.periodStart(),
                summary.periodEnd(),
                summary.completedEvaluations(),
                formatHours(summary.estimatedHoursSaved()),
                summary.estimationFormula(),
                formatHours(summary.assumedHoursPerCompletedEvaluation()),
                summary.estimationMethodologySource(),
                summary.estimationCaveat()
        );
    }

    /** Formata horas com uma casa decimal, sem depender do locale do servidor. */
    private static String formatHours(Double hours) {
        if (hours == null) {
            return "n/a";
        }
        return String.valueOf(Math.round(hours * 10) / 10.0);
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
