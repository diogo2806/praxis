package br.com.iforce.praxis.gupy.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


public enum ResultTier implements DescribedEnum {

    MAJOR("major"),
    MINOR("minor");

    private final String descricao;

    ResultTier(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static ResultTier fromString(String valor) {
        return DescribedEnums.fromValue(ResultTier.class, valor);
    }
}
