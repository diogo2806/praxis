package br.com.iforce.praxis.simulation.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


public enum SimulationVersionStatus implements DescribedEnum {

    DRAFT("draft"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    private final String descricao;

    SimulationVersionStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static SimulationVersionStatus fromString(String valor) {
        return DescribedEnums.fromValue(SimulationVersionStatus.class, valor);
    }
}
