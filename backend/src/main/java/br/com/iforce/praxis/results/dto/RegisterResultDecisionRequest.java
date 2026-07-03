package br.com.iforce.praxis.results.dto;

import br.com.iforce.praxis.audit.model.HumanDecision;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;


/**
 * O que o recrutador envia ao decidir sobre um candidato.
 *
 * <p>É o registro do "humano no controle": a plataforma organiza e sugere, mas quem
 * decide o rumo do candidato é a pessoa. A decisão é obrigatória; a observação é
 * opcional e serve para justificar ou contextualizar a escolha.</p>
 *
 * @param decision a decisão tomada (avançar, reprovar, contratar ou deixar em espera) — obrigatória
 * @param note observação/justificativa opcional (até 1000 caracteres)
 */
public record RegisterResultDecisionRequest(
        @NotNull HumanDecision decision,
        @Size(max = 1000) String note
) {
}
