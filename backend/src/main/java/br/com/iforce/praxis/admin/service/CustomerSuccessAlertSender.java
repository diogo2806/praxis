package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.EmpresaHealthResponse;


import java.time.Instant;

import java.util.List;


/**
 * Entrega o alerta diário da fila de atuação para a equipe de Customer Success.
 *
 * <p>A abstração isola a decisão de "avisar o CS que há clientes em risco" do meio concreto de
 * entrega. Enquanto a plataforma não tem um provedor de e-mail/mensageria configurado, a
 * implementação padrão apenas registra o alerta em log; quando um provedor real for adicionado,
 * basta fornecer outra implementação deste contrato, sem alterar o agendamento nem o cálculo de
 * saúde do cliente.</p>
 */
public interface CustomerSuccessAlertSender {

    /**
     * Dispara o alerta com os clientes em risco para o time de Customer Success intervir.
     *
     * @param csTeamEmail e-mail (ou canal) do time de Customer Success; pode ser vazio se não configurado
     * @param atRisk      clientes ativos com queda de uso acima do limite, do mais crítico ao menos
     * @param generatedAt instante em que o alerta foi gerado
     */
    void sendAtRiskDigest(String csTeamEmail, List<EmpresaHealthResponse> atRisk, Instant generatedAt);
}
