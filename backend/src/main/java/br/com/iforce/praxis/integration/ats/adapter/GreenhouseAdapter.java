package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter para integração com Greenhouse.
 * Implementa a interface padrão ATSAdapter para permitir que o Praxis funcione
 * com Greenhouse através da abstração de adapter.
 *
 * Status: Implementação base com stubs para métodos de integração.
 */
@Slf4j
@Component
public class GreenhouseAdapter implements ATSAdapter {

    @Override
    public CandidateContext createCandidate(CreateCandidateCommand cmd) {
        log.info("Criando candidato no Greenhouse: candidateId={}, jobId={}", cmd.candidateExternalId(), cmd.jobId());

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
        log.info("Enviando resultado para Greenhouse: candidateId={}, score={}", payload.candidateId(), payload.score());

        try {
            // TODO: Implementar integração específica do Greenhouse
            // Este método deverá:
            // 1. Transformar ResultPayload para formato esperado pelo Greenhouse
            // 2. Chamar API/webhook do Greenhouse com o resultado
            // 3. Gerenciar erros e retries conforme necessário
            log.warn("GreenhouseAdapter.pushResult não está implementado. Payload: {}", payload);
        } catch (Exception e) {
            log.error("Falha ao enviar resultado para Greenhouse: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ATSPlatform type() {
        return ATSPlatform.GREENHOUSE;
    }

    private String buildEvaluationLink(CreateCandidateCommand cmd) {
        return null;
    }
}
