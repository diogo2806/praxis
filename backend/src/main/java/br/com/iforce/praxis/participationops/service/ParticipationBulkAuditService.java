package br.com.iforce.praxis.participationops.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.participationops.persistence.entity.ParticipationBulkJobEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ParticipationBulkAuditService {

    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public ParticipationBulkAuditService(
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordCreated(ParticipationBulkJobEntity job) {
        auditEventService.appendUserEvent(
                job.getEmpresaId(),
                job.getRequestedBy(),
                AuditEventType.PARTICIPATION_BULK_JOB_CREATED,
                "Operação em lote de participações criada.",
                metadata(job)
        );
    }

    @Transactional
    public void recordCompleted(ParticipationBulkJobEntity job) {
        auditEventService.appendUserEvent(
                job.getEmpresaId(),
                job.getRequestedBy(),
                AuditEventType.PARTICIPATION_BULK_JOB_COMPLETED,
                "Operação em lote de participações concluída.",
                metadata(job)
        );
    }

    private String metadata(ParticipationBulkJobEntity job) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("jobId", job.getId());
        metadata.put("action", job.getAction());
        metadata.put("selectionMode", job.getSelectionMode());
        metadata.put("filter", job.getFilterJson());
        metadata.put("total", job.getTotalItems());
        metadata.put("succeeded", job.getSucceededItems());
        metadata.put("skipped", job.getSkippedItems());
        metadata.put("failed", job.getFailedItems());
        metadata.put("justification", job.getJustification());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao serializar auditoria do lote.", exception);
        }
    }
}
