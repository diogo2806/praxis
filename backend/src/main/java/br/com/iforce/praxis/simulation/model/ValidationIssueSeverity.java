package br.com.iforce.praxis.simulation.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


public enum ValidationIssueSeverity implements DescribedEnum {

    WARNING("warning"),
    BLOCKER("blocker");

    private final String descricao;

    ValidationIssueSeverity(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static ValidationIssueSeverity fromString(String valor) {
        return DescribedEnums.fromValue(ValidationIssueSeverity.class, valor);
    }
}
