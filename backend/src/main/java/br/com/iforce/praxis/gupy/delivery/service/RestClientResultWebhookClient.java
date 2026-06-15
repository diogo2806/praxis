package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientResultWebhookClient implements ResultWebhookClient {

    private final RestClient restClient;

    public RestClientResultWebhookClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public void postResult(String webhookUrl, TestResultResponse testResultResponse) {
        restClient.post()
                .uri(webhookUrl)
                .body(testResultResponse)
                .retrieve()
                .toBodilessEntity();
    }
}
