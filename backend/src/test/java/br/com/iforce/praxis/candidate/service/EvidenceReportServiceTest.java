package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.candidate.dto.EvidenceReport;

import br.com.iforce.praxis.config.PraxisProperties;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;

import br.com.iforce.praxis.gupy.model.ReliabilityLevel;

import br.com.iforce.praxis.gupy.model.ResultDecision;

import br.com.iforce.praxis.gupy.model.ResultTier;

import br.com.iforce.praxis.gupy.model.ScenarioNode;

import br.com.iforce.praxis.gupy.model.ScenarioOption;

import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.gupy.service.SimulationCatalogService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.util.List;

import java.util.Map;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.lenient;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EvidenceReportServiceTest {

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    @Mock
    private SimulationCatalogService simulationCatalogService;

    @Mock
    private AuditEventService auditEventService;

    @Mock
    private CurrentEmpresaService currentEmpresaService;

    @Mock
    private PraxisProperties praxisProperties;

    private EvidenceReportService service;

    @BeforeEach
    void setUp() {
        service = new EvidenceReportService(
                candidateAttemptRepository,
                simulationCatalogService,
                auditEventService,
                currentEmpresaService,
                praxisProperties,
                new ObjectMapper()
        );
        lenient().when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        lenient().when(praxisProperties.recommendInterviewThreshold()).thenReturn(70);
    }

    @Test
    void buildsReportWithDeclarationPathCompetenciesAndHumanDecision() {
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "att_1"))
                .thenReturn(Optional.of(sampleAttempt()));
        when(simulationCatalogService.findByVersionId(100L)).thenReturn(Optional.of(sampleSimulation()));
        when(auditEventService.listCandidateAttemptEvents("att_1")).thenReturn(sampleAuditTrail());

        EvidenceReport report = service.build("att_1");

        assertThat(report.declaration().deterministic()).isTrue();
        assertThat(report.declaration().usesArtificialIntelligence()).isFalse();
        assertThat(report.declaration().usesTrainingData()).isFalse();
        assertThat(report.declaration().recommendInterviewThreshold()).isEqualTo(70);
        assertThat(report.declaration().formula()).contains("norm(c)");

        assertThat(report.generalScore()).isEqualTo(82);
        assertThat(report.decision()).isEqualTo(ResultDecision.RECOMMEND_INTERVIEW);
        assertThat(report.simulationName()).isEqualTo("Atendimento crítico");

        assertThat(report.competencies()).hasSize(2);
        EvidenceReport.CompetencyEvidence comunicacao = report.competencies().stream()
                .filter(competency -> competency.name().equals("Comunicacao"))
                .findFirst().orElseThrow();
        assertThat(comunicacao.score()).isEqualTo(88);
        assertThat(comunicacao.weight()).isEqualTo(0.6);

        assertThat(report.path()).hasSize(2);
        assertThat(report.path().get(0).nodeId()).isEqualTo("n1");
        assertThat(report.path().get(0).answeredOptionId()).isEqualTo("A");
        assertThat(report.path().get(0).competencyPoints()).containsEntry("Comunicacao", 3);
        assertThat(report.path().get(1).nodeId()).isEqualTo("n2");
        assertThat(report.path().get(1).answeredOptionId()).isEqualTo("B");

        assertThat(report.humanDecision()).isNotNull();
        assertThat(report.humanDecision().decision()).isEqualTo("REJECTED");
        assertThat(report.humanDecision().decidedByUserId()).isEqualTo("9");
        assertThat(report.humanDecision().reason()).isEqualTo("Erro crítico não tratado.");
        assertThat(report.auditTrail()).hasSize(2);
    }

    @Test
    void reportWorksWhenSimulationVersionNoLongerResolvable() {
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "att_1"))
                .thenReturn(Optional.of(sampleAttempt()));
        when(simulationCatalogService.findByVersionId(100L)).thenReturn(Optional.empty());
        when(auditEventService.listCandidateAttemptEvents("att_1")).thenReturn(List.of());

        EvidenceReport report = service.build("att_1");

        assertThat(report.simulationName()).isNull();
        assertThat(report.path()).isEmpty();
        assertThat(report.competencies()).hasSize(2);
        assertThat(report.competencies().get(0).weight()).isNull();
        assertThat(report.humanDecision()).isNull();
    }

    @Test
    void rejectsWhenAttemptNotFound() {
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "ghost"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.build("ghost"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    private CandidateAttemptEntity sampleAttempt() {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att_1");
        attempt.setEmpresaId("empresa-1");
        attempt.setSimulationId("sim-1");
        attempt.setSimulationVersionId(100L);
        attempt.setSimulationVersionNumber(2);
        attempt.setCandidateName("Maria");
        attempt.setCandidateEmail("maria@example.com");
        attempt.setScore(82);
        attempt.setDecision(ResultDecision.RECOMMEND_INTERVIEW);
        attempt.setHumanReviewRequired(false);
        attempt.setReliabilityLevel(ReliabilityLevel.NORMAL);
        attempt.setCompanyResultString("# Resumo");
        attempt.setStartedAt(Instant.parse("2026-06-20T10:00:00Z"));
        attempt.setFinishedAt(Instant.parse("2026-06-20T10:05:00Z"));

        attempt.getResultItems().add(resultItem(attempt, "Comunicacao", 88));
        attempt.getResultItems().add(resultItem(attempt, "Resolucao", 80));

        attempt.getAnswers().add(answer(attempt, "n1", "A"));
        attempt.getAnswers().add(answer(attempt, "n2", "B"));
        return attempt;
    }

    private ResultItemEntity resultItem(CandidateAttemptEntity attempt, String name, int score) {
        ResultItemEntity item = new ResultItemEntity();
        item.setCandidateAttempt(attempt);
        item.setName(name);
        item.setScore(score);
        item.setTier(ResultTier.MAJOR);
        return item;
    }

    private AttemptAnswerEntity answer(CandidateAttemptEntity attempt, String nodeId, String optionId) {
        AttemptAnswerEntity answer = new AttemptAnswerEntity();
        answer.setCandidateAttempt(attempt);
        answer.setNodeId(nodeId);
        answer.setOptionId(optionId);
        answer.setTimedOut(false);
        answer.setAnsweredAt(Instant.parse("2026-06-20T10:01:00Z"));
        return answer;
    }

    private PublishedSimulation sampleSimulation() {
        ScenarioOption a = new ScenarioOption("A", "Acolher", "n2", Map.of("Comunicacao", 3, "Resolucao", 2), false, "nota A");
        ScenarioOption c = new ScenarioOption("C", "Ignorar", null, Map.of("Comunicacao", 0, "Resolucao", 0), false, "nota C");
        ScenarioOption b = new ScenarioOption("B", "Resolver", null, Map.of("Comunicacao", 5, "Resolucao", 4), false, "nota B");
        ScenarioOption d = new ScenarioOption("D", "Escalar", null, Map.of("Comunicacao", 1, "Resolucao", 1), false, "nota D");

        ScenarioNode n1 = new ScenarioNode("n1", 1, "Cliente", "Mensagem 1", 60, List.of(a, c));
        ScenarioNode n2 = new ScenarioNode("n2", 2, "Cliente", "Mensagem 2", 60, List.of(b, d));

        return new PublishedSimulation(
                100L,
                2,
                "sim-1",
                "Atendimento crítico",
                "Descrição",
                List.of("Comunicacao", "Resolucao"),
                Map.of("Comunicacao", 0.6, "Resolucao", 0.4),
                Map.of("Comunicacao", ResultTier.MAJOR, "Resolucao", ResultTier.MAJOR),
                "n1",
                List.of(n1, n2)
        );
    }

    private List<AuditEventResponse> sampleAuditTrail() {
        AuditEventResponse answerEvent = new AuditEventResponse(
                1L, "Tentativa do candidato", "att_1", AuditEventType.ANSWER_SUBMITTED,
                "Resposta salva.", "{\"nodeId\":\"n1\"}", Instant.parse("2026-06-20T10:01:00Z"));
        AuditEventResponse decisionEvent = new AuditEventResponse(
                2L, "Tentativa do candidato", "att_1", AuditEventType.HUMAN_DECISION,
                "Decisão humana registrada: REJECTED por usuário 9",
                "{\"attemptId\":\"att_1\",\"decidedByUserId\":\"9\",\"decision\":\"REJECTED\","
                        + "\"reason\":\"Erro crítico não tratado.\",\"decidedAt\":\"2026-06-20T11:00:00Z\"}",
                Instant.parse("2026-06-20T11:00:00Z"));
        return List.of(answerEvent, decisionEvent);
    }
}
