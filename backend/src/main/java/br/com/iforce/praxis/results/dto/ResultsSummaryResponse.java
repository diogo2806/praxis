package br.com.iforce.praxis.results.dto;

/**
 * Os números-resumo que aparecem no topo da Central de Resultados.
 *
 * <p>Dão ao recrutador uma visão rápida do andamento do processo antes mesmo de
 * olhar candidato por candidato. Consideram todos os candidatos que passam pelos
 * filtros aplicados no momento.</p>
 *
 * @param completed quantos candidatos concluíram a avaliação
 * @param inProgress quantos ainda estão avaliando — incluindo quem não começou ou pausou
 * @param expired quantos perderam o prazo sem concluir
 * @param averageScore nota média entre os que concluíram (fica em branco quando ninguém concluiu ainda)
 */
public record ResultsSummaryResponse(
        long completed,
        long inProgress,
        long expired,
        Integer averageScore
) {
}
