package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;

import org.springframework.stereotype.Component;

import org.springframework.web.client.RestClient;


import java.net.URI;


@Component
public class RestClientResultWebhookClient implements ResultWebhookClient {

    private final RestClient restClient;
    private final GupyOutboundUrlValidator outboundUrlValidator;

    public RestClientResultWebhookClient(RestClient.Builder restClientBuilder, GupyOutboundUrlValidator outboundUrlValidator) {
        this.restClient = restClientBuilder.build();
        this.outboundUrlValidator = outboundUrlValidator;
    }

    @Override
    public void postResult(String webhookUrl, TestResultResponse testResultResponse) {
        postPayload(webhookUrl, testResultResponse);
    }

    @Override
    public void postPayload(String webhookUrl, Object payload) {
        URI uri = outboundUrlValidator.validate(webhookUrl);
        restClient.post()
                .uri(uri)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
