package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter para integração com Catho.
 * Implementa a interface padrão ATSAdapter para permitir que o Praxis funcione
 * com Catho através da abstração de adapter.
 *
 * Catho é um dos maiores portais de recrutamento do Brasil.
 * Status: Implementação base com stubs para métodos de integração.
 */
@Slf4j
@Component
public class CathoAdapter implements ATSAdapter {

    @Override
    public CandidateContext createCandidate(CreateCandidateCommand cmd) {
        log.info("Criando candidato na Catho: candidateId={}, jobId={}", cmd.candidateExternalId(), cmd.jobId());

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
        log.info("Enviando resultado para Catho: candidateId={}, score={}", payload.candidateId(), payload.score());

        try {
            // TODO: Implementar integração específica da Catho
            // Este método deverá:
            // 1. Transformar ResultPayload para formato esperado pela Catho
            // 2. Chamar API/webhook da Catho com o resultado
            // 3. Gerenciar erros e retries conforme necessário
            log.warn("CathoAdapter.pushResult não está implementado. Payload: {}", payload);
        } catch (Exception e) {
            log.error("Falha ao enviar resultado para Catho: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ATSPlatform type() {
        return ATSPlatform.CATHO;
    }

    private String buildEvaluationLink(CreateCandidateCommand cmd) {
        return null;
    }
}
