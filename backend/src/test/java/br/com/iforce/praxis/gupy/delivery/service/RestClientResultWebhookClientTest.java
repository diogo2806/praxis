package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;

import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestClientResultWebhookClientTest {

    @Test
    void allowsPublicWebhookHostWithoutStaticAllowList() {
        GupyOutboundUrlValidator validator = validator();

        assertThat(validator.validate("https://8.8.8.8/result").getHost()).isEqualTo("8.8.8.8");
    }

    @Test
    void rejectsLocalNetworkWebhook() {
        RestClientResultWebhookClient client = new RestClientResultWebhookClient(
                RestClient.builder(),
                validator()
        );

        assertThatThrownBy(() -> client.postResult("http://127.0.0.1/result", response()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL externa não pode apontar para rede local ou reservada.");
    }

    private GupyOutboundUrlValidator validator() {
        return new GupyOutboundUrlValidator(false);
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
