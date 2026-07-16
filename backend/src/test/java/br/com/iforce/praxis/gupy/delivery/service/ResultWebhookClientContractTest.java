package br.com.iforce.praxis.gupy.delivery.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ResultWebhookClientContractTest {

    @SuppressWarnings("removal")
    @Test
    void rejectsGenericPayloadInsteadOfDiscardingItSilently() {
        ResultWebhookClient client = new RestClientResultWebhookClient(
                RestClient.builder(),
                mock(GupyOutboundUrlValidator.class),
                1_000,
                1_000
        );

        assertThatThrownBy(() -> client.postPayload("https://cliente.gupy.io/result-webhook", new Object()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("result_webhook_url aceita exclusivamente o TestResult contratual da Gupy.");
    }
}
