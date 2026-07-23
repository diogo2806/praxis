package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientResultWebhookClient implements ResultWebhookClient {

    private final GupyOutboundUrlValidator outboundUrlValidator;
    private final OutboundRestClientFactory restClientFactory;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public RestClientResultWebhookClient(
            GupyOutboundUrlValidator outboundUrlValidator,
            OutboundRestClientFactory restClientFactory,
            @Value("${praxis.gupy.callback.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${praxis.gupy.callback.read-timeout-ms:5000}") int readTimeoutMs
    ) {
        this.outboundUrlValidator = outboundUrlValidator;
        this.restClientFactory = restClientFactory;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Override
    public void postResult(String webhookUrl, TestResultResponse testResultResponse) {
        ValidatedOutboundTarget target = outboundUrlValidator.validateAndResolve(webhookUrl);
        RestClient client = restClientFactory.create(target, connectTimeoutMs, readTimeoutMs);
        client.post()
                .uri(target.uri())
                .body(testResultResponse)
                .retrieve()
                .onStatus(status -> status.is3xxRedirection(), (request, response) -> {
                    throw new IllegalStateException(
                            "Redirecionamento não permitido no envio de resultado: "
                                    + response.getStatusCode().value()
                    );
                })
                .toBodilessEntity();
    }

    @Override
    public int getCallback(String callbackUrl) {
        ValidatedOutboundTarget target = outboundUrlValidator.validateAndResolve(callbackUrl);
        RestClient client = restClientFactory.create(target, connectTimeoutMs, readTimeoutMs);
        ResponseEntity<Void> response = client.get()
                .uri(target.uri())
                .retrieve()
                .onStatus(status -> status.is3xxRedirection(), (request, redirectResponse) -> {
                    throw new CallbackHttpStatusException(redirectResponse.getStatusCode().value());
                })
                .toBodilessEntity();
        int statusCode = response.getStatusCode().value();
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new CallbackHttpStatusException(statusCode);
        }
        return statusCode;
    }
}
