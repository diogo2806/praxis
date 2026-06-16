package br.com.iforce.praxis.gupy.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;
import br.com.iforce.praxis.shared.model.DescribedEnums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResultDecision implements DescribedEnum {

    RECOMMEND_INTERVIEW("recommendInterview"),
    REVIEW_REQUIRED("reviewRequired"),
    IN_PROGRESS("inProgress");

    private final String descricao;

    ResultDecision(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static ResultDecision fromString(String valor) {
        return DescribedEnums.fromValue(ResultDecision.class, valor);
    }
}
