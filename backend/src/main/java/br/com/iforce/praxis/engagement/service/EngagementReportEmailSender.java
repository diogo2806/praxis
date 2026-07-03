package br.com.iforce.praxis.engagement.service;

import br.com.iforce.praxis.engagement.dto.EngagementReportSummary;


/**
 * Entrega o relatório mensal de engajamento ao e-mail corporativo do cliente.
 *
 * <p>Segue o mesmo padrão de abstração de envio já usado na recuperação de senha: isola o fluxo
 * de retenção do mecanismo concreto de entrega. Enquanto não há provedor SMTP configurado, a
 * implementação padrão apenas registra o envio em log; quando um provedor real for adicionado,
 * basta fornecer outra implementação deste contrato.</p>
 */
public interface EngagementReportEmailSender {

    /**
     * Envia o relatório mensal de engajamento.
     *
     * @param recipientEmail e-mail corporativo do cliente (destinatário)
     * @param empresaName    nome do cliente, usado na saudação
     * @param summary        métricas agregadas do período, incluindo as horas economizadas
     */
    void sendMonthlyReport(String recipientEmail, String empresaName, EngagementReportSummary summary);
}
