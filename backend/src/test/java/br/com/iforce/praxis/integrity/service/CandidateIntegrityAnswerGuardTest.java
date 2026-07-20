package br.com.iforce.praxis.integrity.service;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.integrity.model.IntegrityInputMode;
import br.com.iforce.praxis.integrity.model.IntegritySessionStatus;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegritySessionEntity;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityEventRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegritySessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateIntegrityAnswerGuardTest {

    private static final Instant NOW = Instant.parse("2026-07-20T20:00:00Z");

    @Mock
    private CandidateIntegritySessionRepository sessionRepository;

    @Mock
    private CandidateIntegrityEventRepository eventRepository;

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    @Mock
    private JwtService jwtService;

    @Test
    void rejectsAnswerWithoutSessionWhenSecurityIsEnabled() {
        assertThatThrownBy(() -> service(true).requireActiveSession("token", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("sessão segura");
    }

    @Test
    void acceptsAnswerFromActiveSessionOwnedByAttempt() {
        when(jwtService.parseCandidateAttemptToken("token"))
                .thenReturn(new JwtService.CandidateAttemptToken("empresa-1", "att-1"));
        CandidateIntegritySessionEntity session = activeSession();
        when(sessionRepository.findByIdAndCandidateAttemptId(session.getId(), "att-1"))
                .thenReturn(Optional.of(session));

        assertThatCode(() -> service(true).requireActiveSession("token", session.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void keepsLegacyIntegrationTestsCompatibleWhenSecurityIsDisabled() {
        assertThatCode(() -> service(false).requireActiveSession("att_1234567890abcdef", null))
                .doesNotThrowAnyException();
    }

    private CandidateIntegrityService service(boolean securityEnabled) {
        return new CandidateIntegrityService(
                sessionRepository,
                eventRepository,
                candidateAttemptRepository,
                jwtService,
                securityEnabled,
                "test-integrity-secret-with-at-least-32-characters",
                30,
                90,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private CandidateIntegritySessionEntity activeSession() {
        CandidateIntegritySessionEntity session = new CandidateIntegritySessionEntity();
        session.setId("6e49bb74-5e1d-4f24-894f-1a3ea25700d1");
        session.setEmpresaId("empresa-1");
        session.setCandidateAttemptId("att-1");
        session.setClientSessionId("browser-session-00000001");
        session.setStatus(IntegritySessionStatus.ACTIVE);
        session.setStartedAt(NOW.minusSeconds(60));
        session.setLastHeartbeatAt(NOW.minusSeconds(10));
        session.setInputMode(IntegrityInputMode.UNKNOWN);
        session.setUserAgentCategory("DESKTOP_BROWSER");
        return session;
    }
}
