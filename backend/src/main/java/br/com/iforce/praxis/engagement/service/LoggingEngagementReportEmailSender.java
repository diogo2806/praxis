package br.com.iforce.praxis.engagement.service;

import br.com.iforce.praxis.engagement.dto.EngagementReportSummary;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;


/**
 * Implementação padrão do envio do relatório mensal de engajamento.
 *
 * <p>Enquanto não há provedor SMTP configurado, o envio é registrado em log para permitir o fluxo
 * de retenção de ponta a ponta em desenvolvimento e homologação. O e-mail do destinatário é
 * mascarado no log.</p>
 *
 * <p>Mensagem enviada (assunto "Seu mês na Práxis"):</p>
 * <pre>
 * Olá, equipe {empresa}.
 * Neste mês você concluiu {n} avaliações comportamentais na Práxis,
 * o que representa cerca de {horas} horas economizadas em triagem manual.
 * Continue contando com a Práxis para decisões de contratação mais rápidas e defensáveis.
 * Equipe Práxis
 * </pre>
 */
@Component
public class LoggingEngagementReportEmailSender implements EngagementReportEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEngagementReportEmailSender.class);

    @Override
    public void sendMonthlyReport(String recipientEmail, String empresaName, EngagementReportSummary summary) {
        log.info(
                "Relatório mensal de engajamento (assunto: \"Seu mês na Práxis\") enviado para {} — {}: "
                        + "{} avaliações concluídas, ~{}h economizadas no período.",
                mask(recipientEmail),
                empresaName,
                summary.completedEvaluations(),
                formatHours(summary.hoursSaved())
        );
    }

    /** Formata as horas economizadas com uma casa decimal, sem depender do locale do servidor. Uso interno. */
    private static String formatHours(double hours) {
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
