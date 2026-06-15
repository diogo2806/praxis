package br.com.iforce.praxis.gupy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum AttemptStatus {

    NOT_STARTED("notStarted"),
    IN_PROGRESS("inProgress"),
    PAUSED("paused"),
    COMPLETED("completed"),
    FAILED("failed");

    private static final Map<String, AttemptStatus> NOME_PARA_ENUM_MAP = Stream.of(values())
            .collect(Collectors.toMap(status -> status.name().toLowerCase(), status -> status));

    private final String descricao;

    AttemptStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static AttemptStatus fromString(String valor) {
        if (valor == null) {
            return null;
        }

        AttemptStatus enumPorNome = NOME_PARA_ENUM_MAP.get(valor.toLowerCase());
        if (enumPorNome != null) {
            return enumPorNome;
        }

        for (AttemptStatus status : values()) {
            if (status.getDescricao().equalsIgnoreCase(valor)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Valor invalido para AttemptStatus: '" + valor + "'");
    }
}
