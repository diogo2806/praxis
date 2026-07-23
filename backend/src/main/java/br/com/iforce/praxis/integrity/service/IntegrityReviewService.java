package br.com.iforce.praxis.integrity.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.integrity.dto.IntegrityReviewDecisionRequest;
import br.com.iforce.praxis.integrity.dto.IntegrityReviewDetailResponse;
import br.com.iforce.praxis.integrity.dto.IntegrityReviewPageResponse;
import br.com.iforce.praxis.integrity.dto.IntegrityReviewQueueItemResponse;
import br.com.iforce.praxis.integrity.dto.IntegrityReviewSharedStatusResponse;
import br.com.iforce.praxis.integrity.model.IntegrityEventType;
import br.com.iforce.praxis.integrity.model.IntegrityReviewAuditAction;
import br.com.iforce.praxis.integrity.model.IntegrityReviewDecision;
import br.com.iforce.praxis.integrity.model.IntegrityReviewStatus;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityEventEntity;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityReviewAuditEntity;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityReviewEntity;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegritySessionEntity;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityEventRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityReviewAuditRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityReviewRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegritySessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class IntegrityReviewService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int EVENT_SCAN_PAGE_SIZE = 500;
    private static final String SYSTEM_ACTOR = "SYSTEM_INTEGRITY_RULES";
    private static final Map<IntegrityEventType, Long> ALERT_THRESHOLDS = Map.of(
            IntegrityEventType.SESSION_EXPIRED, 1L,
            IntegrityEventType.TAB_HIDDEN, 3L,
            IntegrityEventType.SESSION_RESUMED, 2L,
            IntegrityEventType.INPUT_MODE_CHANGED, 8L
    );

    private final CandidateIntegrityReviewRepository reviewRepository;
    private final CandidateIntegrityReviewAuditRepository reviewAuditRepository;
    private final CandidateIntegrityEventRepository eventRepository;
    private final CandidateIntegritySessionRepository sessionRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int retentionDays;

    public IntegrityReviewService(
            CandidateIntegrityReviewRepository reviewRepository,
            CandidateIntegrityReviewAuditRepository reviewAuditRepository,
            CandidateIntegrityEventRepository eventRepository,
            CandidateIntegritySessionRepository sessionRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            AuditEventService auditEventService,
            ObjectMapper objectMapper,
            @Value("${praxis.integrity.evidence-retention-days:180}") int retentionDays
    ) {
        this(
                reviewRepository,
                reviewAuditRepository,
                eventRepository,
                sessionRepository,
                candidateAttemptRepository,
                currentEmpresaService,
                currentUserService,
                auditEventService,
                objectMapper,
                Clock.systemUTC(),
                retentionDays
        );
    }

    IntegrityReviewService(
            CandidateIntegrityReviewRepository reviewRepository,
            CandidateIntegrityReviewAuditRepository reviewAuditRepository,
            CandidateIntegrityEventRepository eventRepository,
            CandidateIntegritySessionRepository sessionRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            AuditEventService auditEventService,
            ObjectMapper objectMapper,
            Clock clock,
            int retentionDays
    ) {
        this.reviewRepository = reviewRepository;
        this.reviewAuditRepository = reviewAuditRepository;
        this.eventRepository = eventRepository;
        this.sessionRepository = sessionRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Transactional
    public IntegrityReviewPageResponse search(int page, int size) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        synchronizeQueueForEmpresa(empresaId);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Page<CandidateIntegrityReviewEntity> reviews = reviewRepository.findByEmpresaIdOrderByUpdatedAtDesc(
                empresaId,
                PageRequest.of(safePage, safeSize)
        );
        List<IntegrityReviewQueueItemResponse> items = reviews.getContent().stream()
                .map(review -> toQueueItem(empresaId, review))
                .toList();
        return new IntegrityReviewPageResponse(
                items,
                reviews.getNumber(),
                reviews.getSize(),
                reviews.getTotalElements(),
                reviews.getTotalPages()
        );
    }

    @Transactional
    public IntegrityReviewDetailResponse detail(String attemptId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        synchronizeQueueForEmpresa(empresaId);
        CandidateIntegrityReviewEntity review = requireReview(empresaId, attemptId);
        CandidateAttemptEntity attempt = requireAttempt(empresaId, attemptId);
        String actorUserId = currentUserService.requiredUserId();
        Instant now = clock.instant();

        appendReviewAudit(
                review,
                IntegrityReviewAuditAction.EVIDENCE_ACCESSED,
                actorUserId,
                json(Map.of("accessedAt", now.toString()))
        );
        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                attemptId,
                AuditEventType.INTEGRITY_EVIDENCE_ACCESSED,
                "Evidências técnicas acessadas por usuário autorizado.",
                json(Map.of("actorUserId", actorUserId, "reviewId", review.getId()))
        );

        List<CandidateIntegritySessionEntity> sessions = review.getEvidenceDiscardedAt() == null
                ? sessionRepository.findByEmpresaIdAndCandidateAttemptIdOrderByStartedAtAsc(empresaId, attemptId)
                : List.of();
        List<CandidateIntegrityEventEntity> events = review.getEvidenceDiscardedAt() == null
                ? eventRepository.findByEmpresaIdAndCandidateAttemptIdOrderByOccurredAtAscIdAsc(empresaId, attemptId)
                : List.of();

        return new IntegrityReviewDetailResponse(
                attemptId,
                attempt.getCandidateName(),
                attempt.getCandidateEmail(),
                attempt.getStatus(),
                review.getStatus(),
                review.getDecision(),
                review.getJustification(),
                review.isShareWithCompany(),
                review.getReviewedBy(),
                review.getDecidedAt(),
                review.getRetentionUntil(),
                review.getEvidenceDiscardedAt(),
                alertsFor(empresaId, attemptId),
                sessions.stream().map(this::toSession).toList(),
                events.stream().map(this::toTimelineEvent).toList(),
                reviewAuditRepository
                        .findByEmpresaIdAndCandidateAttemptIdOrderByCreatedAtAscIdAsc(empresaId, attemptId)
                        .stream()
                        .map(this::toAuditEntry)
                        .toList()
        );
    }

    @Transactional
    public IntegrityReviewDetailResponse decide(String attemptId, IntegrityReviewDecisionRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        synchronizeQueueForEmpresa(empresaId);
        CandidateIntegrityReviewEntity review = requireReview(empresaId, attemptId);
        String justification = request.justification() == null ? "" : request.justification().trim();
        if (justification.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A justificativa do parecer é obrigatória.");
        }

        IntegrityReviewStatus previousStatus = review.getStatus();
        IntegrityReviewDecision previousDecision = review.getDecision();
        String actorUserId = currentUserService.requiredUserId();
        Instant now = clock.instant();

        review.setStatus(IntegrityReviewStatus.DECIDED);
        review.setDecision(request.decision());
        review.setJustification(justification);
        review.setShareWithCompany(request.shareWithCompany());
        review.setReviewedBy(actorUserId);
        review.setDecidedAt(now);
        review.setUpdatedAt(now);
        reviewRepository.save(review);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("previousStatus", previousStatus.name());
        details.put("previousDecision", previousDecision == null ? null : previousDecision.name());
        details.put("newStatus", review.getStatus().name());
        details.put("newDecision", review.getDecision().name());
        details.put("justification", justification);
        details.put("shareWithCompany", review.isShareWithCompany());
        appendReviewAudit(
                review,
                IntegrityReviewAuditAction.DECISION_RECORDED,
                actorUserId,
                json(details)
        );
        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                attemptId,
                AuditEventType.INTEGRITY_REVIEW_DECIDED,
                "Parecer humano neutro registrado para a revisão técnica.",
                json(Map.of(
                        "actorUserId", actorUserId,
                        "reviewId", review.getId(),
                        "decision", review.getDecision().name(),
                        "shareWithCompany", review.isShareWithCompany()
                ))
        );
        return detailWithoutAccessAudit(empresaId, attemptId, review);
    }

    @Transactional(readOnly = true)
    public Optional<IntegrityReviewSharedStatusResponse> sharedStatus(String attemptId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        requireAttempt(empresaId, attemptId);
        return reviewRepository.findByEmpresaIdAndCandidateAttemptId(empresaId, attemptId)
                .filter(review -> review.getStatus() == IntegrityReviewStatus.DECIDED)
                .filter(CandidateIntegrityReviewEntity::isShareWithCompany)
                .map(review -> new IntegrityReviewSharedStatusResponse(
                        review.getDecision(),
                        review.getDecidedAt()
                ));
    }

    @Transactional
    public int synchronizeQueueForEmpresa(String empresaId) {
        Map<String, EnumMap<IntegrityEventType, Long>> countsByAttempt = new LinkedHashMap<>();
        int pageNumber = 0;
        Page<CandidateIntegrityEventEntity> eventPage;
        do {
            eventPage = eventRepository.findByEmpresaIdOrderByOccurredAtAscIdAsc(
                    empresaId,
                    PageRequest.of(pageNumber, EVENT_SCAN_PAGE_SIZE)
            );
            for (CandidateIntegrityEventEntity event : eventPage.getContent()) {
                if (!ALERT_THRESHOLDS.containsKey(event.getEventType())) {
                    continue;
                }
                countsByAttempt
                        .computeIfAbsent(event.getCandidateAttemptId(), ignored -> new EnumMap<>(IntegrityEventType.class))
                        .merge(event.getEventType(), 1L, Long::sum);
            }
            pageNumber++;
        } while (eventPage.hasNext());

        int created = 0;
        for (Map.Entry<String, EnumMap<IntegrityEventType, Long>> entry : countsByAttempt.entrySet()) {
            if (hasDeterministicAlert(entry.getValue()) && ensureReview(empresaId, entry.getKey()).created()) {
                created++;
            }
        }
        return created;
    }

    private IntegrityReviewDetailResponse detailWithoutAccessAudit(
            String empresaId,
            String attemptId,
            CandidateIntegrityReviewEntity review
    ) {
        CandidateAttemptEntity attempt = requireAttempt(empresaId, attemptId);
        List<CandidateIntegritySessionEntity> sessions = review.getEvidenceDiscardedAt() == null
                ? sessionRepository.findByEmpresaIdAndCandidateAttemptIdOrderByStartedAtAsc(empresaId, attemptId)
                : List.of();
        List<CandidateIntegrityEventEntity> events = review.getEvidenceDiscardedAt() == null
                ? eventRepository.findByEmpresaIdAndCandidateAttemptIdOrderByOccurredAtAscIdAsc(empresaId, attemptId)
                : List.of();
        return new IntegrityReviewDetailResponse(
                attemptId,
                attempt.getCandidateName(),
                attempt.getCandidateEmail(),
                attempt.getStatus(),
                review.getStatus(),
                review.getDecision(),
                review.getJustification(),
                review.isShareWithCompany(),
                review.getReviewedBy(),
                review.getDecidedAt(),
                review.getRetentionUntil(),
                review.getEvidenceDiscardedAt(),
                alertsFor(empresaId, attemptId),
                sessions.stream().map(this::toSession).toList(),
                events.stream().map(this::toTimelineEvent).toList(),
                reviewAuditRepository
                        .findByEmpresaIdAndCandidateAttemptIdOrderByCreatedAtAscIdAsc(empresaId, attemptId)
                        .stream()
                        .map(this::toAuditEntry)
                        .toList()
        );
    }

    private IntegrityReviewQueueItemResponse toQueueItem(
            String empresaId,
            CandidateIntegrityReviewEntity review
    ) {
        CandidateAttemptEntity attempt = requireAttempt(empresaId, review.getCandidateAttemptId());
        return new IntegrityReviewQueueItemResponse(
                review.getCandidateAttemptId(),
                attempt.getCandidateName(),
                attempt.getCandidateEmail(),
                attempt.getStatus(),
                alertsFor(empresaId, review.getCandidateAttemptId()).size(),
                review.getStatus(),
                review.getDecision(),
                review.getUpdatedAt(),
                review.getEvidenceDiscardedAt()
        );
    }

    private List<IntegrityReviewDetailResponse.Alert> alertsFor(String empresaId, String attemptId) {
        List<IntegrityReviewDetailResponse.Alert> alerts = new ArrayList<>();
        addAlertIfThresholdReached(
                alerts,
                "SESSION_INTERRUPTION",
                "Interrupção ou expiração de sessão",
                "A sessão deixou de receber heartbeat no prazo esperado. Isso pode decorrer de conexão instável, suspensão do navegador ou interrupção operacional.",
                count(empresaId, attemptId, IntegrityEventType.SESSION_EXPIRED),
                ALERT_THRESHOLDS.get(IntegrityEventType.SESSION_EXPIRED)
        );
        addAlertIfThresholdReached(
                alerts,
                "VISIBILITY_CHANGES",
                "Alternância recorrente de visibilidade",
                "A aba deixou de ficar visível em mais de uma ocasião. O registro é técnico e não classifica intenção ou comportamento.",
                count(empresaId, attemptId, IntegrityEventType.TAB_HIDDEN),
                ALERT_THRESHOLDS.get(IntegrityEventType.TAB_HIDDEN)
        );
        addAlertIfThresholdReached(
                alerts,
                "SESSION_RESUMPTIONS",
                "Sessão retomada mais de uma vez",
                "A mesma sessão do navegador foi retomada repetidamente, situação compatível com recarga, perda de conexão ou retomada operacional.",
                count(empresaId, attemptId, IntegrityEventType.SESSION_RESUMED),
                ALERT_THRESHOLDS.get(IntegrityEventType.SESSION_RESUMED)
        );
        addAlertIfThresholdReached(
                alerts,
                "INPUT_MODE_CHANGES",
                "Mudanças frequentes no modo de entrada",
                "Houve alternância frequente entre teclado, toque e ponteiro. Esse dado não representa deficiência, não altera pontuação e requer contexto humano.",
                count(empresaId, attemptId, IntegrityEventType.INPUT_MODE_CHANGED),
                ALERT_THRESHOLDS.get(IntegrityEventType.INPUT_MODE_CHANGED)
        );
        return List.copyOf(alerts);
    }

    private void addAlertIfThresholdReached(
            List<IntegrityReviewDetailResponse.Alert> alerts,
            String code,
            String title,
            String explanation,
            long occurrences,
            long threshold
    ) {
        if (occurrences >= threshold) {
            alerts.add(new IntegrityReviewDetailResponse.Alert(code, title, explanation, occurrences));
        }
    }

    private long count(String empresaId, String attemptId, IntegrityEventType eventType) {
        return eventRepository.countByEmpresaIdAndCandidateAttemptIdAndEventType(
                empresaId,
                attemptId,
                eventType
        );
    }

    private boolean hasDeterministicAlert(EnumMap<IntegrityEventType, Long> counts) {
        return ALERT_THRESHOLDS.entrySet().stream()
                .anyMatch(entry -> counts.getOrDefault(entry.getKey(), 0L) >= entry.getValue());
    }

    private ReviewCreation ensureReview(String empresaId, String attemptId) {
        Optional<CandidateIntegrityReviewEntity> existing = reviewRepository
                .findByEmpresaIdAndCandidateAttemptId(empresaId, attemptId);
        if (existing.isPresent()) {
            return new ReviewCreation(existing.get(), false);
        }
        requireAttempt(empresaId, attemptId);
        Instant now = clock.instant();
        CandidateIntegrityReviewEntity review = new CandidateIntegrityReviewEntity();
        review.setId(UUID.randomUUID().toString());
        review.setEmpresaId(empresaId);
        review.setCandidateAttemptId(attemptId);
        review.setStatus(IntegrityReviewStatus.PENDING);
        review.setShareWithCompany(false);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        review.setRetentionUntil(now.plus(retentionDays, ChronoUnit.DAYS));
        try {
            reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException exception) {
            CandidateIntegrityReviewEntity concurrent = reviewRepository
                    .findByEmpresaIdAndCandidateAttemptId(empresaId, attemptId)
                    .orElseThrow(() -> exception);
            return new ReviewCreation(concurrent, false);
        }
        appendReviewAudit(
                review,
                IntegrityReviewAuditAction.QUEUE_CREATED,
                SYSTEM_ACTOR,
                json(Map.of("rulesVersion", "2026-07-23", "retentionDays", retentionDays))
        );
        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                attemptId,
                AuditEventType.INTEGRITY_REVIEW_CREATED,
                "Tentativa incluída na fila de revisão técnica por regra determinística.",
                json(Map.of("reviewId", review.getId(), "rulesVersion", "2026-07-23"))
        );
        return new ReviewCreation(review, true);
    }

    private CandidateIntegrityReviewEntity requireReview(String empresaId, String attemptId) {
        return reviewRepository.findByEmpresaIdAndCandidateAttemptId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Revisão técnica não encontrada."
                ));
    }

    private CandidateAttemptEntity requireAttempt(String empresaId, String attemptId) {
        return candidateAttemptRepository.findByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Participação não encontrada."
                ));
    }

    private void appendReviewAudit(
            CandidateIntegrityReviewEntity review,
            IntegrityReviewAuditAction action,
            String actorUserId,
            String details
    ) {
        CandidateIntegrityReviewAuditEntity audit = new CandidateIntegrityReviewAuditEntity();
        audit.setEmpresaId(review.getEmpresaId());
        audit.setReviewId(review.getId());
        audit.setCandidateAttemptId(review.getCandidateAttemptId());
        audit.setAction(action);
        audit.setActorUserId(actorUserId);
        audit.setDetails(details);
        audit.setCreatedAt(clock.instant());
        reviewAuditRepository.save(audit);
    }

    private IntegrityReviewDetailResponse.Session toSession(CandidateIntegritySessionEntity session) {
        return new IntegrityReviewDetailResponse.Session(
                session.getId(),
                session.getStatus(),
                session.getStartedAt(),
                session.getLastHeartbeatAt(),
                session.getClosedAt(),
                session.getUserAgentCategory(),
                session.getInputMode()
        );
    }

    private IntegrityReviewDetailResponse.TimelineEvent toTimelineEvent(CandidateIntegrityEventEntity event) {
        return new IntegrityReviewDetailResponse.TimelineEvent(
                event.getId(),
                event.getSessionId(),
                event.getEventType(),
                event.getOccurredAt(),
                event.getReceivedAt(),
                event.getInputMode(),
                event.getVisibilityState(),
                event.getSequenceNumber(),
                event.getDetail()
        );
    }

    private IntegrityReviewDetailResponse.AuditEntry toAuditEntry(CandidateIntegrityReviewAuditEntity audit) {
        return new IntegrityReviewDetailResponse.AuditEntry(
                audit.getId(),
                audit.getAction(),
                audit.getActorUserId(),
                audit.getDetails(),
                audit.getCreatedAt()
        );
    }

    private String json(Map<String, ?> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível registrar a auditoria da revisão técnica.", exception);
        }
    }

    private record ReviewCreation(CandidateIntegrityReviewEntity review, boolean created) {
    }
}
