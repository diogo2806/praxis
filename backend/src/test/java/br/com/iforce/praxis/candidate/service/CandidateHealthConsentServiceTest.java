package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.HealthVerticalService;
import br.com.iforce.praxis.candidate.dto.HealthConsentRequest;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateHealthConsentServiceTest {

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
                healthVerticalService
        );
    }

    @Test
    void persistsVersionedConsentBeforeAllowingHealthAssessment() {
        CandidateAttemptEntity attempt = resolveHealthAttempt("token-1", "att-1");

        service.register("token-1", new HealthConsentRequest("2026-06-01", false));

        assertThat(attempt.getHealthConsentRecordedAt()).isNotNull();
        assertThat(attempt.getHealthConsentVersion()).isEqualTo("2026-06-01");
        assertThat(attempt.getHealthConsentSubjectType()).isEqualTo("DATA_SUBJECT");
        assertThat(attempt.getHealthConsentRevokedAt()).isNull();
        verify(candidateAttemptRepository).save(attempt);
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("empresa-1"),
                eq("att-1"),
                eq(AuditEventType.HEALTH_CONSENT_RECORDED),
                anyString(),
                anyString()
        );
    }

    @Test
    void blocksHealthAssessmentWithoutPersistedConsent() {
        resolveHealthAttempt("token-2", "att-2");

        assertThatThrownBy(() -> service.assertConsentGranted("token-2"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("428");
    }

    @Test
    void allowsHealthAssessmentWithValidPersistedConsent() {
        CandidateAttemptEntity attempt = resolveHealthAttempt("token-3", "att-3");
        attempt.setHealthConsentRecordedAt(Instant.now());
        attempt.setHealthConsentVersion("2026-06-01");
        attempt.setHealthConsentSubjectType("DATA_SUBJECT");

        assertThatCode(() -> service.assertConsentGranted("token-3")).doesNotThrowAnyException();
    }

    @Test
    void doesNotRequireHealthConsentOutsideHealthVertical() {
        CandidateAttemptEntity attempt = attempt("att-4");
        when(tokenResolver.resolve("token-4"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-1", "att-4"));
        when(candidateAttemptRepository.findById("att-4")).thenReturn(Optional.of(attempt));
        when(healthVerticalService.isHealthVertical("empresa-1")).thenReturn(false);

        assertThatCode(() -> service.assertConsentGranted("token-4")).doesNotThrowAnyException();
        verifyNoInteractions(auditEventService);
    }

    private CandidateAttemptEntity resolveHealthAttempt(String token, String attemptId) {
        CandidateAttemptEntity attempt = attempt(attemptId);
        when(tokenResolver.resolve(token))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-1", attemptId));
        when(candidateAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(healthVerticalService.isHealthVertical("empresa-1")).thenReturn(true);
        return attempt;
    }

    private CandidateAttemptEntity attempt(String id) {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId(id);
        attempt.setEmpresaId("empresa-1");
        return attempt;
    }
}
