package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ReliabilityLevel;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.model.ScoreCalculationResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Máquina de estados da {@link CandidateAttempt}. Concentra as transições
 * (NOT_STARTED → IN_PROGRESS → COMPLETED, além de EXPIRED/ABANDONED) e o disparo do
 * scoring determinístico na conclusão, mantendo a auditoria de cada transição. Cada método é uma
 * função que devolve um novo agregado imutável.
 */
@Service
public class AttemptStateMachine {

    private final PraxisProperties praxisProperties;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;
    private final ResultScoringService resultScoringService;

    public AttemptStateMachine(
            PraxisProperties praxisProperties,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata,
            ResultScoringService resultScoringService
    ) {
        this.praxisProperties = praxisProperties;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
        this.resultScoringService = resultScoringService;
    }

    public boolean isTerminalBlocked(AttemptStatus status) {
        return status == AttemptStatus.ABANDONED || status == AttemptStatus.EXPIRED || status == AttemptStatus.FAILED;
    }

    public CandidateAttempt expireIfNeeded(CandidateAttempt attempt) {
        Instant now = Instant.now();

        if (attempt.status() == AttemptStatus.NOT_STARTED
                && attempt.createdAt().plusSeconds(praxisProperties.attemptLinkTtlHours() * 3600L).isBefore(now)) {
            auditEventService.appendCandidateAttemptEvent(
                    attempt.tenantId(),
                    attempt.id(),
                    AuditEventType.ATTEMPT_EXPIRED,
                    "Link da tentativa expirou antes do início.",
                    auditMetadata.of("expiredAt", now)
            );
            return blocked(attempt, AttemptStatus.EXPIRED, now);
        }

        if ((attempt.status() == AttemptStatus.IN_PROGRESS || attempt.status() == AttemptStatus.PAUSED)
                && attempt.startedAt() != null
                && attempt.startedAt().plusSeconds(praxisProperties.attemptSessionTtlHours() * 3600L).isBefore(now)) {
            auditEventService.appendCandidateAttemptEvent(
                    attempt.tenantId(),
                    attempt.id(),
                    AuditEventType.ATTEMPT_ABANDONED,
                    "Sessão da tentativa expirou sem conclusão.",
                    auditMetadata.of("abandonedAt", now)
            );
            return blocked(attempt, AttemptStatus.ABANDONED, now);
        }

        return attempt;
    }

    public CandidateAttempt startIfNeeded(CandidateAttempt attempt) {
        if (attempt.status() != AttemptStatus.NOT_STARTED) {
            return attempt;
        }

        Instant startedAt = Instant.now();
        auditEventService.appendCandidateAttemptEvent(
                attempt.tenantId(),
                attempt.id(),
                AuditEventType.ATTEMPT_STARTED,
                "Tentativa iniciada pela pessoa participante.",
                auditMetadata.of("startedAt", startedAt)
        );

        return copy(
                attempt,
                AttemptStatus.IN_PROGRESS,
                attempt.score(),
                attempt.results(),
                attempt.answersByNodeId(),
                attempt.servedAtByNodeId(),
                attempt.decision(),
                attempt.humanReviewRequired(),
                attempt.reliabilityLevel(),
                attempt.companyResultString(),
                startedAt,
                attempt.finishedAt()
        );
    }

    /**
     * Aplica uma resposta e, quando o fluxo chegou ao fim, dispara o scoring
     * determinístico e deriva a decisão.
     */
    public CandidateAttempt applyAnswer(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            Map<String, AttemptAnswer> answersByNodeId,
            ScenarioOption selectedOption,
            boolean reachedEnd
    ) {
        boolean completed = reachedEnd || (selectedOption != null && selectedOption.nextNodeId() == null);

        if (!completed) {
            return copy(
                    attempt,
                    AttemptStatus.IN_PROGRESS,
                    attempt.score(),
                    attempt.results(),
                    answersByNodeId,
                    attempt.servedAtByNodeId(),
                    ResultDecision.IN_PROGRESS,
                    attempt.humanReviewRequired(),
                    attempt.reliabilityLevel(),
                    attempt.companyResultString(),
                    attempt.startedAt(),
                    attempt.finishedAt()
            );
        }

        ScoreCalculationResult scoreResult = resultScoringService.calculate(
                simulation,
                answersByNodeId,
                attempt.startedAt(),
                attempt.servedAtByNodeId()
        );
        return copy(
                attempt,
                AttemptStatus.COMPLETED,
                scoreResult.score(),
                scoreResult.resultItems(),
                answersByNodeId,
                attempt.servedAtByNodeId(),
                scoreResult.decision(),
                scoreResult.humanReviewRequired(),
                scoreResult.reliabilityLevel(),
                buildCompanyResultString(simulation, scoreResult, answersByNodeId),
                attempt.startedAt(),
                Instant.now()
        );
    }

    private String buildCompanyResultString(
            PublishedSimulation simulation,
            ScoreCalculationResult scoreResult,
            Map<String, AttemptAnswer> answersByNodeId
    ) {
        String outcome = switch (scoreResult.decision()) {
            case RECOMMEND_INTERVIEW -> "Atingiu a referência configurada";
            case REVIEW_REQUIRED -> "Requer análise da equipe responsável";
            case NO_RECOMMENDATION -> "Participação concluída";
            case IN_PROGRESS -> "Participação concluída";
        };
        String reviewLine = scoreResult.humanReviewRequired()
                ? "Resposta marcada como crítica e sinalizada para análise da equipe responsável."
                : "Nenhum ponto crítico identificado nas respostas.";
        String responseTimeSignal = responseTimeSignalLabel(scoreResult.reliabilityLevel());
        String terminalReport = terminalReport(simulation, answersByNodeId);

        String result = "Resumo da participação\n\n"
                + "Teste: " + simulation.name() + "\n\n"
                + "Resultado em relação aos critérios: " + outcome + "\n\n"
                + "Pontuação geral: " + scoreResult.score() + "/100\n\n"
                + reviewLine + "\n\n"
                + "Observação sobre o tempo de resposta: " + responseTimeSignal + ".";
        if (terminalReport == null || terminalReport.isBlank()) {
            return result;
        }
        return result + "\n\nResumo do caminho percorrido:\n" + terminalReport;
    }

    private String terminalReport(PublishedSimulation simulation, Map<String, AttemptAnswer> answersByNodeId) {
        String currentNodeId = simulation.rootNodeId();
        while (currentNodeId != null) {
            ScenarioNode currentNode = findNode(simulation, currentNodeId);
            if (currentNode.isFinal()) {
                return currentNode.reportText();
            }

            AttemptAnswer answer = answersByNodeId.get(currentNodeId);
            if (answer == null) {
                return null;
            }
            if (answer.timedOut() || answer.optionId() == null) {
                currentNodeId = currentNode.timeoutNextNodeId();
                continue;
            }

            ScenarioOption pickedOption = currentNode.options().stream()
                    .filter(option -> option.id().equals(answer.optionId()))
                    .findFirst()
                    .orElse(null);
            currentNodeId = pickedOption == null ? null : pickedOption.nextNodeId();
        }
        return null;
    }

    private ScenarioNode findNode(PublishedSimulation simulation, String nodeId) {
        return simulation.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .findFirst()
                .orElseThrow();
    }

    private String responseTimeSignalLabel(ReliabilityLevel reliabilityLevel) {
        return switch (reliabilityLevel) {
            case LOW_RELIABILITY -> "uma ou mais respostas foram concluídas abaixo do intervalo de referência configurado";
            case NORMAL -> "nenhum alerta identificado";
        };
    }

    private CandidateAttempt blocked(CandidateAttempt attempt, AttemptStatus status, Instant finishedAt) {
        return copy(
                attempt,
                status,
                attempt.score(),
                attempt.results(),
                attempt.answersByNodeId(),
                attempt.servedAtByNodeId(),
                attempt.decision(),
                attempt.humanReviewRequired(),
                attempt.reliabilityLevel(),
                attempt.companyResultString(),
                attempt.startedAt(),
                finishedAt
        );
    }

    private CandidateAttempt copy(
            CandidateAttempt attempt,
            AttemptStatus status,
            Integer score,
            List<ResultItem> results,
            Map<String, AttemptAnswer> answersByNodeId,
            Map<String, Instant> servedAtByNodeId,
            ResultDecision decision,
            boolean humanReviewRequired,
            ReliabilityLevel reliabilityLevel,
            String companyResultString,
            Instant startedAt,
            Instant finishedAt
    ) {
        return new CandidateAttempt(
                attempt.id(),
                attempt.resultId(),
                attempt.tenantId(),
                attempt.companyId(),
                attempt.simulationId(),
                attempt.simulationVersionId(),
                attempt.simulationVersionNumber(),
                attempt.idempotencyKey(),
                attempt.candidateName(),
                attempt.candidateEmail(),
                status,
                score,
                results,
                answersByNodeId,
                servedAtByNodeId,
                decision,
                humanReviewRequired,
                reliabilityLevel,
                attempt.accommodationTimeMultiplier(),
                companyResultString,
                attempt.createdAt(),
                startedAt,
                finishedAt
        );
    }
}
