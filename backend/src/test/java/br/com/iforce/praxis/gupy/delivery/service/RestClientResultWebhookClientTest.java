package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
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
                validator(),
                new OutboundRestClientFactory(),
                1_000,
                1_000
        );

        assertThatThrownBy(() -> client.postResult("http://127.0.0.1/result", response()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL externa não pode apontar para rede local ou reservada.");
    }

    @Test
    void rejectsUserInfoAndHttpWhenSecurityIsEnabled() {
        GupyOutboundUrlValidator validator = new GupyOutboundUrlValidator(true);

        assertThatThrownBy(() -> validator.validate("https://user:pass@8.8.8.8/result"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL externa inválida.");
        assertThatThrownBy(() -> validator.validate("http://8.8.8.8/result"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL externa deve usar HTTPS em produção.");
    }

    @Test
    void pinsTheAddressResolvedDuringValidation() throws Exception {
        AtomicInteger resolutions = new AtomicInteger();
        GupyOutboundUrlValidator validator = new GupyOutboundUrlValidator(false, host -> {
            if (resolutions.getAndIncrement() == 0) {
                return new InetAddress[]{InetAddress.getByName("8.8.8.8")};
            }
            return new InetAddress[]{InetAddress.getByName("127.0.0.1")};
        });

        ValidatedOutboundTarget target = validator.validateAndResolve("http://example.test/result");

        assertThat(target.addresses()).containsExactly(InetAddress.getByName("8.8.8.8"));
        assertThat(resolutions).hasValue(1);
    }

    @Test
    void doesNotFollowRedirectForCallback() throws Exception {
        AtomicInteger redirectedRequests = new AtomicInteger();
        server = redirectServer(redirectedRequests);
        URI callbackUri = uri("/redirect");
        RestClientResultWebhookClient client = clientForLocalTarget(callbackUri);

        assertThatThrownBy(() -> client.getCallback(callbackUri.toString()))
                .isInstanceOfSatisfying(
                        CallbackHttpStatusException.class,
                        exception -> assertThat(exception.statusCode()).isEqualTo(302)
                );
        assertThat(redirectedRequests).hasValue(0);
    }

    @Test
    void doesNotFollowRedirectForResultPost() throws Exception {
        AtomicInteger redirectedRequests = new AtomicInteger();
        server = redirectServer(redirectedRequests);
        URI webhookUri = uri("/redirect");
        RestClientResultWebhookClient client = clientForLocalTarget(webhookUri);

        assertThatThrownBy(() -> client.postResult(webhookUri.toString(), response()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redirecionamento não permitido");
        assertThat(redirectedRequests).hasValue(0);
    }

    private RestClientResultWebhookClient clientForLocalTarget(URI uri) throws Exception {
        GupyOutboundUrlValidator validator = mock(GupyOutboundUrlValidator.class);
        ValidatedOutboundTarget target = new ValidatedOutboundTarget(
                uri,
                new InetAddress[]{InetAddress.getByName("127.0.0.1")}
        );
        when(validator.validateAndResolve(uri.toString())).thenReturn(target);
        return new RestClientResultWebhookClient(
                validator,
                new OutboundRestClientFactory(),
                1_000,
                1_000
        );
    }

    private HttpServer redirectServer(AtomicInteger redirectedRequests) throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        httpServer.createContext("/target", exchange -> {
            redirectedRequests.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        httpServer.start();
        return httpServer;
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.getAddress().getPort() + path);
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
