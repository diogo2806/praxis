package br.com.iforce.praxis.results.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;


import java.time.Instant;

import java.util.List;


/**
 * O resultado completo de um candidato em uma avaliação — a tela de detalhe.
 *
 * <p>É a fotografia que embasa a decisão de contratação: quem é o candidato, qual
 * avaliação fez, em que situação está, a nota geral, o desempenho por competência,
 * o caminho que percorreu na prova e a última decisão humana já registrada sobre
 * ele.</p>
 *
 * @param attemptId identificador da avaliação do candidato
 * @param candidate dados de quem foi avaliado
 * @param simulation qual avaliação foi feita
 * @param status situação da avaliação (ex.: concluída, em andamento)
 * @param startedAt quando o candidato começou
 * @param finishedAt quando concluiu (em branco se ainda não concluiu)
 * @param overallScore nota geral obtida
 * @param competencies desempenho detalhado competência por competência
 * @param answers o passo a passo do que o candidato respondeu na avaliação
 * @param humanDecision a decisão humana mais recente registrada (vem vazia se ainda não houve)
 */
public record ResultDetailResponse(
        String attemptId,
        Candidate candidate,
        Simulation simulation,
        AttemptStatus status,
        Instant startedAt,
        Instant finishedAt,
        Integer overallScore,
        List<Competency> competencies,
        List<Answer> answers,
        HumanDecision humanDecision
) {
    /**
     * Quem foi avaliado.
     *
     * @param name nome do candidato
     * @param email e-mail do candidato
     * @param externalId identificador do candidato no sistema de origem, quando veio de uma integração
     */
    public record Candidate(String name, String email, String externalId) {
    }

    /**
     * Qual avaliação o candidato respondeu.
     *
     * @param id identificador da avaliação
     * @param title nome amigável da avaliação
     * @param versionNumber número da versão publicada que o candidato respondeu
     */
    public record Simulation(String id, String title, Integer versionNumber) {
    }

    /**
     * O desempenho do candidato em uma competência avaliada.
     *
     * @param name nome da competência
     * @param score nota obtida na competência
     * @param level nível de leitura rápida (alto, médio ou baixo)
     * @param summary frase que ajuda a interpretar o resultado da competência
     */
    public record Competency(String name, int score, String level, String summary) {
    }

    /**
     * Uma etapa do caminho que o candidato percorreu na avaliação.
     *
     * @param stepTitle título da situação apresentada
     * @param question o que foi apresentado ao candidato
     * @param answer a alternativa que ele escolheu (ou "Tempo esgotado", se o tempo acabou)
     * @param score quantos pontos aquela escolha valeu
     */
    public record Answer(String stepTitle, String question, String answer, Integer score) {
    }

    /**
     * A decisão que uma pessoa registrou sobre o candidato.
     *
     * @param status a decisão tomada (ex.: avançar, reprovar, contratar, em espera)
     * @param decidedBy quem tomou a decisão
     * @param decidedAt quando a decisão foi registrada
     * @param note observação/justificativa opcional
     */
    public record HumanDecision(String status, String decidedBy, Instant decidedAt, String note) {
    }
}
