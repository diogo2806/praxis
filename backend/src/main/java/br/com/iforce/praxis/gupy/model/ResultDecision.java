package br.com.iforce.praxis.gupy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ResultDecision {

    RECOMMEND_INTERVIEW("recommendInterview"),
    REVIEW_REQUIRED("reviewRequired"),
    IN_PROGRESS("inProgress");

    private static final Map<String, ResultDecision> NOME_PARA_ENUM_MAP = Stream.of(values())
            .collect(Collectors.toMap(decision -> decision.name().toLowerCase(), decision -> decision));

    private final String descricao;

    ResultDecision(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static ResultDecision fromString(String valor) {
        if (valor == null) {
            return null;
        }

        ResultDecision enumPorNome = NOME_PARA_ENUM_MAP.get(valor.toLowerCase());
        if (enumPorNome != null) {
            return enumPorNome;
        }

        for (ResultDecision decision : values()) {
            if (decision.getDescricao().equalsIgnoreCase(valor)) {
                return decision;
            }
        }

        throw new IllegalArgumentException("Valor invalido para ResultDecision: '" + valor + "'");
    }
}
