package br.com.iforce.praxis.term.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Situação de aceite do termo pelo usuário atual.")
public record TermAcceptanceStatusResponse(
        String type,
        String currentVersion,
        boolean accepted,
        String acceptedVersion,
        Instant acceptedAt
) {
}
