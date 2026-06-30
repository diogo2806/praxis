package br.com.iforce.praxis.gupy.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


public enum AttemptStatus implements DescribedEnum {

    NOT_STARTED("notStarted"),
    IN_PROGRESS("inProgress"),
    PAUSED("paused"),
    COMPLETED("completed"),
    ABANDONED("abandoned"),
    EXPIRED("expired"),
    FAILED("failed");

    private final String descricao;

    AttemptStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static AttemptStatus fromString(String valor) {
        return DescribedEnums.fromValue(AttemptStatus.class, valor);
    }
}
