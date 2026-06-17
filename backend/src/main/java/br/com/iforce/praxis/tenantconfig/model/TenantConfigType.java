package br.com.iforce.praxis.tenantconfig.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

/**
 * Tipos de catalogo configuravel por tenant. Cada tipo agrupa uma lista de opcoes
 * exibidas no frontend (selects, chips, checklists) que antes eram fixas no codigo.
 */
public enum TenantConfigType implements DescribedEnum {

    COMPETENCY("competency"),
    SENIORITY_LEVEL("seniorityLevel"),
    LANGUAGE_CHECKLIST("languageChecklist"),
    RESULT_USE("resultUse"),
    ANSWER_TIME_LIMIT("answerTimeLimit");

    private final String descricao;

    TenantConfigType(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }
}
