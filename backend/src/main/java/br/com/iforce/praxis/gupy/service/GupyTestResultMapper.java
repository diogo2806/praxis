package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.dto.TestResultItemResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ReliabilityLevel;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;

@Component
public class GupyTestResultMapper {

    private static final String PROVIDER_NAME = "Praxis";
    private static final String TYPE_RESULT = "percentage";

    private final PraxisProperties praxisProperties;

    public GupyTestResultMapper(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

    public TestResultResponse toResponse(CandidateAttempt attempt, PublishedSimulation simulation) {
        return new TestResultResponse(
                simulation.name(),
                simulation.id(),
                simulation.description(),
                PROVIDER_NAME,
                attempt.companyResultString(),
                praxisProperties.publicBaseUrl(),
                toGupyStatus(attempt.status()),
                resultPageUrl(attempt.resultId(), attempt.companyId()),
                candidatePageUrl(attempt.id()),
                attempt.reliabilityLevel() == null ? ReliabilityLevel.NORMAL : attempt.reliabilityLevel(),
                otherInformations(timeoutCount(attempt)),
                attempt.results().stream()
                        .sorted(Comparator.comparing(ResultItem::name))
                        .map(resultItem -> toItemResponse(
                                resultItem.name(),
                                resultItem.score(),
                                resultItem.tier().getDescricao(),
                                attempt.finishedAt()
                        ))
                        .toList()
        );
    }

    public TestResultResponse toResponse(CandidateAttemptEntity attempt, PublishedSimulation simulation) {
        return new TestResultResponse(
                simulation.name(),
                simulation.id(),
                simulation.description(),
                PROVIDER_NAME,
                attempt.getCompanyResultString(),
                praxisProperties.publicBaseUrl(),
                toGupyStatus(attempt.getStatus()),
                resultPageUrl(attempt.getResultId(), attempt.getCompanyId()),
                candidatePageUrl(attempt.getId()),
                attempt.getReliabilityLevel() == null ? ReliabilityLevel.NORMAL : attempt.getReliabilityLevel(),
                otherInformations(timeoutCount(attempt)),
                attempt.getResultItems().stream()
                        .sorted(Comparator.comparing(ResultItemEntity::getName))
                        .map(resultItem -> toItemResponse(
                                resultItem.getName(),
                                resultItem.getScore(),
                                resultItem.getTier().getDescricao(),
                                attempt.getFinishedAt()
                        ))
                        .toList()
        );
    }

    private long timeoutCount(CandidateAttempt attempt) {
        return attempt.answersByNodeId().values().stream()
                .filter(AttemptAnswer::timedOut)
                .count();
    }

    private long timeoutCount(CandidateAttemptEntity attempt) {
        return attempt.getAnswers().stream()
                .filter(AttemptAnswerEntity::isTimedOut)
                .count();
    }

    private Map<String, Object> otherInformations(long timeoutCount) {
        return Map.of(
                "timeout_count", timeoutCount,
                "situational_omission_count", timeoutCount
        );
    }

    private TestResultItemResponse toItemResponse(String title, int score, String tier, Instant finishedAt) {
        return new TestResultItemResponse(
                score,
                score + "%",
                TYPE_RESULT,
                tier,
                title,
                "Pontuacao da competencia " + title + ".",
                finishedAt == null ? null : finishedAt.toString(),
                Map.of()
        );
    }

    private String toGupyStatus(AttemptStatus status) {
        return switch (status) {
            case NOT_STARTED -> "notStarted";
            case COMPLETED, ABANDONED, EXPIRED, FAILED -> "done";
            case IN_PROGRESS, PAUSED -> "paused";
        };
    }

    private String resultPageUrl(String resultId, String companyId) {
        return praxisProperties.publicBaseUrl() + "/test/result/" + resultId
                + "?company_id=" + URLEncoder.encode(companyId, StandardCharsets.UTF_8);
    }

    private String candidatePageUrl(String attemptId) {
        return praxisProperties.publicBaseUrl() + "/candidate/attempts/" + attemptId;
    }
}
