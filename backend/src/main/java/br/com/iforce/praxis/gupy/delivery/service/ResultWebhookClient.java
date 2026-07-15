package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;

public interface ResultWebhookClient {

    void postResult(String webhookUrl, TestResultResponse testResultResponse);

    void postPayload(String webhookUrl, Object payload);

    /**
     * Executa o callback contratual e devolve o status HTTP confirmado.
     *
     * @param callbackUrl URL fornecida pelo ATS
     * @return status HTTP 2xx devolvido pelo callback
     */
    int getCallback(String callbackUrl);
}
