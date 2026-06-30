package br.com.iforce.praxis.journey.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Estados da tentativa de um candidato em uma Jornada de Avaliação.
 *
 * <p>A jornada controla o progresso geral; cada teste interno continua tendo a
 * sua própria {@code CandidateAttempt}.</p>
 */
public enum AssessmentJourneyAttemptStatus implements DescribedEnum {

    CREATED("created"),
    IN_PROGRESS("inProgress"),
    COMPLETED("completed"),
    EXPIRED("expired"),
    ABANDONED("abandoned");

    private final String descricao;

    AssessmentJourneyAttemptStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static AssessmentJourneyAttemptStatus fromString(String valor) {
        return DescribedEnums.fromValue(AssessmentJourneyAttemptStatus.class, valor);
    }
}
