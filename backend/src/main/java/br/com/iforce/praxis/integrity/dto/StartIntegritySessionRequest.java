package br.com.iforce.praxis.integrity.dto;

import br.com.iforce.praxis.integrity.model.IntegrityInputMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

@Schema(description = "Identifica a aba ou navegador que iniciou a aplicação.")
public record StartIntegritySessionRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9._:-]{16,80}")
        @Schema(description = "Identificador aleatório da sessão do navegador, sem dados pessoais.")
        String clientSessionId,

        @Schema(description = "Horário observado pelo navegador. O servidor mantém seu próprio horário de recebimento.")
        Instant occurredAt,

        @Schema(description = "Modo de entrada inicialmente observado.")
        IntegrityInputMode inputMode
) {
}
