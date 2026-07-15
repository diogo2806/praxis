package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Locale;


@Schema(description = "Pedido da empresa para criar uma nova aplicação de simulacao para um candidato.")
public record CreateCandidateLinkRequest(
        @NotBlank
        @Schema(example = "sim-atendimento-n2")
        String simulationId,

        @NotBlank
        @Schema(example = "Maria Silva")
        String candidateName,

        @Email
        @NotBlank
        @Schema(example = "maria@example.com")
        String candidateEmail,

        @NotBlank
        @Size(max = 120)
        @Schema(
                example = "vaga-1234-etapa-tecnica-2026-07",
                description = "Identificador do ciclo de aplicação. Repetir o mesmo valor torna o pedido idempotente; usar outro valor cria uma nova tentativa."
        )
        String applicationCycleId,

        @Size(max = 200)
        @Schema(
                example = "Vaga Desenvolvedor Java - etapa técnica",
                description = "Contexto legível da aplicação, como vaga, processo ou etapa."
        )
        String applicationContext,

        @Schema(example = "1.50", description = "Multiplicador de tempo para acomodacoes de acessibilidade.")
        BigDecimal accommodationTimeMultiplier
) {

    /**
     * Compatibilidade para fluxos internos que ainda não expõem ciclo de aplicação.
     * Chamadas HTTP usam o construtor canônico e exigem applicationCycleId explícito.
     */
    public CreateCandidateLinkRequest(
            String simulationId,
            String candidateName,
            String candidateEmail,
            BigDecimal accommodationTimeMultiplier
    ) {
        this(
                simulationId,
                candidateName,
                candidateEmail,
                legacyCycleId(simulationId, candidateEmail),
                "Fluxo interno",
                accommodationTimeMultiplier
        );
    }

    private static String legacyCycleId(String simulationId, String candidateEmail) {
        String normalizedSimulation = simulationId == null ? "unknown" : simulationId.trim();
        String normalizedEmail = candidateEmail == null
                ? "unknown"
                : candidateEmail.trim().toLowerCase(Locale.ROOT);
        return "internal:" + normalizedSimulation + ":" + normalizedEmail;
    }
}
