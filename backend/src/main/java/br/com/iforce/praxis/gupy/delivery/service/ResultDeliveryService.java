package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.delivery.dto.ProcessReadyDeliveriesResponse;
import br.com.iforce.praxis.gupy.delivery.dto.ReprocessDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.dto.ResultDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import br.com.iforce.praxis.gupy.delivery.persistence.entity.ResultDeliveryEntity;
import br.com.iforce.praxis.gupy.delivery.persistence.repository.ResultDeliveryRepository;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.service.GupyTestResultMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ResultDeliveryService {

    private static final int MAX_ATTEMPT_COUNT = 5;

    private final ResultDeliveryRepository resultDeliveryRepository;
    private final ResultWebhookClient resultWebhookClient;
    private final SimulationCatalogService simulationCatalogService;
    private final GupyTestResultMapper gupyTestResultMapper;

    public ResultDeliveryService(
            ResultDeliveryRepository resultDeliveryRepository,
            ResultWebhookClient resultWebhookClient,
            SimulationCatalogService simulationCatalogService,
            GupyTestResultMapper gupyTestResultMapper
    ) {
        this.resultDeliveryRepository = resultDeliveryRepository;
        this.resultWebhookClient = resultWebhookClient;
        this.simulationCatalogService = simulationCatalogService;
        this.gupyTestResultMapper = gupyTestResultMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueIfNeeded(CandidateAttemptEntity candidateAttemptEntity) {
        if (candidateAttemptEntity.getResultWebhookUrl() == null || candidateAttemptEntity.getResultWebhookUrl().isBlank()) {
            return;
        }

        boolean deliveryAlreadyExists = resultDeliveryRepository.findByCandidateAttemptId(candidateAttemptEntity.getId()).isPresent();
        if (deliveryAlreadyExists) {
            return;
        }

        ResultDeliveryEntity resultDeliveryEntity = new ResultDeliveryEntity();
        resultDeliveryEntity.setCandidateAttempt(candidateAttemptEntity);
        resultDeliveryEntity.setWebhookUrl(candidateAttemptEntity.getResultWebhookUrl());
        resultDeliveryEntity.setStatus(ResultDeliveryStatus.PENDING);
        resultDeliveryEntity.setAttemptCount(0);
        resultDeliveryEntity.setNextAttemptAt(Instant.now());
        resultDeliveryEntity.setCreatedAt(Instant.now());

        resultDeliveryRepository.save(resultDeliveryEntity);
    }

    @Transactional(readOnly = true)
    public List<ResultDeliveryResponse> listDeliveries(
            ResultDeliveryStatus status,
            String simulationId,
            Integer versionNumber
    ) {
        List<ResultDeliveryEntity> deliveries;
        if (simulationId != null && !simulationId.isBlank() && versionNumber != null) {
            deliveries = status == null
                    ? resultDeliveryRepository
                            .findByCandidateAttemptSimulationIdAndCandidateAttemptSimulationVersionNumberOrderByCreatedAtDesc(
                                    simulationId,
                                    versionNumber
                            )
                    : resultDeliveryRepository
                            .findByCandidateAttemptSimulationIdAndCandidateAttemptSimulationVersionNumberAndStatusOrderByCreatedAtDesc(
                                    simulationId,
                                    versionNumber,
                                    status
                            );
        } else {
            deliveries = status == null
                    ? resultDeliveryRepository.findAll()
                    : resultDeliveryRepository.findByStatusOrderByCreatedAtDesc(status);
        }

        return deliveries.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResultDeliveryResponse> listReadyForRetry() {
        return resultDeliveryRepository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(ResultDeliveryStatus.PENDING, ResultDeliveryStatus.RETRYING),
                        Instant.now()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReprocessDeliveryResponse reprocessDelivery(Long deliveryId) {
        ResultDeliveryEntity resultDeliveryEntity = resultDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega de resultado nao encontrada."));

        processDelivery(resultDeliveryEntity);

        return new ReprocessDeliveryResponse(toResponse(resultDeliveryEntity));
    }

    @Transactional
    public ProcessReadyDeliveriesResponse processReadyDeliveries() {
        List<ResultDeliveryEntity> readyDeliveries = resultDeliveryRepository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(ResultDeliveryStatus.PENDING, ResultDeliveryStatus.RETRYING),
                        Instant.now()
                );

        for (ResultDeliveryEntity resultDeliveryEntity : readyDeliveries) {
            processDelivery(resultDeliveryEntity);
        }

        List<ResultDeliveryResponse> responses = readyDeliveries.stream()
                .map(this::toResponse)
                .toList();

        return new ProcessReadyDeliveriesResponse(responses.size(), responses);
    }

    private void processDelivery(ResultDeliveryEntity resultDeliveryEntity) {
        if (resultDeliveryEntity.getStatus() == ResultDeliveryStatus.SENT) {
            return;
        }

        resultDeliveryEntity.setLastAttemptAt(Instant.now());
        resultDeliveryEntity.setAttemptCount(resultDeliveryEntity.getAttemptCount() + 1);

        try {
            resultWebhookClient.postResult(
                    resultDeliveryEntity.getWebhookUrl(),
                    toTestResultResponse(resultDeliveryEntity.getCandidateAttempt())
            );
            resultDeliveryEntity.setStatus(ResultDeliveryStatus.SENT);
            resultDeliveryEntity.setSentAt(Instant.now());
            resultDeliveryEntity.setNextAttemptAt(null);
            resultDeliveryEntity.setLastError(null);
        } catch (RuntimeException exception) {
            registerFailure(resultDeliveryEntity, exception);
        }
    }

    private void registerFailure(ResultDeliveryEntity resultDeliveryEntity, RuntimeException exception) {
        resultDeliveryEntity.setLastError(limitMessage(exception.getMessage()));

        // 4xx = erro de contrato: não adianta retentar, vai direto para a DLQ p/ inspeção do admin.
        if (isContractError(exception) || resultDeliveryEntity.getAttemptCount() >= MAX_ATTEMPT_COUNT) {
            resultDeliveryEntity.setStatus(ResultDeliveryStatus.DLQ);
            resultDeliveryEntity.setNextAttemptAt(null);
            return;
        }

        resultDeliveryEntity.setStatus(ResultDeliveryStatus.RETRYING);
        resultDeliveryEntity.setNextAttemptAt(
                Instant.now().plusSeconds(retryDelaySeconds(resultDeliveryEntity.getAttemptCount()))
        );
    }

    private boolean isContractError(RuntimeException exception) {
        return exception instanceof RestClientResponseException responseException
                && responseException.getStatusCode().is4xxClientError();
    }

    private long retryDelaySeconds(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> 1L;
            case 2 -> 4L;
            case 3 -> 16L;
            case 4 -> 64L;
            default -> 256L;
        };
    }

    private String limitMessage(String message) {
        if (message == null) {
            return "Falha desconhecida no envio.";
        }
        if (message.length() <= 1200) {
            return message;
        }
        return message.substring(0, 1200);
    }

    private TestResultResponse toTestResultResponse(CandidateAttemptEntity candidateAttemptEntity) {
        PublishedSimulation simulation = candidateAttemptEntity.getSimulationVersionId() == null
                ? simulationCatalogService.findPublishedById(candidateAttemptEntity.getSimulationId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulacao publicada nao encontrada."))
                : simulationCatalogService.findByVersionId(candidateAttemptEntity.getSimulationVersionId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao da simulacao nao encontrada."));
        return gupyTestResultMapper.toResponse(candidateAttemptEntity, simulation);
    }

    private ResultDeliveryResponse toResponse(ResultDeliveryEntity resultDeliveryEntity) {
        return new ResultDeliveryResponse(
                resultDeliveryEntity.getId(),
                resultDeliveryEntity.getCandidateAttempt().getId(),
                resultDeliveryEntity.getCandidateAttempt().getResultId(),
                resultDeliveryEntity.getWebhookUrl(),
                resultDeliveryEntity.getStatus(),
                resultDeliveryEntity.getAttemptCount(),
                resultDeliveryEntity.getNextAttemptAt(),
                resultDeliveryEntity.getLastAttemptAt(),
                resultDeliveryEntity.getSentAt(),
                resultDeliveryEntity.getLastError(),
                resultDeliveryEntity.getCreatedAt()
        );
    }
}
