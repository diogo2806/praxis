package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.billing.dto.DunningNotice;


/**
 * Entrega os toques educativos da régua de cobrança (retry) ao cliente por e-mail e/ou SMS.
 *
 * <p>Segue o mesmo padrão de abstração de envio das demais notificações da plataforma: isola a
 * decisão de "avisar o cliente que o pagamento falhou e ele deve tentar de novo" do meio concreto
 * de entrega. Enquanto não há provedor de e-mail/SMS configurado, a implementação padrão apenas
 * registra o toque em log; quando um provedor real for adicionado, basta fornecer outra
 * implementação deste contrato, sem alterar o fluxo de cobrança nem o webhook do Mercado Pago.</p>
 */
public interface BillingDunningNotificationSender {

    /**
     * Envia um toque educativo de cobrança ao cliente.
     *
     * @param notice destino, etapa da régua, situação financeira e prazo de carência do cliente
     */
    void sendRetryNotice(DunningNotice notice);
}
