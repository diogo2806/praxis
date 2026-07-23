package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.HealthVerticalService;
import br.com.iforce.praxis.candidate.dto.HealthConsentRequest;
import br.com.iforce.praxis.candidate.dto.HealthConsentStatusResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateHealthConsentServiceTest {

    private static final String CURRENT_VERSION = "2026-06-01";

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private CandidateAttemptTokenResolver tokenResolver;
    @Mock
    private HealthVerticalService healthVerticalService;

    private CandidateHealthConsentService service;

    @BeforeEach
    void setUp() {
        service = new CandidateHealthConsentService(
                candidateAttemptRepository,
                auditEventService,
                new ObjectMapper(),
                tokenResolver,
                healthVerticalService,
                CURRENT_VERSION
        );
    }

    @Test
    void blocksHealthExecutionWithoutCurrentConsent() {
        CandidateAttemptEntity attempt = attempt();
        stubResolvedAttempt(attempt, true);

        assertThatThrownBy(() -> service.assertConsentGranted("token-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("428");
    }

    @Test
    void validConsentAllowsExecutionAndIsReturnedByStatus() {
        CandidateAttemptEntity attempt = attempt();
        setValidConsent(attempt);
        stubResolvedAttempt(attempt, true);

        assertThatCode(() -> service.assertConsentGranted("token-1")).doesNotThrowAnyException();
        HealthConsentStatusResponse status = service.getStatus("token-1");

        assertThat(status.required()).isTrue();
        assertThat(status.valid()).isTrue();
        assertThat(status.noticeVersion()).isEqualTo(CURRENT_VERSION);
    }

    @Test
    void newVersionRequiresNewConsent() {
        CandidateAttemptEntity attempt = attempt();
        setValidConsent(attempt);
        attempt.setHealthConsentVersion("2026-05-01");
        stubResolvedAttempt(attempt, true);

        assertThatThrownBy(() -> service.assertConsentGranted("token-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("428");
        assertThat(service.getStatus("token-1").valid()).isFalse();
    }

    @Test
    void registerIsIdempotentForCurrentVersion() {
        CandidateAttemptEntity attempt = attempt();
        stubResolvedAttempt(attempt, true);
        when(candidateAttemptRepository.save(any(CandidateAttemptEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HealthConsentRequest request = new HealthConsentRequest(CURRENT_VERSION, false);
        service.register("token-1", request);
        Instant firstRecordedAt = attempt.getHealthConsentRecordedAt();
        service.register("token-1", request);

        assertThat(attempt.getHealthConsentRecordedAt()).isEqualTo(firstRecordedAt);
        assertThat(attempt.getHealthConsentSource()).isEqualTo("CANDIDATE_PORTAL");
        verify(candidateAttemptRepository, times(1)).save(attempt);
        verify(auditEventService, times(1)).appendCandidateAttemptEvent(
                eq("empresa-1"), eq("attempt-1"), eq(AuditEventType.HEALTH_CONSENT_RECORDED),
                anyString(), anyString()
        );
    }

    @Test
    void rejectsOutdatedVersionAndUnvalidatedResponsible() {
        assertThatThrownBy(() -> service.register("token-1", new HealthConsentRequest(CURRENT_VERSION, true)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");

        CandidateAttemptEntity attempt = attempt();
        stubResolvedAttempt(attempt, true);
        assertThatThrownBy(() -> service.register("token-1", new HealthConsentRequest("2026-05-01", false)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(candidateAttemptRepository, never()).save(any());
    }

    @Test
    void revocationBlocksSubsequentExecution() {
        CandidateAttemptEntity attempt = attempt();
        setValidConsent(attempt);
        stubResolvedAttempt(attempt, true);
        when(candidateAttemptRepository.save(any(CandidateAttemptEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.revoke("token-1");

        assertThat(attempt.getHealthConsentRevokedAt()).isNotNull();
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("empresa-1"), eq("attempt-1"), eq(AuditEventType.HEALTH_CONSENT_REVOKED),
                anyString(), anyString()
        );
        assertThatThrownBy(() -> service.assertConsentGranted("token-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("428");
    }

    @Test
    void nonHealthAttemptIsNotAffected() {
        CandidateAttemptEntity attempt = attempt();
        stubResolvedAttempt(attempt, false);

        assertThatCode(() -> service.assertConsentGranted("token-1")).doesNotThrowAnyException();
        HealthConsentStatusResponse status = service.getStatus("token-1");
        assertThat(status.healthVertical()).isFalse();
        assertThat(status.required()).isFalse();
    }

    private void stubResolvedAttempt(CandidateAttemptEntity attempt, boolean healthVertical) {
        when(tokenResolver.resolve("token-1"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-1", "attempt-1"));
        when(candidateAttemptRepository.findById("attempt-1")).thenReturn(Optional.of(attempt));
        when(candidateAttemptRepository.findByEmpresaIdAndIdForUpdate("empresa-1", "attempt-1"))
                .thenReturn(Optional.of(attempt));
        when(healthVerticalService.isHealthVertical("empresa-1")).thenReturn(healthVertical);
    }

    private CandidateAttemptEntity attempt() {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("attempt-1");
        attempt.setEmpresaId("empresa-1");
        attempt.setStatus(AttemptStatus.NOT_STARTED);
        return attempt;
    }

    private void setValidConsent(CandidateAttemptEntity attempt) {
        attempt.setHealthConsentRecordedAt(Instant.now());
        attempt.setHealthConsentVersion(CURRENT_VERSION);
        attempt.setHealthConsentSubjectType("DATA_SUBJECT");
        attempt.setHealthConsentSource("CANDIDATE_PORTAL");
        attempt.setHealthConsentRevokedAt(null);
    }
}
