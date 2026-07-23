package br.com.iforce.praxis.integrity.service;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.integrity.dto.IntegrityEventRequest;
import br.com.iforce.praxis.integrity.model.IntegrityEventType;
import br.com.iforce.praxis.integrity.model.IntegrityInputMode;
import br.com.iforce.praxis.integrity.model.IntegritySessionStatus;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityEventEntity;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegritySessionEntity;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityEventRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegritySessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Primary
@Service
public class AtomicCandidateIntegrityService extends CandidateIntegrityService {

    private static final String LEGACY_ATTEMPT_PATTERN = "att_[A-Za-z0-9]{16,64}";
    private static final Set<String> ALLOWED_ASSET_DETAILS = Set.of("IMAGE", "AUDIO", "VIDEO", "OTHER");
    private static final EnumSet<IntegrityEventType> CLIENT_EVENTS = EnumSet.of(
            IntegrityEventType.TAB_HIDDEN,
            IntegrityEventType.TAB_VISIBLE,
            IntegrityEventType.INPUT_MODE_CHANGED,
            IntegrityEventType.NODE_PRESENTED,
            IntegrityEventType.ASSET_LOADED,
            IntegrityEventType.STIMULUS_STARTED,
            IntegrityEventType.VIDEO_PLAYBACK_STARTED,
            IntegrityEventType.VIDEO_PLAYBACK_PAUSED,
            IntegrityEventType.VIDEO_PLAYBACK_COMPLETED,
            IntegrityEventType.VIDEO_PLAYBACK_ERROR,
            IntegrityEventType.RESPONSE_SELECTED,
            IntegrityEventType.RESPONSE_CONFIRMED
    );

    private final CandidateIntegritySessionRepository sessionRepository;
    private final CandidateIntegrityEventRepository eventRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final JwtService jwtService;
    private final boolean securityEnabled;
    private final int sessionTimeoutSeconds;
    private final Clock clock;

    public AtomicCandidateIntegrityService(
            CandidateIntegritySessionRepository sessionRepository,
            CandidateIntegrityEventRepository eventRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            JwtService jwtService,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.integrity.hash-secret:}") String configuredHashSecret,
            @Value("${praxis.jwt-secret}") String jwtSecret,
            @Value("${praxis.integrity.heartbeat-seconds:30}") int heartbeatIntervalSeconds,
            @Value("${praxis.integrity.session-timeout-seconds:90}") int sessionTimeoutSeconds
    ) {
        super(
                sessionRepository,
                eventRepository,
                candidateAttemptRepository,
                jwtService,
                securityEnabled,
                configuredHashSecret,
                jwtSecret,
                heartbeatIntervalSeconds,
                sessionTimeoutSeconds
        );
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.jwtService = jwtService;
        this.securityEnabled = securityEnabled;
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional
    public void recordEvent(String attemptToken, IntegrityEventRequest request) {
        if (!CLIENT_EVENTS.contains(request.eventType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de evento técnico não permitido para o cliente.");
        }

        CandidateScope scope = resolveScope(attemptToken);
        CandidateIntegritySessionEntity session = requireSession(scope, request.sessionId());
        Instant now = clock.instant();
        requireActive(session, now);

        CandidateIntegrityEventEntity event = createEvent(session, request, now);
        if (request.sequenceNumber() != null && !eventRepository.insertIfAbsent(event)) {
            return;
        }

        IntegrityInputMode inputMode = request.inputMode();
        if (inputMode != null) {
            session.setInputMode(inputMode);
        }
        session.setLastHeartbeatAt(now);
        sessionRepository.save(session);

        if (request.sequenceNumber() == null) {
            eventRepository.save(event);
        }
    }

    private CandidateIntegrityEventEntity createEvent(
            CandidateIntegritySessionEntity session,
            IntegrityEventRequest request,
            Instant now
    ) {
        CandidateIntegrityEventEntity event = new CandidateIntegrityEventEntity();
        event.setEmpresaId(session.getEmpresaId());
        event.setCandidateAttemptId(session.getCandidateAttemptId());
        event.setSessionId(session.getId());
        event.setEventType(request.eventType());
        event.setOccurredAt(normalizeOccurredAt(request.occurredAt(), now));
        event.setReceivedAt(now);
        event.setInputMode(request.inputMode());
        event.setVisibilityState(visibilityFor(request.eventType()));
        event.setSequenceNumber(request.sequenceNumber());
        event.setDetail(normalizeDetail(request.eventType(), request.detail()));
        return event;
    }

    private CandidateScope resolveScope(String attemptToken) {
        try {
            JwtService.CandidateAttemptToken parsed = jwtService.parseCandidateAttemptToken(attemptToken);
            return new CandidateScope(parsed.empresaId(), parsed.attemptId());
        } catch (RuntimeException exception) {
            if (!securityEnabled && attemptToken != null && attemptToken.matches(LEGACY_ATTEMPT_PATTERN)) {
                CandidateAttemptEntity attempt = candidateAttemptRepository.findById(attemptToken)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));
                return new CandidateScope(attempt.getEmpresaId(), attempt.getId());
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token da tentativa inválido ou expirado.");
        }
    }

    private CandidateIntegritySessionEntity requireSession(CandidateScope scope, String sessionId) {
        return sessionRepository.findByIdAndCandidateAttemptId(sessionId, scope.attemptId())
                .filter(session -> session.getEmpresaId().equals(scope.empresaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão técnica não encontrada."));
    }

    private void requireActive(CandidateIntegritySessionEntity session, Instant now) {
        if (session.getStatus() != IntegritySessionStatus.ACTIVE
                || session.getLastHeartbeatAt().isBefore(now.minusSeconds(sessionTimeoutSeconds))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta sessão não está mais ativa. Recarregue a página para retomar a avaliação."
            );
        }
    }

    private Instant normalizeOccurredAt(Instant occurredAt, Instant now) {
        if (occurredAt == null
                || occurredAt.isBefore(now.minusSeconds(86_400))
                || occurredAt.isAfter(now.plusSeconds(86_400))) {
            return now;
        }
        return occurredAt;
    }

    private String visibilityFor(IntegrityEventType eventType) {
        return switch (eventType) {
            case TAB_HIDDEN -> "HIDDEN";
            case TAB_VISIBLE -> "VISIBLE";
            default -> null;
        };
    }

    private String normalizeDetail(IntegrityEventType eventType, String detail) {
        if (eventType != IntegrityEventType.ASSET_LOADED
                && eventType != IntegrityEventType.STIMULUS_STARTED
                && eventType != IntegrityEventType.VIDEO_PLAYBACK_STARTED
                && eventType != IntegrityEventType.VIDEO_PLAYBACK_PAUSED
                && eventType != IntegrityEventType.VIDEO_PLAYBACK_COMPLETED
                && eventType != IntegrityEventType.VIDEO_PLAYBACK_ERROR) {
            return null;
        }
        if (detail == null) {
            return "OTHER";
        }
        String normalized = detail.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ASSET_DETAILS.contains(normalized) ? normalized : "OTHER";
    }

    private record CandidateScope(String empresaId, String attemptId) {
    }
}
