package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.candidate.dto.CandidateNodeResponse;
import br.com.iforce.praxis.candidate.dto.CandidateOptionResponse;
import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tradução entre a entidade JPA {@link CandidateAttemptEntity} e o agregado de domínio
 * {@link CandidateAttempt}, além das views públicas do candidato. Isolar este mapeamento mantém o
 * {@link CandidateAttemptService} focado em orquestração.
 */
@Component
public class CandidateAttemptMapper {

    public CandidateAttemptEntity newEntity(
            String tenantId,
            String idempotencyKey,
            CreateCandidateRequest request,
            PublishedSimulation publishedSimulation
    ) {
        CandidateAttempt initialAttempt = new CandidateAttempt(
                "att_" + randomToken(),
                "res_" + randomToken(),
                tenantId,
                request.companyId().trim(),
                publishedSimulation.id(),
                publishedSimulation.versionId(),
                publishedSimulation.versionNumber(),
                idempotencyKey,
                request.candidateName(),
                request.candidateEmail(),
                AttemptStatus.NOT_STARTED,
                null,
                initialResultItems(publishedSimulation),
                Map.of(),
                ResultDecision.IN_PROGRESS,
                false,
                "Resultado ainda nao finalizado. A trilha auditavel sera preenchida apos a conclusao da simulacao.",
                Instant.now(),
                null,
                null
        );

        CandidateAttemptEntity candidateAttemptEntity = new CandidateAttemptEntity();
        applyDomainToEntity(initialAttempt, candidateAttemptEntity);
        candidateAttemptEntity.setCallbackUrl(request.callbackUrl() == null ? null : request.callbackUrl().toString());
        candidateAttemptEntity.setResultWebhookUrl(request.resultWebhookUrl() == null ? null : request.resultWebhookUrl().toString());
        return candidateAttemptEntity;
    }

    private String randomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private List<ResultItem> initialResultItems(PublishedSimulation publishedSimulation) {
        return publishedSimulation.competencies().stream()
                .map(competency -> new ResultItem(competency, 0, tierFor(competency)))
                .toList();
    }

    private ResultTier tierFor(String competency) {
        return "Aderência à política".equalsIgnoreCase(competency) ? ResultTier.MINOR : ResultTier.MAJOR;
    }

    public void applyDomainToEntity(CandidateAttempt attempt, CandidateAttemptEntity candidateAttemptEntity) {
        candidateAttemptEntity.setId(attempt.id());
        candidateAttemptEntity.setResultId(attempt.resultId());
        candidateAttemptEntity.setTenantId(attempt.tenantId());
        candidateAttemptEntity.setCompanyId(attempt.companyId());
        candidateAttemptEntity.setSimulationId(attempt.simulationId());
        candidateAttemptEntity.setSimulationVersionId(attempt.simulationVersionId());
        candidateAttemptEntity.setSimulationVersionNumber(attempt.simulationVersionNumber());
        candidateAttemptEntity.setIdempotencyKey(attempt.idempotencyKey());
        candidateAttemptEntity.setCandidateName(attempt.candidateName());
        candidateAttemptEntity.setCandidateEmail(attempt.candidateEmail());
        candidateAttemptEntity.setStatus(attempt.status());
        candidateAttemptEntity.setScore(attempt.score());
        candidateAttemptEntity.setDecision(attempt.decision());
        candidateAttemptEntity.setHumanReviewRequired(attempt.humanReviewRequired());
        candidateAttemptEntity.setCompanyResultString(attempt.companyResultString());
        candidateAttemptEntity.setCreatedAt(attempt.createdAt());
        candidateAttemptEntity.setStartedAt(attempt.startedAt());
        candidateAttemptEntity.setFinishedAt(attempt.finishedAt());

        candidateAttemptEntity.getAnswers().clear();
        for (AttemptAnswer answer : attempt.answersByNodeId().values()) {
            AttemptAnswerEntity attemptAnswerEntity = new AttemptAnswerEntity();
            attemptAnswerEntity.setCandidateAttempt(candidateAttemptEntity);
            attemptAnswerEntity.setNodeId(answer.nodeId());
            attemptAnswerEntity.setOptionId(answer.optionId());
            attemptAnswerEntity.setTimedOut(answer.timedOut());
            attemptAnswerEntity.setAnsweredAt(answer.answeredAt());
            candidateAttemptEntity.getAnswers().add(attemptAnswerEntity);
        }

        candidateAttemptEntity.getResultItems().clear();
        for (ResultItem resultItem : attempt.results()) {
            ResultItemEntity resultItemEntity = new ResultItemEntity();
            resultItemEntity.setCandidateAttempt(candidateAttemptEntity);
            resultItemEntity.setName(resultItem.name());
            resultItemEntity.setScore(resultItem.score());
            resultItemEntity.setTier(resultItem.tier());
            candidateAttemptEntity.getResultItems().add(resultItemEntity);
        }
    }

    public CandidateAttempt toDomain(CandidateAttemptEntity candidateAttemptEntity) {
        Map<String, AttemptAnswer> answersByNodeId = new LinkedHashMap<>();
        candidateAttemptEntity.getAnswers().stream()
                .sorted(Comparator.comparing(AttemptAnswerEntity::getAnsweredAt))
                .forEach(answer -> answersByNodeId.put(
                        answer.getNodeId(),
                        new AttemptAnswer(answer.getNodeId(), answer.getOptionId(), answer.isTimedOut(), answer.getAnsweredAt())
                ));

        List<ResultItem> resultItems = candidateAttemptEntity.getResultItems().stream()
                .sorted(Comparator.comparing(ResultItemEntity::getName))
                .map(resultItemEntity -> new ResultItem(
                        resultItemEntity.getName(),
                        resultItemEntity.getScore(),
                        resultItemEntity.getTier()
                ))
                .toList();

        return new CandidateAttempt(
                candidateAttemptEntity.getId(),
                candidateAttemptEntity.getResultId(),
                candidateAttemptEntity.getTenantId(),
                candidateAttemptEntity.getCompanyId(),
                candidateAttemptEntity.getSimulationId(),
                candidateAttemptEntity.getSimulationVersionId(),
                candidateAttemptEntity.getSimulationVersionNumber(),
                candidateAttemptEntity.getIdempotencyKey(),
                candidateAttemptEntity.getCandidateName(),
                candidateAttemptEntity.getCandidateEmail(),
                candidateAttemptEntity.getStatus(),
                candidateAttemptEntity.getScore(),
                resultItems,
                answersByNodeId,
                candidateAttemptEntity.getDecision(),
                candidateAttemptEntity.isHumanReviewRequired(),
                candidateAttemptEntity.getCompanyResultString(),
                candidateAttemptEntity.getCreatedAt(),
                candidateAttemptEntity.getStartedAt(),
                candidateAttemptEntity.getFinishedAt()
        );
    }

    public CandidateNodeResponse toCandidateNodeResponse(ScenarioNode node) {
        if (node == null) {
            return null;
        }

        List<CandidateOptionResponse> options = node.options().stream()
                .map(option -> new CandidateOptionResponse(option.id(), option.text()))
                .toList();

        return new CandidateNodeResponse(
                node.id(),
                node.turnIndex(),
                node.speaker(),
                node.message(),
                node.timeLimitSeconds(),
                options
        );
    }
}
