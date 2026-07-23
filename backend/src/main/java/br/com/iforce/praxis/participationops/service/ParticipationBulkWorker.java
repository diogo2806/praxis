package br.com.iforce.praxis.participationops.service;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;
import br.com.iforce.praxis.candidate.service.CompanyCandidateLinkService;
import br.com.iforce.praxis.journey.service.AssessmentJourneyAttemptLifecycleService;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.ParticipationRef;
import br.com.iforce.praxis.participationops.persistence.entity.ParticipationBulkItemEntity;
import br.com.iforce.praxis.participationops.persistence.entity.ParticipationBulkJobEntity;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationBulkItemRepository;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationBulkJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ParticipationBulkWorker {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ParticipationBulkJobRepository jobRepository;
    private final ParticipationBulkItemRepository itemRepository;
    private final CompanyCandidateLinkService candidateLinkService;
    private final AssessmentJourneyAttemptLifecycleService journeyLifecycleService;
    private final ParticipationTagService tagService;
    private final ParticipationBulkAuditService auditService;
    private final ObjectMapper objectMapper;

    public ParticipationBulkWorker(
            ParticipationBulkJobRepository jobRepository,
            ParticipationBulkItemRepository itemRepository,
            CompanyCandidateLinkService candidateLinkService,
            AssessmentJourneyAttemptLifecycleService journeyLifecycleService,
            ParticipationTagService tagService,
            ParticipationBulkAuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.jobRepository = jobRepository;
        this.itemRepository = itemRepository;
        this.candidateLinkService = candidateLinkService;
        this.journeyLifecycleService = journeyLifecycleService;
        this.tagService = tagService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Async
    public void process(String jobId) {
        ParticipationBulkJobEntity job = jobRepository.findById(jobId).orElse(null);
        if (job == null || !"PENDING".equals(job.getStatus())) {
            return;
        }

        EmpresaContextHolder.set(job.getEmpresaId());
        try {
            job.setStatus("RUNNING");
            job.setStartedAt(Instant.now());
            jobRepository.save(job);

            Map<String, Object> payload = payload(job.getPayloadJson());
            List<ParticipationBulkItemEntity> items = itemRepository.findByJobIdOrderByIdAsc(jobId);
            for (ParticipationBulkItemEntity item : items) {
                if (!"PENDING".equals(item.getStatus())) {
                    continue;
                }
                try {
                    execute(job, item, payload);
                    item.setStatus("SUCCEEDED");
                    item.setReason(null);
                    job.setSucceededItems(job.getSucceededItems() + 1);
                } catch (Exception exception) {
                    item.setStatus("FAILED");
                    item.setReason(safeReason(exception));
                    job.setFailedItems(job.getFailedItems() + 1);
                }
                item.setProcessedAt(Instant.now());
                itemRepository.save(item);
                job.setProcessedItems(job.getProcessedItems() + 1);
                jobRepository.save(job);
            }

            job.setStatus(job.getFailedItems() > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
            job.setCompletedAt(Instant.now());
            ParticipationBulkJobEntity completed = jobRepository.save(job);
            auditService.recordCompleted(completed);
        } finally {
            EmpresaContextHolder.clear();
        }
    }

    private void execute(
            ParticipationBulkJobEntity job,
            ParticipationBulkItemEntity item,
            Map<String, Object> payload
    ) {
        String type = item.getParticipationType();
        String participationId = item.getParticipationId();
        switch (job.getAction()) {
            case "RESEND" -> resend(type, participationId);
            case "EXTEND" -> extend(type, participationId, intValue(payload.get("additionalDays")));
            case "CANCEL" -> journeyLifecycleService.cancel(participationId);
            case "ADD_TAG" -> tagService.add(
                    job.getRequestedBy(),
                    new ParticipationRef(type, participationId),
                    stringValue(payload.get("tagId"))
            );
            case "REMOVE_TAG" -> tagService.remove(
                    new ParticipationRef(type, participationId),
                    stringValue(payload.get("tagId"))
            );
            case "EXPORT" -> {
                // O relatório é gerado dos itens persistidos, sem dados pessoais.
            }
            default -> throw new IllegalStateException("Ação de lote não suportada: " + job.getAction());
        }
    }

    private void resend(String type, String participationId) {
        if ("journey".equals(type)) {
            journeyLifecycleService.resendInvitation(participationId);
        } else if ("individual".equals(type)) {
            candidateLinkService.resendExisting(participationId);
        } else {
            throw new IllegalArgumentException("Tipo de participação inválido.");
        }
    }

    private void extend(String type, String participationId, int additionalDays) {
        if ("journey".equals(type)) {
            journeyLifecycleService.extendValidity(participationId, additionalDays);
        } else if ("individual".equals(type)) {
            candidateLinkService.extendValidity(participationId, additionalDays);
        } else {
            throw new IllegalArgumentException("Tipo de participação inválido.");
        }
    }

    private Map<String, Object> payload(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Payload do lote inválido.", exception);
        }
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static String stringValue(Object value) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Identificador obrigatório não informado.");
        }
        return text;
    }

    private static String safeReason(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return "Falha ao processar o item.";
        }
        String sanitized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return sanitized.length() <= 1000 ? sanitized : sanitized.substring(0, 1000);
    }
}
