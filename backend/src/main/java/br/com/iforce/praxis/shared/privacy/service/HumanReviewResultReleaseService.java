package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.GupyTestResultMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.integration.service.GenericWebhookDeliveryService;
import br.com.iforce.praxis.shared.outbox.service.OutboxService;
import br.com.iforce.praxis.shared.privacy.model.ComplianceRequestStatus;
import br.com.iforce.praxis.shared.privacy.persistence.entity.HumanReviewRequestEntity;
import br.com.iforce.praxis.shared.privacy.persistence.repository.HumanReviewRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HumanReviewResultReleaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HumanReviewResultReleaseService.class);
    private static final String RESULT_READY_EVENT = "RESULT_READY";
    private static final String CANDIDATE_ATTEMPT_AGGREGATE = "CandidateAttempt";
    private static final int BATCH_SIZE = 100;

    private final HumanReviewRequestRepository reviewRequestRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final GupyTestResultMapper gupyTestResultMapper;
    private final GenericWebhookDeliveryService genericWebhookDeliveryService;
    private final OutboxService outboxService;

    public HumanReviewResultReleaseService(
            HumanReviewRequestRepository reviewRequestRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationCatalogService simulationCatalogService,
            GupyTestResultMapper gupyTestResultMapper,
            GenericWebhookDeliveryService genericWebhookDeliveryService,
            OutboxService outboxService
    ) {
        this.reviewRequestRepository = reviewRequestRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.gupyTestResultMapper = gupyTestResultMapper;
        this.genericWebhookDeliveryService = genericWebhookDeliveryService;
        this.outboxService = outboxService;
    }

    @Scheduled(fixedDelayString = "${praxis.human-review-release-delay-ms:60000}")
    @Transactional
    public void releaseResolvedResults() {
        List<HumanReviewRequestEntity> pending = reviewRequestRepository
                .findByStatusAndResultReleasedAtIsNullOrderByResolvedAtAsc(
                        ComplianceRequestStatus.RESOLVED,
                        PageRequest.of(0, BATCH_SIZE)
                );
        for (HumanReviewRequestEntity review : pending) {
            release(review);
        }
    }

    private void release(HumanReviewRequestEntity review) {
        CandidateAttemptEntity attempt = candidateAttemptRepository
                .findByEmpresaIdAndId(review.getEmpresaId(), review.getAttemptId())
                .orElse(null);
        if (attempt == null || attempt.getAnonymizedAt() != null) {
            markReleased(review);
            return;
        }
        if (attempt.getStatus() != AttemptStatus.COMPLETED || attempt.isHumanReviewRequired()) {
            return;
        }

        boolean hasGupyWebhook = attempt.getResultWebhookUrl() != null
                && !attempt.getResultWebhookUrl().isBlank();
        boolean hasGenericWebhook = genericWebhookDeliveryService.hasActiveResultWebhook(attempt.getEmpresaId());
        if (hasGupyWebhook || hasGenericWebhook) {
            PublishedSimulation simulation = findSimulation(attempt);
            TestResultResponse testResult = gupyTestResultMapper.toResponse(attempt, simulation);
            Map<String, Object> payload = new LinkedHashMap<>();
            if (hasGupyWebhook) {
                payload.put("webhookUrl", attempt.getResultWebhookUrl());
            }
            payload.put("testResult", testResult);
            payload.put("humanReviewRequestId", review.getId());
            outboxService.publish(
                    attempt.getEmpresaId(),
                    RESULT_READY_EVENT,
                    CANDIDATE_ATTEMPT_AGGREGATE,
                    attempt.getId(),
                    payload
            );
        }
        markReleased(review);
        LOGGER.info("Resultado da tentativa {} liberado após revisão humana {}.", attempt.getId(), review.getId());
    }

    private PublishedSimulation findSimulation(CandidateAttemptEntity attempt) {
        if (attempt.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(attempt.getSimulationVersionId())
                    .orElseThrow(() -> new IllegalStateException("Versão da avaliação não encontrada."));
        }
        return simulationCatalogService.findPublishedById(attempt.getEmpresaId(), attempt.getSimulationId())
                .orElseThrow(() -> new IllegalStateException("Avaliação publicada não encontrada."));
    }

    private void markReleased(HumanReviewRequestEntity review) {
        Instant now = Instant.now();
        review.setResultReleasedAt(now);
        review.setUpdatedAt(now);
        reviewRequestRepository.save(review);
    }
}
