package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;

/**
 * Interface padrao para adapters de ATS (Applicant Tracking Systems).
 * Apenas adapters com integracao real devem ser registrados como beans.
 */
public interface ATSAdapter {

    /**
     * Cria um candidato no ATS e retorna o contexto contendo ID, webhook URL, etc.
     */
    CandidateContext createCandidate(CreateCandidateCommand cmd);

    /**
     * Envia o resultado da avaliacao de volta para o ATS.
     */
    void pushResult(ResultPayload payload);

    /**
     * Identifica qual ATS este adapter implementa.
     */
    ATSPlatform type();

    enum ATSPlatform {
        GUPY
    }

    record CreateCandidateCommand(
        String tenantId,
        String candidateExternalId,
        String jobId,
        String evaluationName,
        String callbackWebhookUrl
    ) {}
}
