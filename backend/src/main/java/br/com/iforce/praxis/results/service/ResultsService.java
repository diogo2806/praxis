package br.com.iforce.praxis.results.service;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.candidate.dto.RegisterDispositionRequest;

import br.com.iforce.praxis.candidate.service.CandidateDispositionService;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;

import br.com.iforce.praxis.gupy.model.ScenarioNode;

import br.com.iforce.praxis.gupy.model.ScenarioOption;

import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.gupy.service.SimulationCatalogService;

import br.com.iforce.praxis.results.dto.RegisterResultDecisionRequest;

import br.com.iforce.praxis.results.dto.ResultDetailResponse;

import br.com.iforce.praxis.results.dto.ResultListItemResponse;

import br.com.iforce.praxis.results.dto.ResultsPageResponse;

import br.com.iforce.praxis.results.dto.ResultsSummaryResponse;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;

import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;

import br.com.iforce.praxis.audit.service.AuditEventService;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.Expression;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.util.ArrayList;

import java.util.Comparator;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Locale;

import java.util.Map;

import java.util.Optional;

import java.util.function.Function;

import java.util.stream.Collectors;


/**
 * Motor da Central de Resultados.
 *
 * <p>Depois que um candidato faz uma avaliação, tudo o que ele respondeu, quanto
 * pontuou e onde se destacou passa a ser acompanhado aqui. Esta classe reúne,
 * filtra e apresenta essas avaliações para o time de recrutamento e guarda a
 * decisão final tomada por uma pessoa (avançar, reprovar, contratar ou deixar em
 * espera).</p>
 *
 * <p>Importante: aqui a plataforma apenas <em>organiza e mostra</em> avaliações que
 * já aconteceram. Ela não aplica a prova nem recalcula a nota do candidato — esse
 * cálculo é feito no momento da avaliação. O objetivo é dar ao recrutador uma visão
 * comparável de todos os candidatos para que a decisão de contratação continue sendo
 * humana.</p>
 */
@Service
public class ResultsService {

    /** Teto de candidatos exibidos por página, para não sobrecarregar a tela. */
    private static final int MAX_PAGE_SIZE = 100;

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationRepository simulationRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final AuditEventService auditEventService;
    private final CandidateDispositionService candidateDispositionService;
    private final CurrentEmpresaService currentEmpresaService;
    private final ObjectMapper objectMapper;

    public ResultsService(
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationRepository simulationRepository,
            SimulationCatalogService simulationCatalogService,
            AuditEventService auditEventService,
            CandidateDispositionService candidateDispositionService,
            CurrentEmpresaService currentEmpresaService,
            ObjectMapper objectMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationRepository = simulationRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.auditEventService = auditEventService;
        this.candidateDispositionService = candidateDispositionService;
        this.currentEmpresaService = currentEmpresaService;
        this.objectMapper = objectMapper;
    }

    /**
     * Monta a lista de candidatos avaliados que o recrutador vê na Central de Resultados.
     *
     * <p>Aplica os filtros escolhidos na tela e devolve tanto a página atual de
     * candidatos quanto o resumo que aparece no topo (quantos concluíram, quantos
     * ainda estão em andamento, quantos expiraram e a nota média). A lista vem
     * ordenada dos mais recentes para os mais antigos e é entregue em páginas, para
     * funcionar bem mesmo com muitos candidatos.</p>
     *
     * @param search texto para buscar por nome ou e-mail do candidato (opcional)
     * @param simulationId filtra por uma avaliação específica (opcional)
     * @param status filtra pela situação da avaliação — ex.: concluída, em andamento (opcional)
     * @param integrationProvider filtra pela origem do candidato — Manual, Gupy, Recrutei ou API (opcional)
     * @param periodStart considera apenas avaliações a partir desta data (opcional)
     * @param periodEnd considera apenas avaliações até esta data (opcional)
     * @param page número da página desejada, começando em zero
     * @param size quantos candidatos por página (limitado a 100)
     * @return a página de resultados com os candidatos, o resumo do topo e os dados de paginação
     */
    @Transactional(readOnly = true)
    public ResultsPageResponse list(
            String search,
            String simulationId,
            AttemptStatus status,
            String integrationProvider,
            Instant periodStart,
            Instant periodEnd,
            int page,
            int size
    ) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Specification<CandidateAttemptEntity> spec = buildSpec(
                empresaId,
                search,
                simulationId,
                status,
                integrationProvider,
                periodStart,
                periodEnd
        );

        Page<CandidateAttemptEntity> resultPage = candidateAttemptRepository.findAll(
                spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<CandidateAttemptEntity> filtered = candidateAttemptRepository.findAll(spec);
        Map<String, String> simulationTitles = simulationTitles(empresaId);

        return new ResultsPageResponse(
                resultPage.getContent().stream()
                        .map(attempt -> toListItem(attempt, simulationTitles))
                        .toList(),
                summary(filtered),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    /**
     * Abre o resultado completo de um candidato em uma avaliação.
     *
     * <p>Reúne, em um só lugar, tudo o que o recrutador precisa para analisar o
     * candidato: seus dados, qual avaliação fez, a situação, a nota geral, o
     * desempenho competência por competência, o que ele escolheu em cada situação da
     * prova e a última decisão humana já registrada sobre ele. É a tela de detalhe
     * que embasa a decisão de contratação.</p>
     *
     * @param attemptId identificador da avaliação do candidato
     * @return o detalhe completo do resultado
     * @throws org.springframework.web.server.ResponseStatusException se o resultado não existir para a empresa
     */
    @Transactional(readOnly = true)
    public ResultDetailResponse get(String attemptId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        CandidateAttemptEntity attempt = candidateAttemptRepository.findByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado não encontrado."));
        PublishedSimulation simulation = resolveSimulation(attempt, empresaId).orElse(null);
        String simulationTitle = simulation == null
                ? simulationRepository.findByEmpresaIdAndId(empresaId, attempt.getSimulationId())
                        .map(SimulationEntity::getName)
                        .orElse("Avaliação")
                : simulation.name();
        List<AuditEventResponse> auditTrail = auditEventService.listCandidateAttemptEvents(attemptId);

        return new ResultDetailResponse(
                attempt.getId(),
                new ResultDetailResponse.Candidate(attempt.getCandidateName(), attempt.getCandidateEmail(), null),
                new ResultDetailResponse.Simulation(
                        attempt.getSimulationId(),
                        simulationTitle,
                        attempt.getSimulationVersionNumber()
                ),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getScore(),
                competencies(attempt),
                answers(attempt, simulation),
                latestHumanDecision(auditTrail)
        );
    }

    /**
     * Registra a decisão que uma pessoa tomou sobre o candidato.
     *
     * <p>É aqui que o "humano no controle" fica formalizado: o recrutador diz o que
     * fazer com o candidato (avançar, reprovar, contratar ou deixar em espera) e pode
     * anexar uma observação justificando. A decisão é encaminhada para ser gravada de
     * forma auditável, ficando registrado quem decidiu, o quê e quando.</p>
     *
     * @param attemptId identificador da avaliação do candidato
     * @param request a decisão tomada e uma observação opcional
     */
    @Transactional
    public void registerDecision(String attemptId, RegisterResultDecisionRequest request) {
        candidateDispositionService.register(attemptId, new RegisterDispositionRequest(
                request.decision(),
                request.note()
        ));
    }

    /**
     * Traduz os filtros escolhidos na tela em uma única consulta ao banco.
     *
     * <p>Combina todos os filtros ativos (busca, avaliação, situação, origem e
     * período) e garante que só apareçam candidatos da empresa de quem está logado.
     * Para o filtro de origem, entende "Manual" (sem integração), "API" (integração
     * genérica) ou o nome de um parceiro específico como Gupy/Recrutei.</p>
     */
    private Specification<CandidateAttemptEntity> buildSpec(
            String empresaId,
            String search,
            String simulationId,
            AttemptStatus status,
            String integrationProvider,
            Instant periodStart,
            Instant periodEnd
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("empresaId"), empresaId));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("candidateName")), pattern),
                        cb.like(cb.lower(root.get("candidateEmail")), pattern)
                ));
            }
            if (simulationId != null && !simulationId.isBlank()) {
                predicates.add(cb.equal(root.get("simulationId"), simulationId.trim()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (integrationProvider != null && !integrationProvider.isBlank()) {
                String provider = integrationProvider.trim().toUpperCase(Locale.ROOT);
                if ("MANUAL".equals(provider)) {
                    predicates.add(cb.or(
                            cb.isNull(root.get("resultWebhookUrl")),
                            cb.equal(root.get("resultWebhookUrl"), "")
                    ));
                } else if ("API".equals(provider)) {
                    predicates.add(cb.and(
                            cb.isNotNull(root.get("resultWebhookUrl")),
                            cb.notLike(cb.upper(root.get("resultWebhookUrl")), "%GUPY%"),
                            cb.notLike(cb.upper(root.get("resultWebhookUrl")), "%RECRUTEI%")
                    ));
                } else {
                    predicates.add(cb.like(cb.upper(root.get("resultWebhookUrl")), "%" + provider + "%"));
                }
            }
            if (periodStart != null || periodEnd != null) {
                Expression<Instant> date = cb.coalesce(root.get("finishedAt"), root.get("startedAt"));
                date = cb.coalesce(date, root.get("createdAt"));
                if (periodStart != null) {
                    predicates.add(cb.greaterThanOrEqualTo(date, periodStart));
                }
                if (periodEnd != null) {
                    predicates.add(cb.lessThanOrEqualTo(date, periodEnd));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Resume um candidato em uma linha da lista, com nome da avaliação, nota e competência de destaque. */
    private ResultListItemResponse toListItem(
            CandidateAttemptEntity attempt,
            Map<String, String> simulationTitles
    ) {
        return new ResultListItemResponse(
                attempt.getId(),
                attempt.getCandidateName(),
                attempt.getCandidateEmail(),
                attempt.getSimulationId(),
                simulationTitles.getOrDefault(attempt.getSimulationId(), "Avaliação"),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getScore(),
                highlightCompetency(attempt),
                integrationProvider(attempt)
        );
    }

    /** Busca os nomes das avaliações para que a lista mostre títulos legíveis em vez de códigos internos. */
    private Map<String, String> simulationTitles(String empresaId) {
        return simulationRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId).stream()
                .collect(Collectors.toMap(SimulationEntity::getId, SimulationEntity::getName, (left, right) -> left));
    }

    /**
     * Calcula os números-resumo exibidos no topo da Central.
     *
     * <p>Conta quantos candidatos concluíram, quantos ainda estão em andamento (ou
     * nem começaram / pausaram) e quantos expiraram, além da nota média entre os que
     * concluíram. A média fica em branco quando ninguém concluiu ainda.</p>
     */
    private ResultsSummaryResponse summary(List<CandidateAttemptEntity> attempts) {
        long completed = attempts.stream().filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED).count();
        long inProgress = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.IN_PROGRESS || attempt.getStatus() == AttemptStatus.NOT_STARTED || attempt.getStatus() == AttemptStatus.PAUSED)
                .count();
        long expired = attempts.stream().filter(attempt -> attempt.getStatus() == AttemptStatus.EXPIRED).count();
        List<Integer> scores = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED)
                .map(CandidateAttemptEntity::getScore)
                .filter(score -> score != null)
                .toList();
        Integer averageScore = scores.isEmpty()
                ? null
                : Math.round((float) scores.stream().mapToInt(Integer::intValue).average().orElse(0));
        return new ResultsSummaryResponse(completed, inProgress, expired, averageScore);
    }

    /** Descobre a competência em que o candidato foi melhor, para destacá-la já na lista. */
    private String highlightCompetency(CandidateAttemptEntity attempt) {
        return attempt.getResultItems().stream()
                .max(Comparator.comparingInt(ResultItemEntity::getScore))
                .map(ResultItemEntity::getName)
                .orElse(null);
    }

    /**
     * Detalha o desempenho do candidato competência por competência.
     *
     * <p>Para cada competência avaliada, informa a nota, classifica em nível
     * (alto/médio/baixo) e traz uma frase que ajuda o recrutador a interpretar o
     * resultado sem precisar decorar faixas de pontuação.</p>
     */
    private List<ResultDetailResponse.Competency> competencies(CandidateAttemptEntity attempt) {
        return attempt.getResultItems().stream()
                .sorted(Comparator.comparing(ResultItemEntity::getName))
                .map(item -> new ResultDetailResponse.Competency(
                        item.getName(),
                        item.getScore(),
                        competencyLevel(item.getScore()),
                        competencySummary(item.getName(), item.getScore())
                ))
                .toList();
    }

    /**
     * Reconstrói, situação a situação, o caminho que o candidato percorreu na avaliação.
     *
     * <p>Na ordem em que respondeu, mostra o que foi apresentado e qual alternativa ele
     * escolheu — inclusive marcando quando o tempo esgotou. Quando a versão da avaliação
     * está disponível, apresenta os textos como o candidato os viu; caso contrário, cai
     * em uma versão mais enxuta com os códigos das etapas.</p>
     */
    private List<ResultDetailResponse.Answer> answers(
            CandidateAttemptEntity attempt,
            PublishedSimulation simulation
    ) {
        if (simulation == null) {
            return attempt.getAnswers().stream()
                    .sorted(Comparator.comparing(AttemptAnswerEntity::getAnsweredAt))
                    .map(answer -> new ResultDetailResponse.Answer(
                            "Etapa",
                            answer.getNodeId(),
                            answer.isTimedOut() ? "Tempo esgotado" : answer.getOptionId(),
                            null
                    ))
                    .toList();
        }

        Map<String, ScenarioNode> nodes = simulation.nodes().stream()
                .collect(Collectors.toMap(ScenarioNode::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return attempt.getAnswers().stream()
                .sorted(Comparator.comparing(AttemptAnswerEntity::getAnsweredAt))
                .map(answer -> answerResponse(answer, nodes.get(answer.getNodeId())))
                .toList();
    }

    /** Converte uma resposta técnica em algo legível: a situação, a pergunta, a alternativa escolhida e quantos pontos ela valeu. */
    private ResultDetailResponse.Answer answerResponse(AttemptAnswerEntity answer, ScenarioNode node) {
        if (node == null) {
            return new ResultDetailResponse.Answer(
                    "Etapa",
                    answer.getNodeId(),
                    answer.isTimedOut() ? "Tempo esgotado" : answer.getOptionId(),
                    null
            );
        }

        ScenarioOption option = node.options().stream()
                .filter(candidate -> candidate.id().equals(answer.getOptionId()))
                .findFirst()
                .orElse(null);
        Integer score = option == null || option.competencyScores().isEmpty()
                ? null
                : option.competencyScores().values().stream().mapToInt(Integer::intValue).sum();
        return new ResultDetailResponse.Answer(
                "Situação " + node.turnIndex(),
                node.message(),
                answer.isTimedOut() ? "Tempo esgotado" : option == null ? answer.getOptionId() : option.text(),
                score
        );
    }

    /** Localiza a versão exata da avaliação que o candidato respondeu, para exibir as perguntas como ele as viu. */
    private Optional<PublishedSimulation> resolveSimulation(CandidateAttemptEntity attempt, String empresaId) {
        if (attempt.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(attempt.getSimulationVersionId());
        }
        return simulationCatalogService.findPublishedById(empresaId, attempt.getSimulationId());
    }

    /** Recupera a decisão humana mais recente já registrada sobre o candidato (ou uma decisão vazia, se ainda não houve nenhuma). */
    private ResultDetailResponse.HumanDecision latestHumanDecision(List<AuditEventResponse> auditTrail) {
        return auditTrail.stream()
                .filter(event -> event.eventType() == AuditEventType.HUMAN_DECISION)
                .max(Comparator.comparing(AuditEventResponse::createdAt))
                .map(this::humanDecision)
                .orElse(new ResultDetailResponse.HumanDecision(null, null, null, null));
    }

    /** Extrai do registro de auditoria os dados da decisão: qual foi, quem tomou, quando e por quê. */
    private ResultDetailResponse.HumanDecision humanDecision(AuditEventResponse event) {
        try {
            JsonNode metadata = objectMapper.readTree(event.metadata());
            return new ResultDetailResponse.HumanDecision(
                    text(metadata, "decision"),
                    text(metadata, "decidedByUserId"),
                    event.createdAt(),
                    text(metadata, "reason")
            );
        } catch (Exception exception) {
            return new ResultDetailResponse.HumanDecision(null, null, event.createdAt(), null);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * Identifica por onde o candidato entrou no processo.
     *
     * <p>Diferencia candidatos cadastrados manualmente ("MANUAL") daqueles que chegaram
     * por uma integração — reconhecendo parceiros conhecidos como Gupy e Recrutei, ou
     * tratando o restante como integração via API pública ("API").</p>
     */
    private String integrationProvider(CandidateAttemptEntity attempt) {
        String webhook = attempt.getResultWebhookUrl();
        if (webhook == null || webhook.isBlank()) {
            return "MANUAL";
        }
        String normalized = webhook.toUpperCase(Locale.ROOT);
        if (normalized.contains("GUPY")) {
            return "GUPY";
        }
        if (normalized.contains("RECRUTEI")) {
            return "RECRUTEI";
        }
        return "API";
    }

    /** Classifica uma nota em nível de leitura rápida: alto (80+), médio (60–79) ou baixo. */
    private String competencyLevel(int score) {
        if (score >= 80) {
            return "ALTO";
        }
        if (score >= 60) {
            return "MEDIO";
        }
        return "BAIXO";
    }

    /** Gera a frase que interpreta o desempenho na competência, orientando inclusive quando é preciso análise humana mais cuidadosa. */
    private String competencySummary(String name, int score) {
        return switch (competencyLevel(score)) {
            case "ALTO" -> "Demonstrou forte aderência em " + name + ".";
            case "MEDIO" -> "Apresentou desempenho adequado, com espaço para aprofundar " + name + ".";
            default -> "Requer atenção e análise humana cuidadosa em " + name + ".";
        };
    }
}
