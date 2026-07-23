package br.com.iforce.praxis.results.service;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.results.dto.ResultExecutiveReportResponse;
import br.com.iforce.praxis.results.dto.SaveInterviewGuideRequest;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Compõe o relatório executivo e o roteiro de entrevista sem IA generativa.
 *
 * <p>Todos os textos são produzidos por regras fixas ou recuperados das interpretações
 * cadastradas na versão exata da avaliação. O roteiro revisado pelo entrevistador é
 * persistido como evento append-only da tentativa.</p>
 */
@Service
public class ResultExecutiveService {

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationRepository simulationRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final AuditEventService auditEventService;
    private final CurrentEmpresaService currentEmpresaService;
    private final ObjectMapper objectMapper;

    public ResultExecutiveService(
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationRepository simulationRepository,
            SimulationCatalogService simulationCatalogService,
            AuditEventService auditEventService,
            CurrentEmpresaService currentEmpresaService,
            ObjectMapper objectMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationRepository = simulationRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.auditEventService = auditEventService;
        this.currentEmpresaService = currentEmpresaService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ResultExecutiveReportResponse get(String attemptId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        CandidateAttemptEntity attempt = requiredAttempt(empresaId, attemptId);
        PublishedSimulation simulation = resolveSimulation(attempt, empresaId).orElse(null);
        List<AuditEventResponse> auditEvents = auditEventService.listCandidateAttemptEvents(attemptId);
        List<ResultExecutiveReportResponse.Evidence> evidence = evidence(attempt, simulation);
        List<ResultExecutiveReportResponse.CompetencyInsight> competencies = competencies(attempt, evidence);
        List<ResultExecutiveReportResponse.InterviewQuestion> generatedQuestions = generatedQuestions(competencies);
        ResultExecutiveReportResponse.InterviewGuide interviewGuide = latestGuide(auditEvents, generatedQuestions);

        String simulationTitle = simulation == null
                ? simulationRepository.findByEmpresaIdAndId(empresaId, attempt.getSimulationId())
                        .map(SimulationEntity::getName)
                        .orElse("Avaliação")
                : simulation.name();

        return new ResultExecutiveReportResponse(
                attempt.getId(),
                simulationTitle,
                attempt.getSimulationVersionNumber(),
                Instant.now(),
                new ResultExecutiveReportResponse.ExecutiveSummary(
                        competencies,
                        evidence.stream().filter(ResultExecutiveReportResponse.Evidence::critical).toList(),
                        competencies.stream()
                                .filter(competency -> competency.score() < 80)
                                .map(ResultExecutiveReportResponse.CompetencyInsight::name)
                                .toList(),
                        limitations(evidence)
                ),
                interviewGuide,
                auditEvents.stream()
                        .map(event -> new ResultExecutiveReportResponse.AuditEntry(
                                event.eventType().getDescricao(),
                                event.message(),
                                event.createdAt()
                        ))
                        .toList()
        );
    }

    @Transactional
    public void saveInterviewGuide(String attemptId, SaveInterviewGuideRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        requiredAttempt(empresaId, attemptId);

        List<SaveInterviewGuideRequest.Question> questions = Optional.ofNullable(request.questions())
                .orElse(List.of())
                .stream()
                .map(this::normalize)
                .toList();
        validateUniqueIds(questions);

        String savedBy = currentActor();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("savedBy", savedBy);
        metadata.put("interviewerNotes", normalizeNullable(request.interviewerNotes()));
        metadata.put("questions", questions);

        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                attemptId,
                AuditEventType.INTERVIEW_GUIDE_RECORDED,
                "Roteiro de entrevista revisado e registrado.",
                serialize(metadata)
        );
    }

    private CandidateAttemptEntity requiredAttempt(String empresaId, String attemptId) {
        return candidateAttemptRepository.findByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado não encontrado."));
    }

    private Optional<PublishedSimulation> resolveSimulation(CandidateAttemptEntity attempt, String empresaId) {
        if (attempt.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(attempt.getSimulationVersionId());
        }
        return simulationCatalogService.findPublishedById(empresaId, attempt.getSimulationId());
    }

    private List<ResultExecutiveReportResponse.Evidence> evidence(
            CandidateAttemptEntity attempt,
            PublishedSimulation simulation
    ) {
        if (simulation == null) {
            return List.of();
        }

        Map<String, ScenarioNode> nodes = simulation.nodes().stream()
                .collect(Collectors.toMap(
                        ScenarioNode::id,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<ResultExecutiveReportResponse.Evidence> evidence = new ArrayList<>();
        List<AttemptAnswerEntity> orderedAnswers = attempt.getAnswers().stream()
                .sorted(Comparator.comparing(AttemptAnswerEntity::getAnsweredAt))
                .toList();

        for (AttemptAnswerEntity answer : orderedAnswers) {
            ScenarioNode node = nodes.get(answer.getNodeId());
            if (node == null) {
                continue;
            }
            ScenarioOption option = node.options().stream()
                    .filter(candidate -> Objects.equals(candidate.id(), answer.getOptionId()))
                    .findFirst()
                    .orElse(null);
            if (option == null) {
                continue;
            }

            String reference = node.id() + ":" + option.id();
            evidence.add(new ResultExecutiveReportResponse.Evidence(
                    reference,
                    "Situação " + node.turnIndex(),
                    answer.isTimedOut() ? "Tempo esgotado" : option.text(),
                    firstNonBlank(option.auditNote(), node.reportText()),
                    option.critical(),
                    option.competencyScores() == null ? Map.of() : Map.copyOf(option.competencyScores())
            ));
        }
        return List.copyOf(evidence);
    }

    private List<ResultExecutiveReportResponse.CompetencyInsight> competencies(
            CandidateAttemptEntity attempt,
            List<ResultExecutiveReportResponse.Evidence> evidence
    ) {
        return attempt.getResultItems().stream()
                .sorted(Comparator.comparing(ResultItemEntity::getName))
                .map(item -> {
                    List<String> references = evidence.stream()
                            .filter(entry -> entry.competencyScores().containsKey(item.getName()))
                            .map(ResultExecutiveReportResponse.Evidence::reference)
                            .toList();
                    String level = competencyLevel(item.getScore());
                    return new ResultExecutiveReportResponse.CompetencyInsight(
                            item.getName(),
                            item.getScore(),
                            level,
                            competencyInterpretation(item.getName(), level),
                            references.size(),
                            references
                    );
                })
                .toList();
    }

    private List<ResultExecutiveReportResponse.InterviewQuestion> generatedQuestions(
            List<ResultExecutiveReportResponse.CompetencyInsight> competencies
    ) {
        List<ResultExecutiveReportResponse.InterviewQuestion> questions = new ArrayList<>();
        for (int index = 0; index < competencies.size(); index++) {
            ResultExecutiveReportResponse.CompetencyInsight competency = competencies.get(index);
            String reference = competency.evidenceReferences().stream().findFirst().orElse(null);
            questions.add(new ResultExecutiveReportResponse.InterviewQuestion(
                    "rule-" + (index + 1),
                    competency.name(),
                    questionFor(competency, reference),
                    reference == null ? "RULE" : "EVIDENCE",
                    reference
            ));
        }
        return List.copyOf(questions);
    }

    private String questionFor(ResultExecutiveReportResponse.CompetencyInsight competency, String reference) {
        String context = reference == null ? "o contexto da vaga" : "a evidência " + reference;
        return switch (competency.level()) {
            case "ALTO" -> "Descreva uma situação recente em que você demonstrou " + competency.name()
                    + ". Quais ações e resultados concretos sustentam essa percepção em relação a " + context + "?";
            case "MEDIO" -> "Aprofunde como você aplicaria " + competency.name()
                    + " em uma situação semelhante a " + context + ". O que faria primeiro e por quê?";
            default -> "Que abordagem diferente você adotaria para demonstrar " + competency.name()
                    + " em uma situação semelhante a " + context + "?";
        };
    }

    private ResultExecutiveReportResponse.InterviewGuide latestGuide(
            List<AuditEventResponse> events,
            List<ResultExecutiveReportResponse.InterviewQuestion> generatedQuestions
    ) {
        Optional<AuditEventResponse> latest = events.stream()
                .filter(event -> event.eventType() == AuditEventType.INTERVIEW_GUIDE_RECORDED)
                .max(Comparator.comparing(AuditEventResponse::createdAt));
        if (latest.isEmpty()) {
            return new ResultExecutiveReportResponse.InterviewGuide(
                    generatedQuestions,
                    null,
                    false,
                    null,
                    null
            );
        }

        try {
            AuditEventResponse event = latest.get();
            JsonNode metadata = objectMapper.readTree(event.metadata());
            List<ResultExecutiveReportResponse.InterviewQuestion> questions = new ArrayList<>();
            JsonNode questionsNode = metadata.path("questions");
            if (questionsNode.isArray()) {
                for (JsonNode question : questionsNode) {
                    questions.add(new ResultExecutiveReportResponse.InterviewQuestion(
                            text(question, "id"),
                            text(question, "competency"),
                            text(question, "question"),
                            text(question, "sourceType"),
                            text(question, "evidenceReference")
                    ));
                }
            }
            return new ResultExecutiveReportResponse.InterviewGuide(
                    List.copyOf(questions),
                    text(metadata, "interviewerNotes"),
                    true,
                    text(metadata, "savedBy"),
                    event.createdAt()
            );
        } catch (Exception exception) {
            return new ResultExecutiveReportResponse.InterviewGuide(
                    generatedQuestions,
                    null,
                    false,
                    null,
                    null
            );
        }
    }

    private List<String> limitations(List<ResultExecutiveReportResponse.Evidence> evidence) {
        List<String> limitations = new ArrayList<>();
        limitations.add("A leitura considera somente as situações percorridas nesta versão da avaliação.");
        limitations.add("Pontuações e evidências apoiam a entrevista, mas não determinam contratação, reprovação ou ranking.");
        if (evidence.isEmpty()) {
            limitations.add("Não foi possível reconstruir evidências textuais do percurso para esta tentativa.");
        }
        if (evidence.stream().anyMatch(entry -> entry.configuredInterpretation() == null)) {
            limitations.add("Há escolhas sem interpretação cadastrada pelo autor da avaliação.");
        }
        return List.copyOf(limitations);
    }

    private SaveInterviewGuideRequest.Question normalize(SaveInterviewGuideRequest.Question question) {
        return new SaveInterviewGuideRequest.Question(
                question.id().trim(),
                normalizeNullable(question.competency()),
                question.question().trim(),
                question.sourceType().trim().toUpperCase(),
                normalizeNullable(question.evidenceReference())
        );
    }

    private void validateUniqueIds(List<SaveInterviewGuideRequest.Question> questions) {
        Set<String> ids = new HashSet<>();
        for (SaveInterviewGuideRequest.Question question : questions) {
            if (!ids.add(question.id())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O roteiro contém perguntas duplicadas.");
            }
        }
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "usuário autenticado";
        }
        return authentication.getName();
    }

    private String serialize(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível registrar o roteiro de entrevista."
            );
        }
    }

    private String competencyLevel(int score) {
        if (score >= 80) {
            return "ALTO";
        }
        if (score >= 60) {
            return "MEDIO";
        }
        return "BAIXO";
    }

    private String competencyInterpretation(String name, String level) {
        return switch (level) {
            case "ALTO" -> "A faixa de pontuação indica evidência forte de " + name + ".";
            case "MEDIO" -> "A faixa de pontuação indica evidência intermediária de " + name + ".";
            default -> "A faixa de pontuação indica necessidade de aprofundar " + name + " na entrevista.";
        };
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
