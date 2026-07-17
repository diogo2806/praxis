package br.com.iforce.praxis.gupy.security;

import br.com.iforce.praxis.gupy.delivery.service.GupyOutboundUrlValidator;
import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GupyCallbackRequestAdviceTest {

    @Test
    void rejectsInvalidResultWebhookBeforeControllerFlow() {
        GupyCallbackUrlPolicy callbackUrlPolicy = mock(GupyCallbackUrlPolicy.class);
        GupyCallbackRequestAdvice advice = new GupyCallbackRequestAdvice(
                callbackUrlPolicy,
                new GupyOutboundUrlValidator(true)
        );
        CreateCandidateRequest request = request(URI.create("ftp://example.com/result"));

        assertThatThrownBy(() -> advice.afterBodyRead(
                request,
                null,
                null,
                CreateCandidateRequest.class,
                null
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getReason()).isEqualTo("URL externa deve usar HTTP ou HTTPS.");
        });

        verify(callbackUrlPolicy).validate(request.callbackUrl());
    }

    @Test
    void acceptsPublicHttpsWebhookWithoutResolvingDns() {
        GupyCallbackUrlPolicy callbackUrlPolicy = mock(GupyCallbackUrlPolicy.class);
        GupyCallbackRequestAdvice advice = new GupyCallbackRequestAdvice(
                callbackUrlPolicy,
                new GupyOutboundUrlValidator(true)
        );
        CreateCandidateRequest request = request(
                URI.create("https://host-que-nao-precisa-resolver.invalid/result")
        );

        Object result = advice.afterBodyRead(
                request,
                null,
                null,
                CreateCandidateRequest.class,
                null
        );

        assertThat(result).isSameAs(request);
        verify(callbackUrlPolicy).validate(request.callbackUrl());
    }

    private CreateCandidateRequest request(URI resultWebhookUrl) {
        return new CreateCandidateRequest(
                1L,
                4398157034L,
                "sim-atendimento",
                "Candidato Teste",
                "candidato@example.com",
                100L,
                URI.create("https://cliente.gupy.io/candidate-return"),
                resultWebhookUrl,
                null,
                CreateCandidateRequest.CandidateType.EXTERNAL,
                null
        );
    }
}
