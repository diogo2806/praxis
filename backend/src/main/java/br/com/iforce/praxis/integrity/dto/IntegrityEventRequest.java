package br.com.iforce.praxis.integrity.dto;

import br.com.iforce.praxis.integrity.model.IntegrityEventType;
import br.com.iforce.praxis.integrity.model.IntegrityInputMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record IntegrityEventRequest(
        @NotBlank
        @Pattern(regexp = "[0-9a-fA-F-]{36}")
        String sessionId,

        @NotNull
        IntegrityEventType eventType,

        Instant occurredAt,
        IntegrityInputMode inputMode,

        @Pattern(regexp = "VISIBLE|HIDDEN")
        String visibilityState,

        @PositiveOrZero
        Long sequenceNumber,

        @Size(max = 120)
        String detail
) {
}
