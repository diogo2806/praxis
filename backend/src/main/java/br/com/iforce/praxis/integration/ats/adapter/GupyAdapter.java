package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.delivery.service.ResultWebhookClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter para integração com Gupy.
 * Implementa a interface padrão ATSAdapter para permitir que o Praxis funcione
 * com Gupy através da abstração de adapter.
 */
@Slf4j
@Component
public class GupyAdapter implements ATSAdapter {

    private final ResultWebhookClient resultWebhookClient;

    public GupyAdapter(ResultWebhookClient resultWebhookClient) {
        this.resultWebhookClient = resultWebhookClient;
    }

    @Override
    public CandidateContext createCandidate(CreateCandidateCommand cmd) {
        log.info("Criando candidato na Gupy: candidateId={}, jobId={}", cmd.candidateExternalId(), cmd.jobId());

        // Nota: A lógica de criação de candidato em Gupy já existe em GupyIntegrationController
        // Este adapter apenas padroniza a interface para outros ATSs também usarem
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
        log.info("Enviando resultado para Gupy: candidateId={}, score={}", payload.candidateId(), payload.score());

        TestResultResponse testResult = new TestResultResponse(
            payload.candidateId(),
            payload.simulationId(),
            payload.score(),
            payload.resultId(),
            payload.decision(),
            payload.humanReviewRequired(),
            payload.explanation(),
            null // additionalMetadata
        );

        try {
            resultWebhookClient.postResult(payload.resultId(), testResult);
        } catch (Exception e) {
            log.error("Falha ao enviar resultado para Gupy: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ATSPlatform type() {
        return ATSPlatform.GUPY;
    }

    private String buildEvaluationLink(CreateCandidateCommand cmd) {
        // Link do candidato será construído quando a tentativa for criada
        // Formato: /candidato/:token
        return null;
    }
}
