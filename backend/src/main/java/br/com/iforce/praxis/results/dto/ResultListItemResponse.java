package br.com.iforce.praxis.results.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;


import java.time.Instant;


/**
 * Uma linha da lista de resultados: o resumo de um candidato para comparação rápida.
 *
 * <p>Reúne o mínimo que o recrutador precisa para bater o olho e decidir em quem
 * clicar para ver mais: quem é, qual avaliação fez, em que situação está, como
 * pontuou, onde se destacou e por onde entrou no processo.</p>
 *
 * @param attemptId identificador da avaliação do candidato (usado para abrir o detalhe)
 * @param candidateName nome do candidato
 * @param candidateEmail e-mail do candidato
 * @param simulationId identificador da avaliação realizada
 * @param simulationTitle nome amigável da avaliação
 * @param status situação da avaliação (ex.: concluída, em andamento, expirada)
 * @param startedAt quando o candidato começou a avaliação
 * @param finishedAt quando concluiu (em branco se ainda não concluiu)
 * @param overallScore nota geral obtida (em branco enquanto não há resultado)
 * @param highlightCompetency competência em que o candidato foi melhor
 * @param integrationProvider origem do candidato (Manual, Gupy, Recrutei ou API)
 */
public record ResultListItemResponse(
        String attemptId,
        String candidateName,
        String candidateEmail,
        String simulationId,
        String simulationTitle,
        AttemptStatus status,
        Instant startedAt,
        Instant finishedAt,
        Integer overallScore,
        String highlightCompetency,
        String integrationProvider
) {
}
