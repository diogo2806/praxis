package br.com.iforce.praxis.simulation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum GupyPreflightCheckCode {

    PUBLIC_BASE_URL("publicBaseUrl"),
    INTEGRATION_TOKEN("integrationToken"),
    SIMULATION_VALIDATION("simulationValidation");

    private static final Map<String, GupyPreflightCheckCode> NOME_PARA_ENUM_MAP = Stream.of(values())
            .collect(Collectors.toMap(code -> code.name().toLowerCase(), code -> code));

    private final String descricao;

    GupyPreflightCheckCode(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static GupyPreflightCheckCode fromString(String valor) {
        if (valor == null) {
            return null;
        }

        GupyPreflightCheckCode enumPorNome = NOME_PARA_ENUM_MAP.get(valor.toLowerCase());
        if (enumPorNome != null) {
            return enumPorNome;
        }

        for (GupyPreflightCheckCode code : values()) {
            if (code.getDescricao().equalsIgnoreCase(valor)) {
                return code;
            }
        }

        throw new IllegalArgumentException("Valor invalido para GupyPreflightCheckCode: '" + valor + "'");
    }
}
