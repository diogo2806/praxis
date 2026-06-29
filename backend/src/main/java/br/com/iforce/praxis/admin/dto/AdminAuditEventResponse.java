package br.com.iforce.praxis.admin.dto;

import br.com.iforce.praxis.audit.model.AuditEventType;

import java.time.Instant;

/**
 * Evento de auditoria exibido no painel ADMIN. Diferente do response operacional, inclui o
 * ator (operador) e o tenant alvo. Somente leitura: a trilha é append-only.
 */
public record AdminAuditEventResponse(
        Long id,
        String actorUserId,
        String tenantId,
        String aggregateType,
        String aggregateId,
        AuditEventType eventType,
        String message,
        String metadata,
        Instant createdAt
) {
}
