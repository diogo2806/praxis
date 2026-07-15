package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.dto.TestResultItemResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class GupyTestResultMapper {

    private static final String PROVIDER_NAME = "Praxis";
    private static final String TYPE_RESULT = "percentage";

    private final PraxisProperties praxisProperties;

    public GupyTestResultMapper(PraxisProperties praxisProperties, JwtService jwtService) {
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
                recruiterResultPageUrl(attempt.id()),
                candidateResultPageUrl(attempt.id()),
                attempt.status() == AttemptStatus.COMPLETED
                        ? attempt.results().stream()
                                .sorted(Comparator.comparing(ResultItem::name))
                                .map(resultItem -> toItemResponse(
                                        resultItem.name(),
                                        resultItem.score(),
                                        resultItem.tier().getDescricao(),
                                        attempt.finishedAt()
                                ))
                                .toList()
                        : List.of()
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
                recruiterResultPageUrl(attempt.getId()),
                candidateResultPageUrl(attempt.getId()),
                attempt.getStatus() == AttemptStatus.COMPLETED
                        ? attempt.getResultItems().stream()
                                .sorted(Comparator.comparing(ResultItemEntity::getName))
                                .map(resultItem -> toItemResponse(
                                        resultItem.getName(),
                                        resultItem.getScore(),
                                        resultItem.getTier().getDescricao(),
                                        attempt.getFinishedAt()
                                ))
                                .toList()
                        : List.of()
        );
    }

    private TestResultItemResponse toItemResponse(String title, int score, String tier, Instant finishedAt) {
        return new TestResultItemResponse(
                score,
                score + "%",
                TYPE_RESULT,
                tier,
                title,
                "Pontuação da competência " + title + ".",
                finishedAt == null ? null : finishedAt.toString(),
                Map.of()
        );
    }

    private String toGupyStatus(AttemptStatus status) {
        return switch (status) {
            case NOT_STARTED -> "notStarted";
            case COMPLETED -> "done";
            case IN_PROGRESS, ABANDONED, EXPIRED -> "paused";
        };
    }

    private String recruiterResultPageUrl(String attemptId) {
        return frontendBaseUrl() + "/results/" + attemptId;
    }

    private String candidateResultPageUrl(String attemptId) {
        return frontendBaseUrl() + "/candidato/" + attemptId + "/resultado";
    }

    private String frontendBaseUrl() {
        String baseUrl = praxisProperties.candidatePageBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
