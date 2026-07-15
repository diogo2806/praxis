package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
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
        CreateCandidateRequest request = request("Candidato Teste", "CANDIDATO@EXAMPLE.COM", null);
        CandidateAttemptEntity existing = legacySnapshot("Candidato Teste", "candidato@example.com");
        when(repository.findByEmpresaIdAndIdempotencyKey(anyString(), anyString()))
                .thenReturn(Optional.of(existing));
        when(joinPoint.proceed()).thenReturn("ok");

        Object response = aspect.enforceEquivalentRetry(joinPoint, request, context);

        assertThat(response).isEqualTo("ok");
        assertThat(existing.getRequestFingerprint()).hasSize(64);
        assertThat(existing.getRequestFingerprintVersion()).isEqualTo(2);
        verify(joinPoint).proceed();
        verify(repository).save(existing);
    }

    @Test
    void shouldReturnConflictWhenSameKeyHasDifferentContent() {
        CreateCandidateRequest request = request("Nome Alterado", "candidato@example.com", null);
        CandidateAttemptEntity existing = legacySnapshot("Candidato Teste", "candidato@example.com");
        when(repository.findByEmpresaIdAndIdempotencyKey(anyString(), anyString()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> aspect.enforceEquivalentRetry(joinPoint, request, context))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(joinPoint, never()).proceed();
        verify(repository, never()).save(existing);
    }

    @Test
    void shouldCreateRetestAfterTerminalAttempt() throws Throwable {
        CreateCandidateRequest request = request("Candidato Teste", "candidato@example.com",
                CreateCandidateRequest.PreviousResult.FAIL);
        String initialKey = CandidateAttemptIdempotencyKeyFactory.initialKey(request, context);
        String retestKey = CandidateAttemptIdempotencyKeyFactory.currentKey(request, context);
        CandidateAttemptEntity previous = legacySnapshot("Candidato Teste", "candidato@example.com");
        previous.setStatus(AttemptStatus.COMPLETED);
        CandidateAttemptEntity saved = legacySnapshot("Candidato Teste", "candidato@example.com");
        saved.setStatus(AttemptStatus.NOT_STARTED);

        when(repository.findByEmpresaIdAndIdempotencyKey("empresa-1", retestKey))
                .thenReturn(Optional.empty(), Optional.of(saved));
        when(repository.findByEmpresaIdAndIdempotencyKey("empresa-1", initialKey))
                .thenReturn(Optional.of(previous));
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            assertThat(IdempotencyKeyHasher.sha256Hex(
                    CandidateAttemptIdempotencyKeyFactory.initialSource(request, context)))
                    .isEqualTo(retestKey);
            return "ok";
        });

        Object response = aspect.enforceEquivalentRetry(joinPoint, request, context);

        assertThat(response).isEqualTo("ok");
        assertThat(saved.getRequestFingerprintVersion()).isEqualTo(2);
        assertThat(IdempotencyKeyHasher.sha256Hex(
                CandidateAttemptIdempotencyKeyFactory.initialSource(request, context)))
                .isEqualTo(initialKey);
        verify(joinPoint).proceed();
        verify(repository).save(saved);
    }

    @Test
    void shouldReuseEquivalentRetestInsteadOfCreatingAnotherCycle() throws Throwable {
        CreateCandidateRequest request = request("Candidato Teste", "candidato@example.com",
                CreateCandidateRequest.PreviousResult.FAIL);
        CandidateAttemptEntity existingRetest = legacySnapshot("Candidato Teste", "candidato@example.com");
        existingRetest.setStatus(AttemptStatus.IN_PROGRESS);
        when(repository.findByEmpresaIdAndIdempotencyKey(anyString(), anyString()))
                .thenReturn(Optional.of(existingRetest));
        when(joinPoint.proceed()).thenReturn("ok");

        Object response = aspect.enforceEquivalentRetry(joinPoint, request, context);

        assertThat(response).isEqualTo("ok");
        verify(joinPoint).proceed();
        verify(repository).save(existingRetest);
    }

    @Test
    void shouldRejectRetestWithoutPreviousAttempt() {
        CreateCandidateRequest request = request("Candidato Teste", "candidato@example.com",
                CreateCandidateRequest.PreviousResult.FAIL);
        when(repository.findByEmpresaIdAndIdempotencyKey(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> aspect.enforceEquivalentRetry(joinPoint, request, context))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).contains("exige uma tentativa anterior");
                });

        verify(joinPoint, never()).proceed();
    }

    @Test
    void shouldRejectRetestWhilePreviousAttemptIsActive() {
        CreateCandidateRequest request = request("Candidato Teste", "candidato@example.com",
                CreateCandidateRequest.PreviousResult.FAIL);
        String initialKey = CandidateAttemptIdempotencyKeyFactory.initialKey(request, context);
        String retestKey = CandidateAttemptIdempotencyKeyFactory.currentKey(request, context);
        CandidateAttemptEntity previous = legacySnapshot("Candidato Teste", "candidato@example.com");
        previous.setStatus(AttemptStatus.IN_PROGRESS);
        when(repository.findByEmpresaIdAndIdempotencyKey("empresa-1", retestKey))
                .thenReturn(Optional.empty());
        when(repository.findByEmpresaIdAndIdempotencyKey("empresa-1", initialKey))
                .thenReturn(Optional.of(previous));

        assertThatThrownBy(() -> aspect.enforceEquivalentRetry(joinPoint, request, context))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).contains("concluída, abandonada ou expirada");
                });

        verify(joinPoint, never()).proceed();
    }

    @Test
    void shouldKeepInitialAndRetestKeysDistinctAndStable() {
        CreateCandidateRequest initial = request("Candidato Teste", "candidato@example.com", null);
        CreateCandidateRequest retest = request("Candidato Teste", "candidato@example.com",
                CreateCandidateRequest.PreviousResult.FAIL);

        assertThat(CandidateAttemptIdempotencyKeyFactory.currentKey(initial, context))
                .isEqualTo(CandidateAttemptIdempotencyKeyFactory.initialKey(initial, context));
        assertThat(CandidateAttemptIdempotencyKeyFactory.currentKey(retest, context))
                .isNotEqualTo(CandidateAttemptIdempotencyKeyFactory.initialKey(retest, context));
        assertThat(CandidateAttemptIdempotencyKeyFactory.currentKey(retest, context))
                .isEqualTo(CandidateAttemptIdempotencyKeyFactory.currentKey(retest, context));
    }

    private static CreateCandidateRequest request(String name, String email,
                                                   CreateCandidateRequest.PreviousResult previousResult) {
        return new CreateCandidateRequest(
                1L, 4398157034L, "sim-atendimento-n2", name, email, 100L,
                URI.create("https://cliente.gupy.io/candidates/return"),
                URI.create("https://cliente.gupy.io/result-webhook"),
                new BigDecimal("1.50"), CreateCandidateRequest.CandidateType.EXTERNAL, previousResult
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
