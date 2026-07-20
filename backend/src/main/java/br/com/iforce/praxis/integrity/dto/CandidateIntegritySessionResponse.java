package br.com.iforce.praxis.integrity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sessão técnica autorizada para a aplicação do candidato.")
public record CandidateIntegritySessionResponse(
        String sessionId,
        boolean resumed,
        int heartbeatIntervalSeconds,
        int expiresAfterSeconds
) {
}
