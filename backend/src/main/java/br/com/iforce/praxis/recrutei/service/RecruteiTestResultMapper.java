package br.com.iforce.praxis.recrutei.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.recrutei.dto.RecruteiTestResultItemResponse;
import br.com.iforce.praxis.recrutei.dto.RecruteiTestResultResponse;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;

@Component
public class RecruteiTestResultMapper {

    private static final String PROVIDER_NAME = "Praxis";
    private static final String TYPE_RESULT = "percentage";

    private final PraxisProperties praxisProperties;

    public RecruteiTestResultMapper(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

    public RecruteiTestResultResponse toResponse(CandidateAttempt attempt, PublishedSimulation simulation) {
        return new RecruteiTestResultResponse(
                simulation.name(),
                simulation.id(),
                simulation.description(),
                PROVIDER_NAME,
                toRecruteiStatus(attempt.status()),
                attempt.companyResultString(),
                resultPageUrl(attempt.resultId(), attempt.companyId()),
                candidatePageUrl(attempt.id()),
                attempt.results().stream()
                        .sorted(Comparator.comparing(ResultItem::name))
                        .map(item -> toItemResponse(item.name(), item.score(), item.tier().getDescricao(), attempt.finishedAt()))
                        .toList()
        );
    }

    private RecruteiTestResultItemResponse toItemResponse(String title, int score, String tier, Instant finishedAt) {
        return new RecruteiTestResultItemResponse(
                title,
                "Pontuação da competência " + title + ".",
                score,
                TYPE_RESULT,
                tier,
                finishedAt == null ? null : finishedAt.toString()
        );
    }

    private String toRecruteiStatus(AttemptStatus status) {
        return switch (status) {
            case NOT_STARTED -> "pending";
            case COMPLETED, ABANDONED, EXPIRED, FAILED -> "done";
            case IN_PROGRESS, PAUSED -> "in_progress";
        };
    }

    private String resultPageUrl(String resultId, String companyId) {
        return praxisProperties.publicBaseUrl() + "/recrutei/test/result/" + resultId
                + "?company_id=" + URLEncoder.encode(companyId, StandardCharsets.UTF_8);
    }

    private String candidatePageUrl(String attemptId) {
        return praxisProperties.publicBaseUrl() + "/candidate/attempts/" + attemptId;
    }
}
