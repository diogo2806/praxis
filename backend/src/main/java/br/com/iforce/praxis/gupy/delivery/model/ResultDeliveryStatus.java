package br.com.iforce.praxis.gupy.delivery.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;
import br.com.iforce.praxis.shared.model.DescribedEnums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResultDeliveryStatus implements DescribedEnum {

    PENDING("pending"),
    RETRYING("retrying"),
    SENT("sent"),
    DLQ("dlq");

    private final String descricao;

    ResultDeliveryStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static ResultDeliveryStatus fromString(String valor) {
        return DescribedEnums.fromValue(ResultDeliveryStatus.class, valor);
    }
}
