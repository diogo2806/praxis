package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ReliabilityLevel;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import br.com.iforce.praxis.gupy.model.ResultItem;
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
    private final ResultScoringService resultScoringService;

    public AttemptStateMachine(
            PraxisProperties praxisProperties,
            AuditEventService auditEventService,
            ResultScoringService resultScoringService
    ) {
        this.praxisProperties = praxisProperties;
        this.auditEventService = auditEventService;
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
                    "Link da tentativa expirou antes do inicio.",
                    "{\"expiredAt\":\"" + now + "\"}"
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
                    "Sessao da tentativa expirou sem conclusao.",
                    "{\"abandonedAt\":\"" + now + "\"}"
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
                "Tentativa iniciada pelo candidato.",
                "{\"startedAt\":\"" + startedAt + "\"}"
        );

        return copy(
                attempt,
                AttemptStatus.IN_PROGRESS,
                attempt.score(),
                attempt.results(),
                attempt.answersByNodeId(),
                attempt.decision(),
                attempt.humanReviewRequired(),
                attempt.reliabilityLevel(),
                attempt.companyResultString(),
                startedAt,
                attempt.finishedAt()
        );
    }

    /**
     * Aplica uma resposta (ou um timeout, quando {@code selectedOption} é nulo e
     * {@code forceComplete} é verdadeiro) e, se a tentativa concluiu, dispara o scoring
     * determinístico e deriva a decisão.
     */
    public CandidateAttempt applyAnswer(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            Map<String, AttemptAnswer> answersByNodeId,
            ScenarioOption selectedOption,
            boolean forceComplete
    ) {
        boolean completed = forceComplete || (selectedOption != null && selectedOption.nextNodeId() == null);

        if (!completed) {
            return copy(
                    attempt,
                    AttemptStatus.IN_PROGRESS,
                    attempt.score(),
                    attempt.results(),
                    answersByNodeId,
                    ResultDecision.IN_PROGRESS,
                    attempt.humanReviewRequired(),
                    attempt.reliabilityLevel(),
                    attempt.companyResultString(),
                    attempt.startedAt(),
                    attempt.finishedAt()
            );
        }

        ScoreCalculationResult scoreResult = resultScoringService.calculate(simulation, answersByNodeId, attempt.startedAt());
        return copy(
                attempt,
                AttemptStatus.COMPLETED,
                scoreResult.score(),
                scoreResult.resultItems(),
                answersByNodeId,
                scoreResult.decision(),
                scoreResult.humanReviewRequired(),
                scoreResult.reliabilityLevel(),
                buildCompanyResultString(simulation, scoreResult),
                attempt.startedAt(),
                Instant.now()
        );
    }

    private String buildCompanyResultString(PublishedSimulation simulation, ScoreCalculationResult scoreResult) {
        String reviewLine = scoreResult.humanReviewRequired()
                ? "Revisão humana obrigatória: alternativa crítica selecionada."
                : "Sem blocker crítico na trilha respondida.";

        return "# Práxis - Resultado\n\n"
                + "Simulação: " + simulation.name() + "\n\n"
                + "Score geral: " + scoreResult.score() + "/100\n\n"
                + reviewLine + "\n\n"
                + "Confiabilidade comportamental: " + scoreResult.reliabilityLevel() + ".\n\n"
                + "Trilha auditável: " + scoreResult.auditTrail();
    }

    private CandidateAttempt blocked(CandidateAttempt attempt, AttemptStatus status, Instant finishedAt) {
        return copy(
                attempt,
                status,
                attempt.score(),
                attempt.results(),
                attempt.answersByNodeId(),
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
