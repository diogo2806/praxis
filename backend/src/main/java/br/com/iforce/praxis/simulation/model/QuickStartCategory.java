package br.com.iforce.praxis.simulation.model;

import java.util.Locale;


/**
 * Categorias de modelo pronto do "começar rápido". Cada categoria tem um
 * template estático (JSON) que gera um rascunho mínimo, porém válido.
 *
 * <p>Serializa pelo próprio nome ({@code ATENDIMENTO}, ...), conforme o
 * contrato da API de quick-start.</p>
 */
public enum QuickStartCategory {

    ATENDIMENTO,
    LIDERANCA,
    VENDAS,
    COMPLIANCE,
    ONBOARDING;

    /** Nome do arquivo de template (sem extensão) associado à categoria. */
    public String templateFileName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
