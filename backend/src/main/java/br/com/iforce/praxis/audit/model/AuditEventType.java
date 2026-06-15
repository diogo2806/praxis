package br.com.iforce.praxis.audit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum AuditEventType {

    ATTEMPT_CREATED("attemptCreated"),
    ATTEMPT_STARTED("attemptStarted"),
    ANSWER_SUBMITTED("answerSubmitted"),
    ATTEMPT_COMPLETED("attemptCompleted"),
    SIMULATION_VERSION_PUBLISHED("simulationVersionPublished");

    private static final Map<String, AuditEventType> NOME_PARA_ENUM_MAP = Stream.of(values())
            .collect(Collectors.toMap(type -> type.name().toLowerCase(), type -> type));

    private final String descricao;

    AuditEventType(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static AuditEventType fromString(String valor) {
        if (valor == null) {
            return null;
        }

        AuditEventType enumPorNome = NOME_PARA_ENUM_MAP.get(valor.toLowerCase());
        if (enumPorNome != null) {
            return enumPorNome;
        }

        for (AuditEventType type : values()) {
            if (type.getDescricao().equalsIgnoreCase(valor)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Valor invalido para AuditEventType: '" + valor + "'");
    }
}
