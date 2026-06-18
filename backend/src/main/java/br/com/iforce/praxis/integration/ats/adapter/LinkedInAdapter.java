package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter para integração com LinkedIn Recruiting.
 * Implementa a interface padrão ATSAdapter para permitir que o Praxis funcione
 * com LinkedIn Recruiting através da abstração de adapter.
 *
 * LinkedIn Recruiting oferece um ATS integrado com a maior rede profissional do mundo.
 * Status: Implementação base com stubs para métodos de integração.
 */
@Slf4j
@Component
public class LinkedInAdapter implements ATSAdapter {

    @Override
    public CandidateContext createCandidate(CreateCandidateCommand cmd) {
        log.info("Criando candidato no LinkedIn Recruiting: candidateId={}, jobId={}", cmd.candidateExternalId(), cmd.jobId());

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
        log.info("Enviando resultado para LinkedIn Recruiting: candidateId={}, score={}", payload.candidateId(), payload.score());

        try {
            // TODO: Implementar integração específica do LinkedIn Recruiting
            // Este método deverá:
            // 1. Transformar ResultPayload para formato esperado pelo LinkedIn
            // 2. Chamar LinkedIn Recruiting API com o resultado
            // 3. Gerenciar erros e retries conforme necessário
            // 4. Lidar com autenticação OAuth 2.0 do LinkedIn
            // 5. Considerar rate limits e quotas do LinkedIn
            log.warn("LinkedInAdapter.pushResult não está implementado. Payload: {}", payload);
        } catch (Exception e) {
            log.error("Falha ao enviar resultado para LinkedIn Recruiting: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ATSPlatform type() {
        return ATSPlatform.LINKEDIN;
    }

    private String buildEvaluationLink(CreateCandidateCommand cmd) {
        return null;
    }
}
