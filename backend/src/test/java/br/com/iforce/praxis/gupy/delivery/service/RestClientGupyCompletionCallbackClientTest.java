package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.config.PraxisProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestClientGupyCompletionCallbackClientTest {

    @Test
    void rejectsCallbackHostOutsideAllowList() {
        RestClientGupyCompletionCallbackClient client = new RestClientGupyCompletionCallbackClient(
                RestClient.builder(),
                validator(List.of("cliente.gupy.io"))
        );

        assertThatThrownBy(() -> client.notifyCompletion("https://evil.example/callback"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Host externo nao permitido.");
    }

    private GupyOutboundUrlValidator validator(List<String> webhookAllowedHosts) {
        PraxisProperties properties = new PraxisProperties(
                "http://localhost:8080",
                "token",
                168,
                24,
                70,
                0.001,
                100,
                30,
                10,
                webhookAllowedHosts
        );
        return new GupyOutboundUrlValidator(properties, false);
    }
}
