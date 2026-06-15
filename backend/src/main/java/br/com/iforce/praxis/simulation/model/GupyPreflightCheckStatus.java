package br.com.iforce.praxis.simulation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum GupyPreflightCheckStatus {

    OK("ok"),
    WARNING("warning"),
    BLOCKER("blocker");

    private static final Map<String, GupyPreflightCheckStatus> NOME_PARA_ENUM_MAP = Stream.of(values())
            .collect(Collectors.toMap(status -> status.name().toLowerCase(), status -> status));

    private final String descricao;

    GupyPreflightCheckStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static GupyPreflightCheckStatus fromString(String valor) {
        if (valor == null) {
            return null;
        }

        GupyPreflightCheckStatus enumPorNome = NOME_PARA_ENUM_MAP.get(valor.toLowerCase());
        if (enumPorNome != null) {
            return enumPorNome;
        }

        for (GupyPreflightCheckStatus status : values()) {
            if (status.getDescricao().equalsIgnoreCase(valor)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Valor invalido para GupyPreflightCheckStatus: '" + valor + "'");
    }
}
