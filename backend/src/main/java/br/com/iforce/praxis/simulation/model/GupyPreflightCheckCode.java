package br.com.iforce.praxis.simulation.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


public enum GupyPreflightCheckCode implements DescribedEnum {

    PUBLIC_BASE_URL("publicBaseUrl"),
    INTEGRATION_TOKEN("integrationToken"),
    SIMULATION_VALIDATION("simulationValidation");

    private final String descricao;

    GupyPreflightCheckCode(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static GupyPreflightCheckCode fromString(String valor) {
        return DescribedEnums.fromValue(GupyPreflightCheckCode.class, valor);
    }
}
