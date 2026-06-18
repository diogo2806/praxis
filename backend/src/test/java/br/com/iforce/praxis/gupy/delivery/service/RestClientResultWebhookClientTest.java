package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestClientResultWebhookClientTest {

    @Test
    void rejectsWebhookHostOutsideAllowList() {
        RestClientResultWebhookClient client = new RestClientResultWebhookClient(
                RestClient.builder(),
                validator(List.of("cliente.gupy.io"))
        );

        assertThatThrownBy(() -> client.postResult("https://evil.example/result", response()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Host externo nao permitido.");
    }

    @Test
    void rejectsLocalNetworkWebhookEvenWhenHostIsAllowed() {
        RestClientResultWebhookClient client = new RestClientResultWebhookClient(
                RestClient.builder(),
                validator(List.of("127.0.0.1"))
        );

        assertThatThrownBy(() -> client.postResult("http://127.0.0.1/result", response()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL externa nao pode apontar para rede local ou reservada.");
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

    private TestResultResponse response() {
        return new TestResultResponse(
                "Titulo",
                "teste",
                "Descricao",
                "Praxis",
                "Resultado",
                "http://localhost:8080",
                "done",
                "http://localhost:8080/test/result/res_1?company_id=empresa-123",
                "http://localhost:8080/candidate/attempts/att_1",
                List.of()
        );
    }
}
