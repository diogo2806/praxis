package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestClientResultWebhookClientTest {

    @Test
    void allowsPublicWebhookHostWithoutStaticAllowList() {
        GupyOutboundUrlValidator validator = validator();

        assertThat(validator.validate("https://8.8.8.8/result").getHost()).isEqualTo("8.8.8.8");
    }

    @Test
    void validatesPersistenceWithoutResolvingExternalDns() {
        GupyOutboundUrlValidator validator = validator();

        assertThatCode(() -> validator.validateForPersistence(
                URI.create("https://host-que-nao-precisa-resolver.invalid/result")
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsupportedSchemeBeforePersistence() {
        GupyOutboundUrlValidator validator = validator();

        assertThatThrownBy(() -> validator.validateForPersistence(URI.create("ftp://example.com/result")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL externa deve usar HTTP ou HTTPS.");
    }

    @Test
    void rejectsPrivateLiteralAddressBeforePersistence() {
        GupyOutboundUrlValidator validator = validator();

        assertThatThrownBy(() -> validator.validateForPersistence(URI.create("https://127.0.0.1/result")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL externa não pode apontar para rede local ou reservada.");
    }

    @Test
    void rejectsLocalNetworkWebhookAtDeliveryTime() {
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
