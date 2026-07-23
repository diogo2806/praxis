package br.com.iforce.praxis.gupy.delivery.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ResultWebhookClientContractTest {

    @SuppressWarnings("removal")
    @Test
    void rejectsGenericPayloadInsteadOfDiscardingItSilently() {
        ResultWebhookClient client = new RestClientResultWebhookClient(
                mock(GupyOutboundUrlValidator.class),
                mock(OutboundRestClientFactory.class),
                1_000,
                1_000
        );

        assertThatThrownBy(() -> client.postPayload("https://cliente.gupy.io/result-webhook", new Object()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("result_webhook_url aceita exclusivamente o TestResult contratual da Gupy.");
    }
}
