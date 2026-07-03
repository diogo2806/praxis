package br.com.iforce.praxis.gupy.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


public enum AttemptStatus implements DescribedEnum {

    NOT_STARTED("notStarted"),
    IN_PROGRESS("inProgress"),
    /**
     * RESERVADO / INALCANÇÁVEL hoje: nenhum fluxo de produção chama {@code setStatus(PAUSED)} e não
     * existe endpoint de pausar. É mantido apenas para uma futura funcionalidade de pausa. Enquanto
     * não houver transição que o produza, "pausada"/CONTINUAR_TESTE em telas e mapeamentos são
     * apenas placeholders — não anunciar como capacidade existente.
     */
    PAUSED("paused"),
    COMPLETED("completed"),
    ABANDONED("abandoned"),
    EXPIRED("expired"),
    /**
     * RESERVADO / INALCANÇÁVEL hoje: nenhum fluxo de produção chama {@code setStatus(FAILED)}.
     * Mantido para um futuro estado de falha terminal; não representa um estado que ocorra hoje.
     */
    FAILED("failed");

    private final String descricao;

    AttemptStatus(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static AttemptStatus fromString(String valor) {
        return DescribedEnums.fromValue(AttemptStatus.class, valor);
    }
}
