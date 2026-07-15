package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestClientResultWebhookClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void allowsPublicWebhookHostWithoutStaticAllowList() {
        GupyOutboundUrlValidator validator = validator();

        assertThat(validator.validate("https://8.8.8.8/result").getHost()).isEqualTo("8.8.8.8");
    }

    @Test
    void rejectsLocalNetworkWebhook() {
        RestClientResultWebhookClient client = new RestClientResultWebhookClient(
                RestClient.builder(),
                validator(),
                1_000,
                1_000
        );

        assertThatThrownBy(() -> client.postResult("http://127.0.0.1/result", response()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL externa não pode apontar para rede local ou reservada.");
    }

    @Test
    void confirmsCallbackOnlyAfterReceiving2xx() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/callback", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        URI callbackUri = uri("/callback");
        GupyOutboundUrlValidator validator = mock(GupyOutboundUrlValidator.class);
        when(validator.validate(callbackUri.toString())).thenReturn(callbackUri);

        ResultWebhookClient client = new RestClientResultWebhookClient(
                RestClient.builder(),
                validator,
                1_000,
                1_000
        );

        assertThat(client.getCallback(callbackUri.toString())).isEqualTo(204);
    }

    @Test
    void doesNotFollowRedirectWithoutValidatingTheNewDestination() throws Exception {
        AtomicInteger redirectedRequests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> {
            redirectedRequests.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        URI callbackUri = uri("/redirect");
        GupyOutboundUrlValidator validator = mock(GupyOutboundUrlValidator.class);
        when(validator.validate(callbackUri.toString())).thenReturn(callbackUri);

        ResultWebhookClient client = new RestClientResultWebhookClient(
                RestClient.builder(),
                validator,
                1_000,
                1_000
        );

        assertThatThrownBy(() -> client.getCallback(callbackUri.toString()))
                .isInstanceOfSatisfying(
                        CallbackHttpStatusException.class,
                        exception -> assertThat(exception.statusCode()).isEqualTo(302)
                );
        assertThat(redirectedRequests).hasValue(0);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
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
