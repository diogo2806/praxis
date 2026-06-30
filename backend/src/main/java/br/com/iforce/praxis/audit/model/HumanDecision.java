package br.com.iforce.praxis.audit.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Disposição registrada por uma pessoa sobre um candidato. É o evento que materializa o
 * "humano no controle": a decisão final cabe a quem registra aqui, não à ferramenta.
 */
public enum HumanDecision implements DescribedEnum {

    ADVANCED("advanced"),
    REJECTED("rejected"),
    HIRED("hired"),
    ON_HOLD("onHold");

    private final String descricao;

    HumanDecision(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static HumanDecision fromString(String valor) {
        return DescribedEnums.fromValue(HumanDecision.class, valor);
    }
}
