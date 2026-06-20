package br.com.iforce.praxis.audit.dto;

import br.com.iforce.praxis.audit.model.AuditEventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Evento imutável de auditoria operacional.")
public record AuditEventResponse(
        @Schema(example = "1")
        Long id,

        @Schema(example = "Tentativa do candidato")
        String aggregateType,

        @Schema(example = "att_123")
        String aggregateId,

        @Schema(example = "answerSubmitted")
        AuditEventType eventType,

        @Schema(example = "Resposta salva para a etapa turno-1.")
        String message,

        @Schema(example = "{\"nodeId\":\"turno-1\"}")
        String metadata,

        @Schema(example = "2026-06-15T20:00:00Z")
        Instant createdAt
) {
}
