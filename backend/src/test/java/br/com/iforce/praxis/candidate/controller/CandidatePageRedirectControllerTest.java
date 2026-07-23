package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.config.PraxisProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidatePageRedirectControllerTest {

    @Test
    void redirectsCandidateToConfiguredFrontendPreservingBasePath() {
        PraxisProperties properties = properties(
                "https://api.example.com",
                "https://candidate.example.com/app/"
        );
        CandidatePageRedirectController controller = new CandidatePageRedirectController(properties);
        MockHttpServletRequest request = request("api.example.com", "/candidato/token-123");

        ResponseEntity<Void> response = controller.redirectCandidatePage("token-123", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://candidate.example.com/app/candidato/token-123"));
    }

    @Test
    void rejectsRedirectWhenCandidatePageUsesCurrentDomain() {
        PraxisProperties properties = properties(
                "https://candidate.example.com",
                "https://candidate.example.com/"
        );
        CandidatePageRedirectController controller = new CandidatePageRedirectController(properties);
        MockHttpServletRequest request = request("candidate.example.com", "/candidato/token-123");

        assertThatThrownBy(() -> controller.redirectCandidatePage("token-123", request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static MockHttpServletRequest request(String serverName, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName(serverName);
        request.setServerPort(443);
        request.setRequestURI(requestUri);
        return request;
    }

    private static PraxisProperties properties(String publicBaseUrl, String candidatePageBaseUrl) {
        return new PraxisProperties(
                publicBaseUrl,
                candidatePageBaseUrl,
                168,
                24,
                720,
                70,
                15,
                0.001
        );
    }
}
