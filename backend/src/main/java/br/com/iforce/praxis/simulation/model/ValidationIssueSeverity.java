package br.com.iforce.praxis.simulation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ValidationIssueSeverity {

    WARNING("warning"),
    BLOCKER("blocker");

    private static final Map<String, ValidationIssueSeverity> NOME_PARA_ENUM_MAP = Stream.of(values())
            .collect(Collectors.toMap(severity -> severity.name().toLowerCase(), severity -> severity));

    private final String descricao;

    ValidationIssueSeverity(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static ValidationIssueSeverity fromString(String valor) {
        if (valor == null) {
            return null;
        }

        ValidationIssueSeverity enumPorNome = NOME_PARA_ENUM_MAP.get(valor.toLowerCase());
        if (enumPorNome != null) {
            return enumPorNome;
        }

        for (ValidationIssueSeverity severity : values()) {
            if (severity.getDescricao().equalsIgnoreCase(valor)) {
                return severity;
            }
        }

        throw new IllegalArgumentException("Valor invalido para ValidationIssueSeverity: '" + valor + "'");
    }
}
