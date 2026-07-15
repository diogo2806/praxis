package br.com.iforce.praxis.engagement.service;

import br.com.iforce.praxis.engagement.dto.EngagementReportSummary;


/**
 * Entrega o relatório mensal de engajamento ao e-mail corporativo do cliente.
 *
 * <p>O contrato recebe o período e, quando habilitada, a estimativa de tempo poupado acompanhada de
 * fórmula, hipótese, fonte metodológica e ressalva. Implementações concretas não devem apresentar a
 * estimativa como economia observada.</p>
 */
public interface EngagementReportEmailSender {

    /**
     * Envia o relatório mensal de engajamento.
     *
     * @param recipientEmail e-mail corporativo do cliente (destinatário)
     * @param empresaName    nome do cliente, usado na saudação
     * @param summary        métricas agregadas e metodologia da estimativa, quando habilitada
     */
    void sendMonthlyReport(String recipientEmail, String empresaName, EngagementReportSummary summary);
}
