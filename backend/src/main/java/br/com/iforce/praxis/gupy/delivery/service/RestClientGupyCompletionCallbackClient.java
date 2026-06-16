package br.com.iforce.praxis.gupy.delivery.service;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Component
public class RestClientGupyCompletionCallbackClient implements GupyCompletionCallbackClient {

    private final RestClient restClient;
    private final GupyOutboundUrlValidator outboundUrlValidator;

    public RestClientGupyCompletionCallbackClient(
            RestClient.Builder restClientBuilder,
            GupyOutboundUrlValidator outboundUrlValidator
    ) {
        this.restClient = restClientBuilder.build();
        this.outboundUrlValidator = outboundUrlValidator;
    }

    @Override
    public void notifyCompletion(String callbackUrl) {
        URI uri = outboundUrlValidator.validate(callbackUrl);
        restClient.get()
                .uri(uri)
                .retrieve()
                .toBodilessEntity();
    }
}
