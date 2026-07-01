package br.com.iforce.praxis.journey.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Estados de uma Jornada de Avaliação.
 *
 * <ul>
 *     <li>{@code DRAFT}: rascunho editável pelo empresa dono.</li>
 *     <li>{@code PUBLISHED}: composição estável, pode gerar links de candidatos.</li>
 *     <li>{@code ARCHIVED}: não gera novos links, mas preserva o histórico.</li>
 * </ul>
 */
public enum AssessmentJourneyStatus implements DescribedEnum {

    DRAFT("draft"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    private final String descricao;

    AssessmentJourneyStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static AssessmentJourneyStatus fromString(String valor) {
        return DescribedEnums.fromValue(AssessmentJourneyStatus.class, valor);
    }
}
