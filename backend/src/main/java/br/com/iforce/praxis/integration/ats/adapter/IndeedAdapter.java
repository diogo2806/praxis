package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter para integração com Indeed.
 * Implementa a interface padrão ATSAdapter para permitir que o Praxis funcione
 * com Indeed através da abstração de adapter.
 *
 * Indeed é um dos maiores portais de emprego globais com forte presença no Brasil.
 * Status: Implementação base com stubs para métodos de integração.
 */
@Slf4j
@Component
public class IndeedAdapter implements ATSAdapter {

    @Override
    public CandidateContext createCandidate(CreateCandidateCommand cmd) {
        log.info("Criando candidato no Indeed: candidateId={}, jobId={}", cmd.candidateExternalId(), cmd.jobId());

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
        log.info("Enviando resultado para Indeed: candidateId={}, score={}", payload.candidateId(), payload.score());

        try {
            // TODO: Implementar integração específica do Indeed
            // Este método deverá:
            // 1. Transformar ResultPayload para formato esperado pelo Indeed
            // 2. Chamar API/webhook do Indeed com o resultado
            // 3. Gerenciar erros e retries conforme necessário
            // 4. Considerar rate limits do Indeed API
            log.warn("IndeedAdapter.pushResult não está implementado. Payload: {}", payload);
        } catch (Exception e) {
            log.error("Falha ao enviar resultado para Indeed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ATSPlatform type() {
        return ATSPlatform.INDEED;
    }

    private String buildEvaluationLink(CreateCandidateCommand cmd) {
        return null;
    }
}
