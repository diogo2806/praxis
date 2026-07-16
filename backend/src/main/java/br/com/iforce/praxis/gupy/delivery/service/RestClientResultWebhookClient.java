package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

@Component
public class RestClientResultWebhookClient implements ResultWebhookClient {

    private final RestClient resultRestClient;
    private final RestClient callbackRestClient;
    private final GupyOutboundUrlValidator outboundUrlValidator;

    public RestClientResultWebhookClient(
            RestClient.Builder restClientBuilder,
            GupyOutboundUrlValidator outboundUrlValidator,
            @Value("${praxis.gupy.callback.connect-timeout-ms:3000}") int callbackConnectTimeoutMs,
            @Value("${praxis.gupy.callback.read-timeout-ms:5000}") int callbackReadTimeoutMs
    ) {
        this.resultRestClient = restClientBuilder.build();
        this.callbackRestClient = RestClient.builder()
                .requestFactory(callbackRequestFactory(callbackConnectTimeoutMs, callbackReadTimeoutMs))
                .build();
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

    @Override
    public int getCallback(String callbackUrl) {
        URI uri = outboundUrlValidator.validate(callbackUrl);
        ResponseEntity<Void> response = callbackRestClient.get()
                .uri(uri)
                .retrieve()
                .toBodilessEntity();
        int statusCode = response.getStatusCode().value();
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new CallbackHttpStatusException(statusCode);
        }
        return statusCode;
    }

    private SimpleClientHttpRequestFactory callbackRequestFactory(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                // Redirecionamentos não são seguidos: cada destino precisa passar pela validação SSRF.
                connection.setInstanceFollowRedirects(false);
            }
        };
        requestFactory.setConnectTimeout(Math.max(1, connectTimeoutMs));
        requestFactory.setReadTimeout(Math.max(1, readTimeoutMs));
        return requestFactory;
    }
}
