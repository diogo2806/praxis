package br.com.iforce.praxis.recrutei.service;

import br.com.iforce.praxis.config.PraxisProperties;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.model.CandidateAttempt;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;

import br.com.iforce.praxis.gupy.model.ResultItem;

import br.com.iforce.praxis.recrutei.dto.RecruteiTestResultItemResponse;

import br.com.iforce.praxis.recrutei.dto.RecruteiTestResultResponse;

import org.springframework.stereotype.Component;


import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.util.Comparator;


/**
 * Tradutor do resultado de uma prova para o "idioma" da Recrutei.
 *
 * <p>Na visão do processo, depois que um candidato termina a prova, a Práxis
 * guarda o resultado no seu próprio formato. Quando a Recrutei pede esse
 * resultado, este componente pega o que a Práxis tem e reorganiza nos campos e
 * nomes que a Recrutei espera receber — por exemplo, traduzindo a situação da
 * prova ("em andamento", "concluída") e montando os links para abrir o
 * relatório. Ele não decide nada e não altera dados: apenas converte o que já
 * existe para o formato do parceiro.</p>
 */
@Component
public class RecruteiTestResultMapper {

    private static final String PROVIDER_NAME = "Praxis";
    private static final String TYPE_RESULT = "percentage";

    private final PraxisProperties praxisProperties;

    public RecruteiTestResultMapper(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

    /**
     * Monta o resultado completo da prova no formato esperado pela Recrutei.
     *
     * <p>Junta os dados da prova aplicada (situação, pontuação, data de
     * conclusão) com os dados do teste publicado (nome e descrição) e gera os
     * links do relatório — tanto o da empresa quanto o do candidato. As
     * competências avaliadas são listadas em ordem alfabética, cada uma com a
     * sua nota e o seu nível de desempenho.</p>
     *
     * @param attempt a participação do candidato, com a situação e as notas
     * @param simulation o teste publicado que foi aplicado (nome e descrição)
     * @return o resultado pronto para ser entregue à Recrutei
     */
    public RecruteiTestResultResponse toResponse(CandidateAttempt attempt, PublishedSimulation simulation) {
        return new RecruteiTestResultResponse(
                simulation.name(),
                simulation.id(),
                simulation.description(),
                PROVIDER_NAME,
                toRecruteiStatus(attempt.status()),
                attempt.companyResultString(),
                resultPageUrl(attempt.resultId(), attempt.companyId()),
                candidatePageUrl(attempt.id()),
                attempt.results().stream()
                        .sorted(Comparator.comparing(ResultItem::name))
                        .map(item -> toItemResponse(item.name(), item.score(), item.tier().getDescricao(), attempt.finishedAt()))
                        .toList()
        );
    }

    /**
     * Monta a linha de resultado de uma única competência avaliada.
     *
     * <p>Para cada competência da prova, gera um item com o nome, uma frase
     * descritiva, a nota obtida, o tipo de nota (percentual) e o nível de
     * desempenho, além da data em que a prova foi concluída. Uso interno.</p>
     */
    private RecruteiTestResultItemResponse toItemResponse(String title, int score, String tier, Instant finishedAt) {
        return new RecruteiTestResultItemResponse(
                title,
                "Pontuação da competência " + title + ".",
                score,
                TYPE_RESULT,
                tier,
                finishedAt == null ? null : finishedAt.toString()
        );
    }

    /**
     * Traduz a situação interna da prova para os termos usados pela Recrutei.
     *
     * <p>Resumindo o estágio em que a participação está: ainda não iniciada
     * vira "pending"; concluída, abandonada, expirada ou com falha viram
     * "done" (ou seja, encerrada); e em andamento ou pausada viram
     * "in_progress". Uso interno.</p>
     */
    private String toRecruteiStatus(AttemptStatus status) {
        return switch (status) {
            case NOT_STARTED -> "pending";
            case COMPLETED, ABANDONED, EXPIRED -> "done";
            case IN_PROGRESS -> "in_progress";
        };
    }

    /**
     * Monta o link da página de resultado vista pela empresa.
     *
     * <p>Combina o endereço público da Práxis com o identificador do resultado
     * e o da empresa, para abrir o relatório do candidato no lado de quem
     * recruta. Uso interno.</p>
     */
    private String resultPageUrl(String resultId, String companyId) {
        return praxisProperties.publicBaseUrl() + "/recrutei/test/result/" + resultId
                + "?company_id=" + URLEncoder.encode(companyId, StandardCharsets.UTF_8);
    }

    /**
     * Monta o link da página de resultado vista pelo próprio candidato.
     *
     * <p>Combina o endereço público da Práxis com o identificador da
     * participação, para que o candidato acesse o seu próprio resultado. Uso
     * interno.</p>
     */
    private String candidatePageUrl(String attemptId) {
        return praxisProperties.publicBaseUrl() + "/candidate/attempts/" + attemptId;
    }
}
