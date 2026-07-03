package br.com.iforce.praxis.results.dto;

import java.util.List;


/**
 * Uma página da lista de resultados exibida na Central.
 *
 * <p>Junta, em uma única resposta, os candidatos da página atual, o resumo que
 * aparece no topo da tela e as informações de navegação entre páginas. É o que
 * alimenta a tela de resultados de ponta a ponta.</p>
 *
 * @param items os candidatos desta página (cada um resumido em uma linha)
 * @param summary os números-resumo do topo, considerando todos os candidatos filtrados
 * @param page número da página atual, começando em zero
 * @param size quantidade de candidatos por página
 * @param totalItems total de candidatos que atendem aos filtros (somando todas as páginas)
 * @param totalPages quantas páginas existem no total
 */
public record ResultsPageResponse(
        List<ResultListItemResponse> items,
        ResultsSummaryResponse summary,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
