package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.dto.TestResultItemResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
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
        AttemptView view = new AttemptView(
                attempt.id(),
                attempt.empresaId(),
                attempt.status(),
                attempt.companyResultString(),
                attempt.finishedAt(),
                attempt.results().stream()
                        .map(item -> new ResultView(item.name(), item.score(), item.tier().getDescricao()))
                        .toList()
        );
        return toResponse(view, simulation);
    }

    public TestResultResponse toResponse(CandidateAttemptEntity attempt, PublishedSimulation simulation) {
        AttemptView view = new AttemptView(
                attempt.getId(),
                attempt.getEmpresaId(),
                attempt.getStatus(),
                attempt.getCompanyResultString(),
                attempt.getFinishedAt(),
                attempt.getResultItems().stream()
                        .map(item -> new ResultView(item.getName(), item.getScore(), item.getTier().getDescricao()))
                        .toList()
        );
        return toResponse(view, simulation);
    }

    private TestResultResponse toResponse(AttemptView attempt, PublishedSimulation simulation) {
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
                                .sorted(Comparator.comparing(ResultView::name))
                                .map(result -> toItemResponse(
                                        result.name(),
                                        result.score(),
                                        result.tier(),
                                        attempt.finishedAt()
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

    private record AttemptView(
            String id,
            String empresaId,
            AttemptStatus status,
            String companyResultString,
            Instant finishedAt,
            List<ResultView> results
    ) {
    }

    private record ResultView(String name, int score, String tier) {
    }
}
