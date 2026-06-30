package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.candidate.dto.EvidenceReport;

import br.com.iforce.praxis.config.PraxisProperties;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;

import br.com.iforce.praxis.gupy.model.ScenarioNode;

import br.com.iforce.praxis.gupy.model.ScenarioOption;

import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.gupy.service.SimulationCatalogService;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.util.ArrayList;

import java.util.Comparator;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.Optional;


/**
 * Monta o relatório de transparência do scoring (REQ-L4). Não recalcula nem altera nada: apenas
 * consolida o que já está persistido (resultado determinístico, caminho do candidato, trilha
 * append-only e decisão humana) em um documento entregável para defesa jurídica e venda.
 */
@Service
public class EvidenceReportService {

    private static final String DETERMINISTIC_STATEMENT =
            "A pontuação é determinística: função pura das opções escolhidas e do grafo "
                    + "publicado. Não há IA, modelo estatístico nem dados de treino. Dado o mesmo "
                    + "cenário e as mesmas respostas, o resultado produzido é idêntico.";

    private static final String SCORING_FORMULA =
            "raw(c) = Σ pontos das opções escolhidas que pontuam em c; "
                    + "max(c) = Σ, nó a nó no mesmo caminho, do maior ponto possível em c; "
                    + "norm(c) = raw(c) / max(c) (ignora c com max == 0); "
                    + "final = round( Σ ( norm(c) × pesoRenormalizado(c) ) × 100 ).";

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final AuditEventService auditEventService;
    private final CurrentEmpresaService currentEmpresaService;
    private final PraxisProperties praxisProperties;
    private final ObjectMapper objectMapper;

    public EvidenceReportService(
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationCatalogService simulationCatalogService,
            AuditEventService auditEventService,
            CurrentEmpresaService currentEmpresaService,
            PraxisProperties praxisProperties,
            ObjectMapper objectMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.auditEventService = auditEventService;
        this.currentEmpresaService = currentEmpresaService;
        this.praxisProperties = praxisProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Monta o relatório de transparência de uma avaliação.
     *
     * <p>Fluxo do processo: localiza a participação do candidato na empresa
     * atual e reúne, em um único documento, tudo o que comprova como o
     * resultado foi obtido — a declaração de que a pontuação é determinística
     * (sem IA), a fórmula usada, os pontos por competência, o caminho que o
     * candidato percorreu, a trilha de auditoria e a decisão humana mais
     * recente. Não recalcula nada: apenas consolida o que já está guardado,
     * gerando uma peça para auditoria, defesa jurídica e transparência.</p>
     *
     * @param attemptId identificador da participação do candidato
     * @return o relatório de transparência consolidado
     */
    @Transactional(readOnly = true)
    public EvidenceReport build(String attemptId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        CandidateAttemptEntity attempt = candidateAttemptRepository.findByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));

        PublishedSimulation simulation = resolveSimulation(attempt, empresaId).orElse(null);
        List<AuditEventResponse> auditTrail = auditEventService.listCandidateAttemptEvents(attemptId);

        EvidenceReport.ScoringDeclaration declaration = new EvidenceReport.ScoringDeclaration(
                true,
                false,
                false,
                DETERMINISTIC_STATEMENT,
                SCORING_FORMULA,
                praxisProperties.recommendInterviewThreshold()
        );

        return new EvidenceReport(
                attempt.getId(),
                attempt.getCandidateName(),
                attempt.getCandidateEmail(),
                attempt.getSimulationId(),
                simulation == null ? null : simulation.name(),
                attempt.getSimulationVersionNumber(),
                attempt.getSimulationVersionId(),
                attempt.getScore(),
                attempt.getDecision(),
                attempt.getReliabilityLevel(),
                attempt.isHumanReviewRequired(),
                attempt.getCompanyResultString(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                declaration,
                buildCompetencies(attempt, simulation),
                buildPath(attempt, simulation),
                latestHumanDecision(auditTrail),
                auditTrail
        );
    }

    /** Descobre qual prova (e qual versão dela) foi aplicada nesta participação. Uso interno. */
    private Optional<PublishedSimulation> resolveSimulation(CandidateAttemptEntity attempt, String empresaId) {
        if (attempt.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(attempt.getSimulationVersionId());
        }
        return simulationCatalogService.findPublishedById(empresaId, attempt.getSimulationId());
    }

    /** Monta a lista de pontos obtidos em cada competência avaliada. Uso interno. */
    private List<EvidenceReport.CompetencyEvidence> buildCompetencies(
            CandidateAttemptEntity attempt,
            PublishedSimulation simulation
    ) {
        Map<String, Double> weights = simulation == null ? Map.of() : simulation.competencyWeights();
        return attempt.getResultItems().stream()
                .sorted(Comparator.comparing(ResultItemEntity::getName))
                .map(item -> new EvidenceReport.CompetencyEvidence(
                        item.getName(),
                        item.getScore(),
                        item.getTier().getDescricao(),
                        weights.get(item.getName())
                ))
                .toList();
    }

    /**
     * Reconstrói o caminho percorrido do nó raiz em diante, seguindo as escolhas (ou a rota de
     * timeout) — a mesma travessia determinística usada no cálculo da pontuação, exposta turno a turno.
     */
    private List<EvidenceReport.PathStepEvidence> buildPath(
            CandidateAttemptEntity attempt,
            PublishedSimulation simulation
    ) {
        if (simulation == null) {
            return List.of();
        }

        Map<String, AttemptAnswerEntity> answersByNodeId = new LinkedHashMap<>();
        attempt.getAnswers().forEach(answer -> answersByNodeId.put(answer.getNodeId(), answer));

        List<EvidenceReport.PathStepEvidence> path = new ArrayList<>();
        String currentNodeId = simulation.rootNodeId();
        while (currentNodeId != null) {
            ScenarioNode node = findNode(simulation, currentNodeId);
            if (node == null || node.isFinal()) {
                break;
            }
            AttemptAnswerEntity answer = answersByNodeId.get(currentNodeId);
            if (answer == null) {
                break;
            }

            if (answer.isTimedOut() || answer.getOptionId() == null) {
                path.add(timeoutStep(node, answer));
                currentNodeId = node.timeoutNextNodeId();
                continue;
            }

            ScenarioOption option = findOption(node, answer.getOptionId());
            if (option == null) {
                break;
            }
            path.add(answeredStep(node, option, answer));
            currentNodeId = option.nextNodeId();
        }
        return path;
    }

    private EvidenceReport.PathStepEvidence timeoutStep(ScenarioNode node, AttemptAnswerEntity answer) {
        return new EvidenceReport.PathStepEvidence(
                node.turnIndex(),
                node.id(),
                node.speaker(),
                node.message(),
                null,
                null,
                true,
                false,
                Map.of(),
                answer.getAnsweredAt()
        );
    }

    private EvidenceReport.PathStepEvidence answeredStep(
            ScenarioNode node,
            ScenarioOption option,
            AttemptAnswerEntity answer
    ) {
        return new EvidenceReport.PathStepEvidence(
                node.turnIndex(),
                node.id(),
                node.speaker(),
                node.message(),
                option.id(),
                option.text(),
                false,
                option.critical(),
                Map.copyOf(option.competencyScores()),
                answer.getAnsweredAt()
        );
    }

    private ScenarioNode findNode(PublishedSimulation simulation, String nodeId) {
        return simulation.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    private ScenarioOption findOption(ScenarioNode node, String optionId) {
        return node.options().stream()
                .filter(option -> option.id().equals(optionId))
                .findFirst()
                .orElse(null);
    }

    /** Encontra, na trilha de auditoria, a decisão humana mais recente sobre o candidato. Uso interno. */
    private EvidenceReport.HumanDecisionEvidence latestHumanDecision(List<AuditEventResponse> auditTrail) {
        return auditTrail.stream()
                .filter(event -> event.eventType() == AuditEventType.HUMAN_DECISION)
                .max(Comparator.comparing(AuditEventResponse::createdAt))
                .map(this::toHumanDecision)
                .orElse(null);
    }

    private EvidenceReport.HumanDecisionEvidence toHumanDecision(AuditEventResponse event) {
        try {
            JsonNode metadata = objectMapper.readTree(event.metadata());
            return new EvidenceReport.HumanDecisionEvidence(
                    text(metadata, "decision"),
                    text(metadata, "decidedByUserId"),
                    text(metadata, "reason"),
                    event.createdAt()
            );
        } catch (Exception exception) {
            // A decisão consta na trilha mesmo se o metadata não puder ser interpretado.
            return new EvidenceReport.HumanDecisionEvidence(null, null, null, event.createdAt());
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
