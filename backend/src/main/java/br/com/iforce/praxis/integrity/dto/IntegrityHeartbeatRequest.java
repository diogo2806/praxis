package br.com.iforce.praxis.integrity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public record IntegrityHeartbeatRequest(
        @NotBlank
        @Pattern(regexp = "[0-9a-fA-F-]{36}")
        String sessionId,
        Instant occurredAt
) {
}
