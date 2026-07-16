package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Component
public class RestClientResultWebhookClient implements ResultWebhookClient {

    private final RestClient resultRestClient;
    private final GupyOutboundUrlValidator outboundUrlValidator;

    public RestClientResultWebhookClient(
            RestClient.Builder restClientBuilder,
            GupyOutboundUrlValidator outboundUrlValidator
    ) {
        this.resultRestClient = restClientBuilder.build();
        this.outboundUrlValidator = outboundUrlValidator;
    }

    @Override
    public void postResult(String webhookUrl, TestResultResponse testResultResponse) {
        URI uri = outboundUrlValidator.validate(webhookUrl);
        resultRestClient.post()
                .uri(uri)
                .body(testResultResponse)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void postPayload(String webhookUrl, Object payload) {
        // O result_webhook_url pertence ao contrato Gupy e aceita exclusivamente TestResult.
        // Eventos proprietários de engajamento não podem reutilizar esse destino.
    }
}
