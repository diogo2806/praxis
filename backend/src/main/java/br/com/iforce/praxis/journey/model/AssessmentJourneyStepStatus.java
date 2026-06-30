package br.com.iforce.praxis.journey.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Estados de cada etapa (teste) dentro da tentativa da jornada do candidato.
 *
 * <ul>
 *     <li>{@code PENDING}: ainda não liberada/iniciada.</li>
 *     <li>{@code IN_PROGRESS}: o teste individual foi iniciado.</li>
 *     <li>{@code COMPLETED}: a {@code CandidateAttempt} correspondente foi concluída.</li>
 * </ul>
 */
public enum AssessmentJourneyStepStatus implements DescribedEnum {

    PENDING("pending"),
    IN_PROGRESS("inProgress"),
    COMPLETED("completed");

    private final String descricao;

    AssessmentJourneyStepStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static AssessmentJourneyStepStatus fromString(String valor) {
        return DescribedEnums.fromValue(AssessmentJourneyStepStatus.class, valor);
    }
}
