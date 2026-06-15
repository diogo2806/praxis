package br.com.iforce.praxis.gupy.delivery.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ResultDeliveryStatus {

    PENDING("pending"),
    RETRYING("retrying"),
    SENT("sent"),
    DLQ("dlq");

    private static final Map<String, ResultDeliveryStatus> NOME_PARA_ENUM_MAP = Stream.of(values())
            .collect(Collectors.toMap(status -> status.name().toLowerCase(), status -> status));

    private final String descricao;

    ResultDeliveryStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static ResultDeliveryStatus fromString(String valor) {
        if (valor == null) {
            return null;
        }

        ResultDeliveryStatus enumPorNome = NOME_PARA_ENUM_MAP.get(valor.toLowerCase());
        if (enumPorNome != null) {
            return enumPorNome;
        }

        for (ResultDeliveryStatus status : values()) {
            if (status.getDescricao().equalsIgnoreCase(valor)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Valor invalido para ResultDeliveryStatus: '" + valor + "'");
    }
}
