package br.com.iforce.praxis.simulation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SimulationVersionStatus {

    DRAFT("draft"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    private static final Map<String, SimulationVersionStatus> NOME_PARA_ENUM_MAP = Stream.of(values())
            .collect(Collectors.toMap(status -> status.name().toLowerCase(), status -> status));

    private final String descricao;

    SimulationVersionStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static SimulationVersionStatus fromString(String valor) {
        if (valor == null) {
            return null;
        }

        SimulationVersionStatus enumPorNome = NOME_PARA_ENUM_MAP.get(valor.toLowerCase());
        if (enumPorNome != null) {
            return enumPorNome;
        }

        for (SimulationVersionStatus status : values()) {
            if (status.getDescricao().equalsIgnoreCase(valor)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Valor invalido para SimulationVersionStatus: '" + valor + "'");
    }
}
