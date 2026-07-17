package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;

public interface ResultWebhookClient {

    void postResult(String webhookUrl, TestResultResponse testResultResponse);

    /**
     * Eventos proprietários não podem ser enviados ao {@code result_webhook_url} da Gupy.
     * O método permanece temporariamente para compatibilidade binária e falha explicitamente
     * caso algum fluxo antigo tente reutilizar esse destino.
     */
    @Deprecated(forRemoval = true)
    default void postPayload(String webhookUrl, Object payload) {
        throw new UnsupportedOperationException(
                "result_webhook_url aceita exclusivamente o TestResult contratual da Gupy."
        );
    }
}
