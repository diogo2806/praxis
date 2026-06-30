package br.com.iforce.praxis.simulation.controller;

import br.com.iforce.praxis.simulation.dto.CalibrationReportResponse;

import br.com.iforce.praxis.simulation.dto.CloneSimulationVersionResponse;

import br.com.iforce.praxis.simulation.dto.CreateNodeRequest;

import br.com.iforce.praxis.simulation.dto.CreateOptionRequest;

import br.com.iforce.praxis.simulation.dto.CreateSimulationDraftRequest;

import br.com.iforce.praxis.simulation.dto.CreateSimulationRequest;

import br.com.iforce.praxis.simulation.dto.GupyPreflightResponse;

import br.com.iforce.praxis.simulation.dto.PublishSimulationResponse;

import br.com.iforce.praxis.simulation.dto.QuickStartCreatedResponse;

import br.com.iforce.praxis.simulation.dto.QuickStartRequest;

import br.com.iforce.praxis.simulation.dto.QuickStartTemplateSummaryResponse;

import br.com.iforce.praxis.simulation.dto.SimulationMonitoringResponse;

import br.com.iforce.praxis.simulation.dto.SimulationSummaryResponse;

import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;

import br.com.iforce.praxis.simulation.dto.SimulationVersionDetailResponse;

import br.com.iforce.praxis.simulation.dto.TalentMatchResponse;

import br.com.iforce.praxis.simulation.dto.UpdateBlueprintRequest;

import br.com.iforce.praxis.simulation.dto.UpdateNodeRequest;

import br.com.iforce.praxis.simulation.dto.UpdateOptionRequest;

import br.com.iforce.praxis.simulation.service.GupyPreflightService;

import br.com.iforce.praxis.simulation.service.SimulationAdminService;

import br.com.iforce.praxis.simulation.service.SimulationCalibrationService;

import br.com.iforce.praxis.simulation.service.SimulationQuickStartService;

import br.com.iforce.praxis.simulation.service.SimulationMonitoringService;

import br.com.iforce.praxis.simulation.service.TalentMatchService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.media.Content;

import io.swagger.v3.oas.annotations.media.ExampleObject;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PatchMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;


import java.util.List;


/**
 * Porta de entrada (API) da autoria e administração das provas (simulações).
 *
 * <p>Na visão do processo, é o "estúdio" onde a equipe monta e gerencia as
 * provas: cria uma prova nova, edita o rascunho (plano da avaliação,
 * competências, pesos, etapas e respostas), valida a estrutura, publica a
 * versão, clona uma versão publicada para gerar a próxima, e acompanha a
 * prova depois de no ar (monitoramento, diagnóstico de integração e
 * comparação de candidatos). Cada prova evolui em versões: as publicadas são
 * imutáveis e as edições acontecem sempre em um rascunho.</p>
 */
@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Simulations Admin", description = "Validação e publicação de versões de simulações.")
public class SimulationAdminController {

    private static final String ERROR_EXAMPLE = """
            {
              "timestamp": "2026-06-16T13:20:00Z",
              "status": 409,
              "error": "Conflict",
              "message": "Versao precisa estar aprovada antes da publicacao.",
              "path": "/api/v1/simulations/sim-atendimento-caos/versions/1/publish",
              "fields": {}
            }
            """;

    private final SimulationAdminService simulationAdminService;
    private final SimulationMonitoringService simulationMonitoringService;
    private final GupyPreflightService gupyPreflightService;
    private final TalentMatchService talentMatchService;
    private final SimulationCalibrationService simulationCalibrationService;
    private final SimulationQuickStartService simulationQuickStartService;

    public SimulationAdminController(
            SimulationAdminService simulationAdminService,
            SimulationMonitoringService simulationMonitoringService,
            GupyPreflightService gupyPreflightService,
            TalentMatchService talentMatchService,
            SimulationCalibrationService simulationCalibrationService,
            SimulationQuickStartService simulationQuickStartService
    ) {
        this.simulationAdminService = simulationAdminService;
        this.simulationMonitoringService = simulationMonitoringService;
        this.gupyPreflightService = gupyPreflightService;
        this.talentMatchService = talentMatchService;
        this.simulationCalibrationService = simulationCalibrationService;
        this.simulationQuickStartService = simulationQuickStartService;
    }

    /**
     * Lista as provas ativas para o painel administrativo.
     *
     * <p>Traz a versão mais recente de cada prova não arquivada.</p>
     *
     * @return o resumo de cada prova ativa
     */
    @GetMapping
    @Operation(
            summary = "Lista simulacoes ativas",
            description = "Retorna a versão mais recente de cada teste não arquivado para alimentar o painel administrativo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulacoes retornadas."),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<List<SimulationSummaryResponse>> listActiveSimulations() {
        return ResponseEntity.ok(simulationAdminService.listActiveSimulations());
    }

    /**
     * Cria uma prova nova já com a primeira versão (v1) em rascunho.
     *
     * <p>Define as competências e os pesos de cada uma logo na criação.</p>
     *
     * @param request dados iniciais da prova (competências e pesos)
     * @return os detalhes da versão recém-criada
     */
    @PostMapping
    @Operation(
            summary = "Cria simulacao e versao inicial",
            description = "Cria um teste com versão v1 em rascunho e pesos explícitos por competência."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Simulacao criada."),
            @ApiResponse(responseCode = "400", description = "Dados invalidos.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationVersionDetailResponse> createSimulation(
            @Valid @RequestBody CreateSimulationRequest request
    ) {
        return ResponseEntity.status(201).body(simulationAdminService.createSimulation(request));
    }

    /**
     * Cria uma prova em rascunho a partir do plano da avaliação.
     *
     * @param request o plano da avaliação que dá origem ao rascunho
     * @return o resumo da prova criada
     */
    @PostMapping("/drafts")
    @Operation(
            summary = "Cria simulacao em rascunho",
            description = "Cria uma simulacao com versao inicial em rascunho a partir do plano da avaliacao."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rascunho criado."),
            @ApiResponse(responseCode = "400", description = "Dados invalidos.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationSummaryResponse> createDraftSimulation(
            @Valid @RequestBody CreateSimulationDraftRequest request
    ) {
        return ResponseEntity.ok(simulationAdminService.createDraftSimulation(request));
    }

    /**
     * Lista os modelos prontos do "começar rápido".
     *
     * @return os resumos de cada modelo disponível por categoria
     */
    @GetMapping("/quick-start/templates")
    @Operation(
            summary = "Lista modelos prontos",
            description = "Retorna os modelos do começar rápido (categoria, título, descrição e número de cenários)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modelos retornados."),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<List<QuickStartTemplateSummaryResponse>> listQuickStartTemplates() {
        return ResponseEntity.ok(simulationQuickStartService.listTemplates());
    }

    /**
     * Cria uma simulação em rascunho a partir de um modelo pronto.
     *
     * @param request a categoria do modelo a usar
     * @return o identificador, a versão criada e a rota de destino
     */
    @PostMapping("/quick-start")
    @Operation(
            summary = "Cria rascunho a partir de modelo pronto",
            description = "Gera uma simulação completa em rascunho (competências, etapas, respostas e pesos) a partir de um modelo por categoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rascunho criado."),
            @ApiResponse(responseCode = "400", description = "Categoria invalida.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<QuickStartCreatedResponse> createFromQuickStart(
            @Valid @RequestBody QuickStartRequest request
    ) {
        return ResponseEntity.status(201).body(simulationQuickStartService.createFromTemplate(request.category()));
    }

    /**
     * Abre uma versão da prova para edição/visualização na tela de autoria.
     *
     * <p>Traz o plano, as competências, as etapas e as respostas da versão.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @return os detalhes completos da versão
     */
    @GetMapping("/{simulationId}/versions/{versionNumber}")
    @Operation(
            summary = "Detalha versao de simulacao",
            description = "Retorna plano do teste, competências, etapas e respostas da versão para telas de autoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versao retornada."),
            @ApiResponse(responseCode = "404", description = "Versão não encontrada.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationVersionDetailResponse> getSimulationVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationAdminService.loadVersion(simulationId, versionNumber));
    }

    /**
     * Atualiza o plano da avaliação de uma versão em rascunho.
     *
     * <p>Permite alterar a etapa inicial, as competências e os pesos. Só vale
     * para versões em rascunho (as publicadas são imutáveis).</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @param request os novos dados do plano
     * @return o resumo atualizado da prova
     */
    @PatchMapping("/{simulationId}/versions/{versionNumber}/blueprint")
    @Operation(
            summary = "Atualiza plano da avaliacao da versao",
            description = "Atualiza rootNodeId, competências e pesos de uma versão em rascunho."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano da avaliacao atualizado."),
            @ApiResponse(responseCode = "400", description = "Dados invalidos.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Versão não pode ser editada neste estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationSummaryResponse> updateBlueprint(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @Valid @RequestBody UpdateBlueprintRequest request
    ) {
        return ResponseEntity.ok(simulationAdminService.updateBlueprint(simulationId, versionNumber, request));
    }

    /**
     * Adiciona uma nova etapa (cena) ao rascunho da prova.
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param request o conteúdo da nova etapa
     * @return o identificador da etapa criada
     */
    @PostMapping("/{simulationId}/versions/{versionNumber}/nodes")
    @Operation(summary = "Adiciona etapa ao rascunho")
    public ResponseEntity<String> addNode(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @Valid @RequestBody CreateNodeRequest request
    ) {
        return ResponseEntity.status(201).body(simulationAdminService.addNode(simulationId, versionNumber, request));
    }

    /**
     * Atualiza o conteúdo de uma etapa existente no rascunho.
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param nodeId identificador da etapa
     * @param request os novos dados da etapa
     * @return confirmação sem conteúdo
     */
    @PutMapping("/{simulationId}/versions/{versionNumber}/nodes/{nodeId}")
    @Operation(summary = "Atualiza etapa do rascunho")
    public ResponseEntity<Void> updateNode(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable String nodeId,
            @Valid @RequestBody UpdateNodeRequest request
    ) {
        simulationAdminService.updateNode(simulationId, versionNumber, nodeId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Remove uma etapa do rascunho da prova.
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param nodeId identificador da etapa a remover
     * @return confirmação sem conteúdo
     */
    @DeleteMapping("/{simulationId}/versions/{versionNumber}/nodes/{nodeId}")
    @Operation(summary = "Remove etapa do rascunho")
    public ResponseEntity<Void> deleteNode(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable String nodeId
    ) {
        simulationAdminService.deleteNode(simulationId, versionNumber, nodeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adiciona uma resposta (alternativa) a uma etapa do rascunho.
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param nodeId identificador da etapa
     * @param request o conteúdo da nova resposta (texto e pontos por competência)
     * @return o identificador da resposta criada
     */
    @PostMapping("/{simulationId}/versions/{versionNumber}/nodes/{nodeId}/options")
    @Operation(summary = "Adiciona resposta à etapa do rascunho")
    public ResponseEntity<String> addOption(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable String nodeId,
            @Valid @RequestBody CreateOptionRequest request
    ) {
        return ResponseEntity.status(201).body(simulationAdminService.addOption(simulationId, versionNumber, nodeId, request));
    }

    /**
     * Atualiza uma resposta (alternativa) de uma etapa do rascunho.
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param nodeId identificador da etapa
     * @param optionId identificador da resposta
     * @param request os novos dados da resposta
     * @return confirmação sem conteúdo
     */
    @PutMapping("/{simulationId}/versions/{versionNumber}/nodes/{nodeId}/options/{optionId}")
    @Operation(summary = "Atualiza resposta do rascunho")
    public ResponseEntity<Void> updateOption(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable String nodeId,
            @PathVariable String optionId,
            @Valid @RequestBody UpdateOptionRequest request
    ) {
        simulationAdminService.updateOption(simulationId, versionNumber, nodeId, optionId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Remove uma resposta (alternativa) de uma etapa do rascunho.
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão em rascunho
     * @param nodeId identificador da etapa
     * @param optionId identificador da resposta a remover
     * @return confirmação sem conteúdo
     */
    @DeleteMapping("/{simulationId}/versions/{versionNumber}/nodes/{nodeId}/options/{optionId}")
    @Operation(summary = "Remove resposta do rascunho")
    public ResponseEntity<Void> deleteOption(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable String nodeId,
            @PathVariable String optionId
    ) {
        simulationAdminService.deleteOption(simulationId, versionNumber, nodeId, optionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Valida a estrutura de uma versão antes da publicação.
     *
     * <p>Aponta problemas que impedem a publicação (por exemplo, etapas sem
     * saída ou competências sem pontuação), para que a equipe corrija.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @return o resultado da validação, com eventuais problemas encontrados
     */
    @GetMapping("/{simulationId}/versions/{versionNumber}/validation")
    @Operation(
            summary = "Valida versão de simulação",
            description = "Executa validação estrutural do teste antes da publicação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validacao executada."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationValidationResponse> validateVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationAdminService.validateVersion(simulationId, versionNumber));
    }

    /**
     * Gera o relatório de calibração estatística de uma versão.
     *
     * <p>A partir das tentativas já concluídas, calcula índices de discriminação
     * e dificuldade por opção e a distribuição das notas por competência,
     * sinalizando o que está estatisticamente fraco ou invertido. Quando a
     * amostra ainda é pequena, devolve apenas o aviso de amostra insuficiente.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @return o relatório de calibração
     */
    @GetMapping("/{simulationId}/versions/{versionNumber}/calibration")
    @Operation(
            summary = "Calibração estatística da versão",
            description = "Calcula discriminação/dificuldade por opção e a distribuição por competência a partir das tentativas concluídas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calibracao calculada."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Versão não encontrada.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<CalibrationReportResponse> getCalibrationReport(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationCalibrationService.getCalibrationReport(simulationId, versionNumber));
    }

    /**
     * Cria a próxima versão em rascunho a partir de uma versão já publicada.
     *
     * <p>Permite evoluir a prova preservando intacta a versão publicada (que é
     * imutável): a edição acontece numa cópia em rascunho.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão publicada a clonar
     * @return os dados da nova versão em rascunho
     */
    @PostMapping("/{simulationId}/versions/{versionNumber}/clone-draft")
    @Operation(
            summary = "Clona versao publicada para edicao",
            description = "Cria a proxima versao em rascunho a partir de uma versao publicada, preservando a publicada imutavel."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rascunho clonado."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Versão não pode ser clonada neste estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<CloneSimulationVersionResponse> clonePublishedVersionToDraft(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationAdminService.clonePublishedVersionToDraft(simulationId, versionNumber));
    }


    /**
     * Publica uma versão da prova, tornando-a disponível para uso.
     *
     * <p>Só publica versões sem impedimentos (blockers). Não há como forçar a
     * publicação de uma versão com problema bloqueante.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão a publicar
     * @return o resultado da publicação
     */
    @PostMapping("/{simulationId}/versions/{versionNumber}/publish")
    @Operation(
            summary = "Publica versão de simulação",
            description = "Publica somente versões sem blockers. Não existe override para blocker."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versao publicada."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Publicacao bloqueada.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<PublishSimulationResponse> publishVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationAdminService.publishVersion(simulationId, versionNumber));
    }

    /**
     * Faz um diagnóstico de prontidão da prova para a integração Gupy.
     *
     * <p>Verifica a configuração pública, o token de integração e a estrutura
     * do teste, ajudando a identificar bloqueios antes de publicar. Funciona
     * tanto para rascunhos quanto para versões publicadas.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @return o diagnóstico com eventuais pendências de integração
     */
    @GetMapping("/{simulationId}/versions/{versionNumber}/gupy-preflight")
    @Operation(
            summary = "Executa preflight Gupy",
            description = "Valida configuracao publica, token de integracao e estrutura do teste. "
                    + "Disponivel para versoes em rascunho ou publicadas, permitindo diagnosticar bloqueios antes de publicar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preflight executado."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<GupyPreflightResponse> runGupyPreflight(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(gupyPreflightService.getPreflight(simulationId, versionNumber));
    }

    /**
     * Mostra os indicadores de uma versão publicada (monitoramento).
     *
     * <p>Reúne números de execução, abandono e entrega de resultados para a
     * equipe acompanhar o desempenho da prova no ar.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @return os indicadores de acompanhamento da versão
     */
    @GetMapping("/{simulationId}/versions/{versionNumber}/monitoring")
    @Operation(
            summary = "Monitora versao publicada",
            description = "Retorna indicadores de execucao, abandono e entrega de resultados para uma versao de simulacao."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores retornados."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationMonitoringResponse> monitorVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationMonitoringService.getMonitoring(simulationId, versionNumber));
    }

    /**
     * Compara candidatos entre si e contra o perfil ideal da vaga.
     *
     * <p>Mostra o referencial (benchmark) da vaga e o desempenho por
     * competência de até 5 candidatos da mesma versão, lado a lado. Aceita o
     * modo "às cegas" para reduzir viés ocultando a identificação.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @param attemptIds os candidatos (participações) a comparar
     * @param blind quando verdadeiro, oculta os dados que identificam os candidatos
     * @return o comparativo de competências entre os candidatos selecionados
     */
    @GetMapping("/{simulationId}/versions/{versionNumber}/talent-match")
    @Operation(
            summary = "Compara candidatos contra benchmark",
            description = "Retorna benchmark da vaga e resultados por competência de até 5 tentativas da mesma versão."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comparativo retornado."),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado a uma tentativa selecionada.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Versão ou tentativa não encontrada.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<TalentMatchResponse> getTalentMatch(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @RequestParam List<String> attemptIds,
            @RequestParam(name = "blind", defaultValue = "false") boolean blind
    ) {
        return ResponseEntity.ok(talentMatchService.getTalentMatch(simulationId, versionNumber, attemptIds, blind));
    }

    /**
     * Exclui definitivamente uma prova e todas as suas versões.
     *
     * <p>Ação irreversível. Não é permitida se houver candidatos (tentativas)
     * vinculados à prova, para preservar o histórico das avaliações.</p>
     *
     * @param simulationId identificador da prova a remover
     * @return confirmação sem conteúdo
     */
    @DeleteMapping("/{simulationId}")
    @Operation(
            summary = "Remove simulacao",
            description = "Exclui definitivamente a simulacao e suas versoes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Simulacao removida."),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Teste não encontrado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Simulacao possui tentativas de candidatos vinculadas.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<Void> deleteSimulation(@PathVariable String simulationId) {
        simulationAdminService.deleteSimulation(simulationId);
        return ResponseEntity.noContent().build();
    }
}
