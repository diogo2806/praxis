package br.com.iforce.praxis.tenantconfig.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;


/**
 * Tipos de catalogo configuravel por empresa usados por telas ativas do produto.
 */
public enum EmpresaConfigType implements DescribedEnum {

    COMPETENCY("competency"),
    ANSWER_TIME_LIMIT("answerTimeLimit");

    private final String descricao;

    EmpresaConfigType(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }
}
