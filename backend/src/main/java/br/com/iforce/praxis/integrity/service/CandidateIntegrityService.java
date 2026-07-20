package br.com.iforce.praxis.integrity.service;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.integrity.dto.CandidateIntegritySessionResponse;
import br.com.iforce.praxis.integrity.dto.CloseIntegritySessionRequest;
import br.com.iforce.praxis.integrity.dto.IntegrityEventRequest;
import br.com.iforce.praxis.integrity.dto.IntegrityHeartbeatRequest;
import br.com.iforce.praxis.integrity.dto.StartIntegritySessionRequest;
import br.com.iforce.praxis.integrity.model.IntegrityEventType;
import br.com.iforce.praxis.integrity.model.IntegrityInputMode;
import br.com.iforce.praxis.integrity.model.IntegritySessionStatus;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityEventEntity;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegritySessionEntity;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityEventRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegritySessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CandidateIntegrityService {

    private static final String LEGACY_ATTEMPT_PATTERN = "att_[A-Za-z0-9]{16,64}";
    private static final Set<String> ALLOWED_ASSET_DETAILS = Set.of("IMAGE", "AUDIO", "OTHER");
    private static final EnumSet<IntegrityEventType> CLIENT_EVENTS = EnumSet.of(
            IntegrityEventType.TAB_HIDDEN,
            IntegrityEventType.TAB_VISIBLE,
            IntegrityEventType.INPUT_MODE_CHANGED,
            IntegrityEventType.NODE_PRESENTED,
            IntegrityEventType.ASSET_LOADED,
            IntegrityEventType.STIMULUS_STARTED,
            IntegrityEventType.RESPONSE_SELECTED,
            IntegrityEventType.RESPONSE_CONFIRMED
    );

    private final CandidateIntegritySessionRepository sessionRepository;
    private final CandidateIntegrityEventRepository eventRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final JwtService jwtService;
    private final boolean securityEnabled;
    private final String hashSecret;
    private final int heartbeatIntervalSeconds;
    private final int sessionTimeoutSeconds;
    private final Clock clock;

    @Autowired
    public CandidateIntegrityService(
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
        this(
                sessionRepository,
                eventRepository,
                candidateAttemptRepository,
                jwtService,
                securityEnabled,
                configuredHashSecret == null || configuredHashSecret.isBlank() ? jwtSecret : configuredHashSecret,
                heartbeatIntervalSeconds,
                sessionTimeoutSeconds,
                Clock.systemUTC()
        );
    }

    CandidateIntegrityService(
            CandidateIntegritySessionRepository sessionRepository,
            CandidateIntegrityEventRepository eventRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            JwtService jwtService,
            boolean securityEnabled,
            String hashSecret,
            int heartbeatIntervalSeconds,
            int sessionTimeoutSeconds,
            Clock clock
    ) {
        if (heartbeatIntervalSeconds <= 0 || sessionTimeoutSeconds <= heartbeatIntervalSeconds) {
            throw new IllegalArgumentException("A expiração da sessão deve ser maior que o intervalo de heartbeat.");
        }
        if (hashSecret == null || hashSecret.isBlank()) {
            throw new IllegalArgumentException("O segredo de pseudonimização da integridade é obrigatório.");
        }
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.jwtService = jwtService;
        this.securityEnabled = securityEnabled;
        this.hashSecret = hashSecret;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
        this.clock = clock;
    }

    @Transactional
    public CandidateIntegritySessionResponse startSession(
            String attemptToken,
            StartIntegritySessionRequest request,
            String remoteAddress,
            String userAgent
    ) {
        CandidateScope scope = resolveScope(attemptToken);
        requireAttempt(scope);
        Instant now = clock.instant();

        Optional<CandidateIntegritySessionEntity> activeSession = sessionRepository
                .findFirstByCandidateAttemptIdAndStatusOrderByStartedAtDesc(
                        scope.attemptId(),
                        IntegritySessionStatus.ACTIVE
                );

        if (activeSession.isPresent()) {
            CandidateIntegritySessionEntity active = activeSession.get();
            if (isExpired(active, now)) {
                expire(active, now);
            } else if (active.getClientSessionId().equals(request.clientSessionId())) {
                active.setLastHeartbeatAt(now);
                active.setInputMode(normalizeInputMode(request.inputMode()));
                sessionRepository.save(active);
                recordServerEvent(active, IntegrityEventType.SESSION_RESUMED, normalizeOccurredAt(request.occurredAt(), now), now);
                return response(active, true);
            } else {
                throw concurrentSession();
            }
        }

        CandidateIntegritySessionEntity session = new CandidateIntegritySessionEntity();
        session.setId(UUID.randomUUID().toString());
        session.setEmpresaId(scope.empresaId());
        session.setCandidateAttemptId(scope.attemptId());
        session.setClientSessionId(request.clientSessionId());
        session.setStatus(IntegritySessionStatus.ACTIVE);
        session.setStartedAt(now);
        session.setLastHeartbeatAt(now);
        session.setIpHash(hashAddress(remoteAddress));
        session.setUserAgentCategory(categorizeUserAgent(userAgent));
        session.setInputMode(normalizeInputMode(request.inputMode()));

        try {
            sessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException exception) {
            throw concurrentSession();
        }
        recordServerEvent(session, IntegrityEventType.SESSION_STARTED, normalizeOccurredAt(request.occurredAt(), now), now);
        return response(session, false);
    }

    @Transactional(readOnly = true)
    public void requireActiveSession(String attemptToken, String sessionId) {
        if ((sessionId == null || sessionId.isBlank()) && !securityEnabled) {
            return;
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A sessão segura da avaliação não foi identificada. Recarregue a página para continuar."
            );
        }
        CandidateScope scope = resolveScope(attemptToken);
        CandidateIntegritySessionEntity session = requireSession(scope, sessionId);
        requireActive(session, clock.instant());
    }

    @Transactional
    public void heartbeat(String attemptToken, IntegrityHeartbeatRequest request) {
        CandidateScope scope = resolveScope(attemptToken);
        Instant now = clock.instant();
        CandidateIntegritySessionEntity session = requireSession(scope, request.sessionId());
        requireActive(session, now);
        session.setLastHeartbeatAt(now);
        sessionRepository.save(session);
        recordServerEvent(session, IntegrityEventType.HEARTBEAT, normalizeOccurredAt(request.occurredAt(), now), now);
    }

    @Transactional
    public void recordEvent(String attemptToken, IntegrityEventRequest request) {
        if (!CLIENT_EVENTS.contains(request.eventType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de evento técnico não permitido para o cliente.");
        }

        CandidateScope scope = resolveScope(attemptToken);
        CandidateIntegritySessionEntity session = requireSession(scope, request.sessionId());
        Instant now = clock.instant();
        requireActive(session, now);

        if (request.sequenceNumber() != null
                && eventRepository.existsBySessionIdAndSequenceNumber(session.getId(), request.sequenceNumber())) {
            return;
        }

        IntegrityInputMode inputMode = request.inputMode();
        if (inputMode != null) {
            session.setInputMode(inputMode);
        }
        session.setLastHeartbeatAt(now);
        sessionRepository.save(session);

        CandidateIntegrityEventEntity event = baseEvent(
                session,
                request.eventType(),
                normalizeOccurredAt(request.occurredAt(), now),
                now
        );
        event.setInputMode(inputMode);
        event.setVisibilityState(visibilityFor(request.eventType()));
        event.setSequenceNumber(request.sequenceNumber());
        event.setDetail(normalizeDetail(request.eventType(), request.detail()));
        eventRepository.save(event);
    }

    @Transactional
    public void closeSession(String attemptToken, CloseIntegritySessionRequest request) {
        CandidateScope scope = resolveScope(attemptToken);
        CandidateIntegritySessionEntity session = requireSession(scope, request.sessionId());
        if (session.getStatus() != IntegritySessionStatus.ACTIVE) {
            return;
        }

        Instant now = clock.instant();
        session.setStatus(IntegritySessionStatus.CLOSED);
        session.setClosedAt(now);
        session.setLastHeartbeatAt(now);
        sessionRepository.save(session);
        recordServerEvent(session, IntegrityEventType.SESSION_CLOSED, normalizeOccurredAt(request.occurredAt(), now), now);
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

    private void requireAttempt(CandidateScope scope) {
        candidateAttemptRepository.findByEmpresaIdAndId(scope.empresaId(), scope.attemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));
    }

    private CandidateIntegritySessionEntity requireSession(CandidateScope scope, String sessionId) {
        return sessionRepository.findByIdAndCandidateAttemptId(sessionId, scope.attemptId())
                .filter(session -> session.getEmpresaId().equals(scope.empresaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão técnica não encontrada."));
    }

    private void requireActive(CandidateIntegritySessionEntity session, Instant now) {
        if (session.getStatus() != IntegritySessionStatus.ACTIVE || isExpired(session, now)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta sessão não está mais ativa. Recarregue a página para retomar a avaliação."
            );
        }
    }

    private boolean isExpired(CandidateIntegritySessionEntity session, Instant now) {
        return session.getLastHeartbeatAt().isBefore(now.minusSeconds(sessionTimeoutSeconds));
    }

    private void expire(CandidateIntegritySessionEntity session, Instant now) {
        session.setStatus(IntegritySessionStatus.EXPIRED);
        session.setClosedAt(now);
        sessionRepository.save(session);
        recordServerEvent(session, IntegrityEventType.SESSION_EXPIRED, now, now);
    }

    private CandidateIntegritySessionResponse response(CandidateIntegritySessionEntity session, boolean resumed) {
        return new CandidateIntegritySessionResponse(
                session.getId(),
                resumed,
                heartbeatIntervalSeconds,
                sessionTimeoutSeconds
        );
    }

    private void recordServerEvent(
            CandidateIntegritySessionEntity session,
            IntegrityEventType eventType,
            Instant occurredAt,
            Instant receivedAt
    ) {
        eventRepository.save(baseEvent(session, eventType, occurredAt, receivedAt));
    }

    private CandidateIntegrityEventEntity baseEvent(
            CandidateIntegritySessionEntity session,
            IntegrityEventType eventType,
            Instant occurredAt,
            Instant receivedAt
    ) {
        CandidateIntegrityEventEntity event = new CandidateIntegrityEventEntity();
        event.setEmpresaId(session.getEmpresaId());
        event.setCandidateAttemptId(session.getCandidateAttemptId());
        event.setSessionId(session.getId());
        event.setEventType(eventType);
        event.setOccurredAt(occurredAt);
        event.setReceivedAt(receivedAt);
        return event;
    }

    private Instant normalizeOccurredAt(Instant occurredAt, Instant now) {
        if (occurredAt == null
                || occurredAt.isBefore(now.minusSeconds(86_400))
                || occurredAt.isAfter(now.plusSeconds(86_400))) {
            return now;
        }
        return occurredAt;
    }

    private IntegrityInputMode normalizeInputMode(IntegrityInputMode inputMode) {
        return inputMode == null ? IntegrityInputMode.UNKNOWN : inputMode;
    }

    private String visibilityFor(IntegrityEventType eventType) {
        return switch (eventType) {
            case TAB_HIDDEN -> "HIDDEN";
            case TAB_VISIBLE -> "VISIBLE";
            default -> null;
        };
    }

    private String normalizeDetail(IntegrityEventType eventType, String detail) {
        if (eventType != IntegrityEventType.ASSET_LOADED && eventType != IntegrityEventType.STIMULUS_STARTED) {
            return null;
        }
        if (detail == null) {
            return "OTHER";
        }
        String normalized = detail.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ASSET_DETAILS.contains(normalized) ? normalized : "OTHER";
    }

    private String categorizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("ipad") || normalized.contains("tablet")) {
            return "TABLET_BROWSER";
        }
        if (normalized.contains("mobile") || normalized.contains("android") || normalized.contains("iphone")) {
            return "MOBILE_BROWSER";
        }
        return "DESKTOP_BROWSER";
    }

    private String hashAddress(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(remoteAddress.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível pseudonimizar o endereço de rede.", exception);
        }
    }

    private ResponseStatusException concurrentSession() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Esta avaliação já está aberta em outra sessão. Feche a outra janela ou aguarde a sessão expirar para continuar."
        );
    }

    private record CandidateScope(String empresaId, String attemptId) {
    }
}
