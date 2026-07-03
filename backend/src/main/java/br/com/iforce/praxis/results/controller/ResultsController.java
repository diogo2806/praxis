package br.com.iforce.praxis.results.controller;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.results.dto.RegisterResultDecisionRequest;

import br.com.iforce.praxis.results.dto.ResultDetailResponse;

import br.com.iforce.praxis.results.dto.ResultsPageResponse;

import br.com.iforce.praxis.results.service.ResultsService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;


import java.time.Instant;


/**
 * Porta de entrada (API) da Central de Resultados.
 *
 * <p>É por aqui que a tela de resultados conversa com o sistema. Oferece as três
 * ações que o recrutador realiza sobre candidatos já avaliados: ver a lista com
 * filtros, abrir o detalhe de um candidato e registrar a decisão sobre ele.</p>
 */
@RestController
@RequestMapping("/api/v1/results")
public class ResultsController {

    private final ResultsService resultsService;

    public ResultsController(ResultsService resultsService) {
        this.resultsService = resultsService;
    }

    /**
     * Lista os candidatos avaliados, aplicando os filtros escolhidos na tela.
     *
     * <p>Devolve a página pedida junto com o resumo do topo (concluídos, em andamento,
     * expirados e nota média). Todos os filtros são opcionais; sem nenhum, mostra os
     * candidatos mais recentes.</p>
     *
     * @param search busca por nome ou e-mail do candidato
     * @param simulationId restringe a uma avaliação específica
     * @param status restringe a uma situação (ex.: concluída, em andamento)
     * @param integrationProvider restringe à origem do candidato (Manual, Gupy, Recrutei, API)
     * @param periodStart data inicial do período considerado
     * @param periodEnd data final do período considerado
     * @param page página desejada (começa em zero)
     * @param size quantidade de candidatos por página
     * @return a página de resultados com o resumo do topo
     */
    @GetMapping
    public ResponseEntity<ResultsPageResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String simulationId,
            @RequestParam(required = false) AttemptStatus status,
            @RequestParam(required = false) String integrationProvider,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(resultsService.list(
                search,
                simulationId,
                status,
                integrationProvider,
                periodStart,
                periodEnd,
                page,
                size
        ));
    }

    /**
     * Abre o resultado completo de um candidato para análise.
     *
     * @param attemptId identificador da avaliação do candidato
     * @return o detalhe do candidato: dados, situação, notas, competências, respostas e decisão registrada
     */
    @GetMapping("/{attemptId}")
    public ResponseEntity<ResultDetailResponse> get(@PathVariable String attemptId) {
        return ResponseEntity.ok(resultsService.get(attemptId));
    }

    /**
     * Registra a decisão de uma pessoa sobre o candidato (avançar, reprovar, contratar ou deixar em espera).
     *
     * @param attemptId identificador da avaliação do candidato
     * @param request a decisão tomada e uma observação opcional
     * @return resposta sem conteúdo, confirmando que a decisão foi registrada
     */
    @PostMapping("/{attemptId}/decision")
    public ResponseEntity<Void> registerDecision(
            @PathVariable String attemptId,
            @Valid @RequestBody RegisterResultDecisionRequest request
    ) {
        resultsService.registerDecision(attemptId, request);
        return ResponseEntity.noContent().build();
    }
}
