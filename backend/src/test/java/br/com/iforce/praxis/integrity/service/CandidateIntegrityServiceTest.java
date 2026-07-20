package br.com.iforce.praxis.integrity.service;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.integrity.dto.CandidateIntegritySessionResponse;
import br.com.iforce.praxis.integrity.dto.StartIntegritySessionRequest;
import br.com.iforce.praxis.integrity.model.IntegrityInputMode;
import br.com.iforce.praxis.integrity.model.IntegritySessionStatus;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityEventEntity;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegritySessionEntity;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityEventRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegritySessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateIntegrityServiceTest {

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
    void opensSessionWithoutPersistingRawAddress() {
        prepareScope();
        when(sessionRepository.findFirstByCandidateAttemptIdAndStatusOrderByStartedAtDesc(
                "att-1",
                IntegritySessionStatus.ACTIVE
        )).thenReturn(Optional.empty());

        CandidateIntegritySessionResponse response = service().startSession(
                "token",
                request("browser-session-00000001"),
                "203.0.113.10",
                "Mozilla/5.0"
        );

        ArgumentCaptor<CandidateIntegritySessionEntity> sessionCaptor =
                ArgumentCaptor.forClass(CandidateIntegritySessionEntity.class);
        verify(sessionRepository).saveAndFlush(sessionCaptor.capture());
        CandidateIntegritySessionEntity saved = sessionCaptor.getValue();

        assertThat(response.resumed()).isFalse();
        assertThat(saved.getIpHash())
                .hasSize(64)
                .isNotEqualTo("203.0.113.10");
        assertThat(saved.getUserAgentCategory()).isEqualTo("DESKTOP_BROWSER");
        verify(eventRepository).save(any(CandidateIntegrityEventEntity.class));
    }

    @Test
    void resumesSameBrowserSession() {
        prepareScope();
        CandidateIntegritySessionEntity active = activeSession("browser-session-00000001", NOW.minusSeconds(10));
        when(sessionRepository.findFirstByCandidateAttemptIdAndStatusOrderByStartedAtDesc(
                "att-1",
                IntegritySessionStatus.ACTIVE
        )).thenReturn(Optional.of(active));

        CandidateIntegritySessionResponse response = service().startSession(
                "token",
                request("browser-session-00000001"),
                "203.0.113.10",
                "Mozilla/5.0"
        );

        assertThat(response.resumed()).isTrue();
        assertThat(response.sessionId()).isEqualTo(active.getId());
        assertThat(active.getLastHeartbeatAt()).isEqualTo(NOW);
        verify(sessionRepository, never()).saveAndFlush(any(CandidateIntegritySessionEntity.class));
    }

    @Test
    void blocksAnotherActiveBrowserSessionWithNeutralConflict() {
        prepareScope();
        CandidateIntegritySessionEntity active = activeSession("browser-session-00000001", NOW.minusSeconds(10));
        when(sessionRepository.findFirstByCandidateAttemptIdAndStatusOrderByStartedAtDesc(
                "att-1",
                IntegritySessionStatus.ACTIVE
        )).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service().startSession(
                "token",
                request("browser-session-00000002"),
                "203.0.113.11",
                "Mozilla/5.0"
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("outra sessão")
                .hasMessageNotContaining("fraude")
                .hasMessageNotContaining("suspeita");

        verify(sessionRepository, never()).saveAndFlush(any(CandidateIntegritySessionEntity.class));
    }

    @Test
    void expiresStaleSessionBeforeOpeningAnother() {
        prepareScope();
        CandidateIntegritySessionEntity stale = activeSession("browser-session-00000001", NOW.minusSeconds(91));
        when(sessionRepository.findFirstByCandidateAttemptIdAndStatusOrderByStartedAtDesc(
                "att-1",
                IntegritySessionStatus.ACTIVE
        )).thenReturn(Optional.of(stale));

        CandidateIntegritySessionResponse response = service().startSession(
                "token",
                request("browser-session-00000002"),
                "203.0.113.11",
                "Mozilla/5.0"
        );

        assertThat(stale.getStatus()).isEqualTo(IntegritySessionStatus.EXPIRED);
        assertThat(stale.getClosedAt()).isEqualTo(NOW);
        assertThat(response.resumed()).isFalse();
        verify(sessionRepository).save(stale);
        verify(sessionRepository).saveAndFlush(any(CandidateIntegritySessionEntity.class));
    }

    private CandidateIntegrityService service() {
        return new CandidateIntegrityService(
                sessionRepository,
                eventRepository,
                candidateAttemptRepository,
                jwtService,
                true,
                "test-integrity-secret-with-at-least-32-characters",
                30,
                90,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private void prepareScope() {
        when(jwtService.parseCandidateAttemptToken("token"))
                .thenReturn(new JwtService.CandidateAttemptToken("empresa-1", "att-1"));
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att-1");
        attempt.setEmpresaId("empresa-1");
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "att-1"))
                .thenReturn(Optional.of(attempt));
    }

    private StartIntegritySessionRequest request(String clientSessionId) {
        return new StartIntegritySessionRequest(clientSessionId, NOW, IntegrityInputMode.UNKNOWN);
    }

    private CandidateIntegritySessionEntity activeSession(String clientSessionId, Instant heartbeatAt) {
        CandidateIntegritySessionEntity session = new CandidateIntegritySessionEntity();
        session.setId("6e49bb74-5e1d-4f24-894f-1a3ea25700d1");
        session.setEmpresaId("empresa-1");
        session.setCandidateAttemptId("att-1");
        session.setClientSessionId(clientSessionId);
        session.setStatus(IntegritySessionStatus.ACTIVE);
        session.setStartedAt(NOW.minusSeconds(60));
        session.setLastHeartbeatAt(heartbeatAt);
        session.setInputMode(IntegrityInputMode.UNKNOWN);
        session.setUserAgentCategory("DESKTOP_BROWSER");
        return session;
    }
}
