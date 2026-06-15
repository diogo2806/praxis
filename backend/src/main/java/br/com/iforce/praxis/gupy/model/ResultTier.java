package br.com.iforce.praxis.gupy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ResultTier {

    MAJOR("major"),
    MINOR("minor");

    private static final Map<String, ResultTier> NOME_PARA_ENUM_MAP = Stream.of(values())
            .collect(Collectors.toMap(tier -> tier.name().toLowerCase(), tier -> tier));

    private final String descricao;

    ResultTier(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static ResultTier fromString(String valor) {
        if (valor == null) {
            return null;
        }

        ResultTier enumPorNome = NOME_PARA_ENUM_MAP.get(valor.toLowerCase());
        if (enumPorNome != null) {
            return enumPorNome;
        }

        for (ResultTier tier : values()) {
            if (tier.getDescricao().equalsIgnoreCase(valor)) {
                return tier;
            }
        }

        throw new IllegalArgumentException("Valor invalido para ResultTier: '" + valor + "'");
    }
}
