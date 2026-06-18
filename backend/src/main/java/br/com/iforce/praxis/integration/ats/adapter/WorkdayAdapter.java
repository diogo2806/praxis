package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter para integração com Workday.
 * Implementa a interface padrão ATSAdapter para permitir que o Praxis funcione
 * com Workday através da abstração de adapter.
 *
 * Status: Implementação base com stubs para métodos de integração.
 */
@Slf4j
@Component
public class WorkdayAdapter implements ATSAdapter {

    @Override
    public CandidateContext createCandidate(CreateCandidateCommand cmd) {
        log.info("Criando candidato no Workday: candidateId={}, jobId={}", cmd.candidateExternalId(), cmd.jobId());

        return new CandidateContext(
            cmd.candidateExternalId(),
            cmd.tenantId(),
            cmd.jobId(),
            cmd.evaluationName(),
            cmd.callbackWebhookUrl(),
            buildEvaluationLink(cmd)
        );
    }

    @Override
    public void pushResult(ResultPayload payload) {
        log.info("Enviando resultado para Workday: candidateId={}, score={}", payload.candidateId(), payload.score());

        try {
            // TODO: Implementar integração específica do Workday
            // Este método deverá:
            // 1. Transformar ResultPayload para formato esperado pelo Workday
            // 2. Chamar API/webhook do Workday com o resultado
            // 3. Gerenciar erros e retries conforme necessário
            log.warn("WorkdayAdapter.pushResult não está implementado. Payload: {}", payload);
        } catch (Exception e) {
            log.error("Falha ao enviar resultado para Workday: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ATSPlatform type() {
        return ATSPlatform.WORKDAY;
    }

    private String buildEvaluationLink(CreateCandidateCommand cmd) {
        return null;
    }
}
