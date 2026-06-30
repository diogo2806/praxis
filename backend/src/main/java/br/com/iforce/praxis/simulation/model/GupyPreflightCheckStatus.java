package br.com.iforce.praxis.simulation.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


public enum GupyPreflightCheckStatus implements DescribedEnum {

    OK("ok"),
    WARNING("warning"),
    BLOCKER("blocker");

    private final String descricao;

    GupyPreflightCheckStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static GupyPreflightCheckStatus fromString(String valor) {
        return DescribedEnums.fromValue(GupyPreflightCheckStatus.class, valor);
    }
}
