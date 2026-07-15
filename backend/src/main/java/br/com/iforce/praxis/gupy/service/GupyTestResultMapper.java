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
    private final JwtService jwtService;

    public GupyTestResultMapper(PraxisProperties praxisProperties, JwtService jwtService) {
        this.praxisProperties = praxisProperties;
        this.jwtService = jwtService;
    }

    public TestResultResponse toResponse(CandidateAttempt attempt, PublishedSimulation simulation) {
        assertExternallyRepresentable(attempt.status());
        return new TestResultResponse(
                simulation.name(),
                simulation.id(),
                simulation.description(),
                PROVIDER_NAME,
                attempt.companyResultString(),
                praxisProperties.publicBaseUrl(),
                toGupyStatus(attempt.status()),
                recruiterResultPageUrl(attempt.id()),
                candidateResultPageUrl(attempt.empresaId(), attempt.id()),
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
        assertExternallyRepresentable(attempt.getStatus());
        return new TestResultResponse(
                simulation.name(),
                simulation.id(),
                simulation.description(),
                PROVIDER_NAME,
                attempt.getCompanyResultString(),
                praxisProperties.publicBaseUrl(),
                toGupyStatus(attempt.getStatus()),
                recruiterResultPageUrl(attempt.getId()),
                candidateResultPageUrl(attempt.getEmpresaId(), attempt.getId()),
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

    private void assertExternallyRepresentable(AttemptStatus status) {
        if (status == AttemptStatus.ABANDONED || status == AttemptStatus.EXPIRED) {
            throw new IllegalStateException(
                    "Tentativas abandonadas ou expiradas não possuem resultado final válido para o contrato Gupy."
            );
        }
    }

    private String toGupyStatus(AttemptStatus status) {
        return switch (status) {
            case NOT_STARTED -> "notStarted";
            case COMPLETED -> "done";
            case IN_PROGRESS -> "paused";
            case ABANDONED, EXPIRED -> throw new IllegalStateException(
                    "Tentativas abandonadas ou expiradas não possuem status contratual válido."
            );
        };
    }

    private String recruiterResultPageUrl(String attemptId) {
        return frontendBaseUrl() + "/results/" + attemptId;
    }

    private String candidateResultPageUrl(String empresaId, String attemptId) {
        String token = jwtService.generateCandidateResultToken(
                empresaId,
                attemptId,
                praxisProperties.candidateResultTtlHours()
        );
        return frontendBaseUrl() + "/candidato/" + token + "/resultado";
    }

    private String frontendBaseUrl() {
        String baseUrl = praxisProperties.candidatePageBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
