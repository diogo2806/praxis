package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;


/**
 * Requisição de um direito do titular feita pelo candidato (LGPD art. 18).
 *
 * <p>O tipo do direito é obrigatório; contato e detalhes são opcionais e servem
 * para o controlador retornar ao titular dentro do prazo legal.</p>
 */
@Schema(description = "Requisição de direito do titular feita pelo candidato (LGPD art. 18).")
public record DataSubjectRequest(
        @NotNull
        @Schema(description = "Direito do titular solicitado, conforme o art. 18 da LGPD.")
        DataSubjectRequestType requestType,

        @Size(max = 320)
        @Schema(nullable = true, description = "Contato (e-mail) opcional para retorno ao titular.")
        String contact,

        @Size(max = 1000)
        @Schema(nullable = true, description = "Detalhamento opcional do pedido.")
        String details
) {
}
