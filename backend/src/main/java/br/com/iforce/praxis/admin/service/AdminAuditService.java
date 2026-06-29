package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.AdminAuditEventResponse;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.persistence.entity.AuditEventEntity;
import br.com.iforce.praxis.audit.persistence.repository.AuditEventRepository;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Leitura da trilha de auditoria pelo painel ADMIN. A trilha é append-only: este serviço
 * apenas consulta eventos; não há caminho para editar, excluir ou ocultar registros.
 */
@Service
public class AdminAuditService {

    private final AuditEventRepository auditEventRepository;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;

    public AdminAuditService(
            AuditEventRepository auditEventRepository,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata
    ) {
        this.auditEventRepository = auditEventRepository;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
    }

    @Transactional(readOnly = true)
    public List<AdminAuditEventResponse> listForTenant(String tenantId, int limit) {
        return auditEventRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, limit)).stream()
                .map(AdminAuditService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminAuditEventResponse> listAll(int limit) {
        return auditEventRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(AdminAuditService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminAuditEventResponse getById(Long eventId) {
        return auditEventRepository.findById(eventId)
                .map(AdminAuditService::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento de auditoria não encontrado."));
    }

    /** Registra que o ADMIN consultou o uso de um cliente (evento obrigatório). */
    @Transactional
    public void recordUsageViewed(String actorUserId, String tenantId) {
        auditEventService.auditAdminAction(
                actorUserId, tenantId, AuditEventType.ADMIN_USAGE_VIEWED,
                "Uso do cliente consultado.",
                auditMetadata.of("tenantId", tenantId));
    }

    private static AdminAuditEventResponse toResponse(AuditEventEntity entity) {
        return new AdminAuditEventResponse(
                entity.getId(),
                entity.getActorUserId(),
                entity.getTenantId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getMessage(),
                entity.getMetadata(),
                entity.getCreatedAt()
        );
    }
}
