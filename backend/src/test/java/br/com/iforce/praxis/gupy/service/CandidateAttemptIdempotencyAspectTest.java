package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.integration.IntegrationEmpresaContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CandidateAttemptIdempotencyAspectTest {

    private final CandidateAttemptRepository repository = mock(CandidateAttemptRepository.class);
    private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    private final CandidateAttemptIdempotencyAspect aspect = new CandidateAttemptIdempotencyAspect(repository);
    private final IntegrationEmpresaContext context = new IntegrationEmpresaContext("empresa-1", "1", "gupy");

    @Test
    void shouldReuseWhenCanonicalRequestIsEquivalent() throws Throwable {
        CreateCandidateRequest request = request("Candidato Teste", "CANDIDATO@EXAMPLE.COM");
        CandidateAttemptEntity existing = legacySnapshot("Candidato Teste", "candidato@example.com");
        when(repository.findByEmpresaIdAndIdempotencyKey(anyString(), anyString()))
                .thenReturn(Optional.of(existing));
        when(joinPoint.proceed()).thenReturn("ok");

        Object response = aspect.enforceEquivalentRetry(joinPoint, request, context);

        assertThat(response).isEqualTo("ok");
        assertThat(existing.getRequestFingerprint()).hasSize(64);
        assertThat(existing.getRequestFingerprintVersion()).isEqualTo(1);
        verify(joinPoint).proceed();
        verify(repository).save(existing);
    }

    @Test
    void shouldReturnConflictWhenSameKeyHasDifferentContent() {
        CreateCandidateRequest request = request("Nome Alterado", "candidato@example.com");
        CandidateAttemptEntity existing = legacySnapshot("Candidato Teste", "candidato@example.com");
        when(repository.findByEmpresaIdAndIdempotencyKey(anyString(), anyString()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> aspect.enforceEquivalentRetry(joinPoint, request, context))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(joinPoint, never()).proceed();
        verify(repository, never()).save(existing);
    }

    private static CreateCandidateRequest request(String name, String email) {
        return new CreateCandidateRequest(
                1L, 4398157034L, "sim-atendimento-n2", name, email, 100L,
                URI.create("https://cliente.gupy.io/candidates/return"),
                URI.create("https://cliente.gupy.io/result-webhook"),
                new BigDecimal("1.50"), CreateCandidateRequest.CandidateType.EXTERNAL, null
        );
    }

    private static CandidateAttemptEntity legacySnapshot(String name, String email) {
        CandidateAttemptEntity entity = new CandidateAttemptEntity();
        entity.setEmpresaId("empresa-1");
        entity.setCompanyId("1");
        entity.setSimulationId("sim-atendimento-n2");
        entity.setCandidateName(name);
        entity.setCandidateEmail(email);
        entity.setGupyJobId(100L);
        entity.setCallbackUrl("https://cliente.gupy.io/candidates/return");
        entity.setResultWebhookUrl("https://cliente.gupy.io/result-webhook");
        entity.setAccommodationTimeMultiplier(new BigDecimal("1.5"));
        return entity;
    }
}