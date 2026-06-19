package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import br.com.iforce.praxis.gupy.delivery.service.ResultWebhookClient;
import br.com.iforce.praxis.gupy.dto.TestResultItemResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.model.ReliabilityLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter para integração com Gupy.
 * Implementa a interface padrão ATSAdapter para permitir que o Praxis funcione
 * com Gupy através da abstração de adapter.
 */
@Slf4j
@Component
public class GupyAdapter implements ATSAdapter {

    private final ResultWebhookClient resultWebhookClient;

    public GupyAdapter(ResultWebhookClient resultWebhookClient) {
        this.resultWebhookClient = resultWebhookClient;
    }

    @Override
    public CandidateContext createCandidate(CreateCandidateCommand cmd) {
        log.info("Criando candidato na Gupy: candidateId={}, jobId={}", cmd.candidateExternalId(), cmd.jobId());

        // Nota: A lógica de criação de candidato em Gupy já existe em GupyIntegrationController
        // Este adapter apenas padroniza a interface para outros ATSs também usarem
        return new CandidateContext(
            cmd.candidateExternalId(),
            cmd.tenantId(),
            cmd.jobId(),
            cmd.evaluationName(),
            cmd.callbackWebhookUrl(),
            buildEvaluationLink(cmd)
        );
    }

    @Override
    public void pushResult(ResultPayload payload) {
        log.info("Enviando resultado para Gupy: candidateId={}, score={}", payload.candidateId(), payload.score());

        TestResultResponse testResult = new TestResultResponse(
            payload.simulationId(),
            payload.simulationId(),
            "Resultado da avaliacao Praxis",
            "Praxis",
            payload.explanation(),
            null,
            "done",
            null,
            null,
            reliabilityLevel(payload),
            toResultItems(payload)
        );

        try {
            resultWebhookClient.postResult(payload.resultId(), testResult);
        } catch (Exception e) {
            log.error("Falha ao enviar resultado para Gupy: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ATSPlatform type() {
        return ATSPlatform.GUPY;
    }

    private String buildEvaluationLink(CreateCandidateCommand cmd) {
        // Link do candidato será construído quando a tentativa for criada
        // Formato: /candidato/:token
        return null;
    }

    private List<TestResultItemResponse> toResultItems(ResultPayload payload) {
        if (payload.competencies() == null || payload.competencies().isEmpty()) {
            return List.of(new TestResultItemResponse(
                    payload.score(),
                    payload.score() + "%",
                    "percentage",
                    payload.decision(),
                    "Resultado geral",
                    payload.explanation(),
                    null,
                    otherInformations(payload)
            ));
        }

        return payload.competencies().stream()
                .map(competency -> new TestResultItemResponse(
                        competency.score(),
                        competency.score() + "%",
                        "percentage",
                        competency.level(),
                        competency.name(),
                        "Pontuacao da competencia " + competency.name() + ".",
                        null,
                        Map.of()
                ))
                .toList();
    }

    private Map<String, Object> otherInformations(ResultPayload payload) {
        Map<String, Object> otherInformations = new LinkedHashMap<>();
        otherInformations.put("humanReviewRequired", payload.humanReviewRequired());
        putIfPresent(otherInformations, "reliabilityLevel", payload.reliabilityLevel());
        putIfPresent(otherInformations, "candidateId", payload.candidateId());
        putIfPresent(otherInformations, "attemptId", payload.attemptId());
        return otherInformations;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private ReliabilityLevel reliabilityLevel(ResultPayload payload) {
        if (payload.reliabilityLevel() == null || payload.reliabilityLevel().isBlank()) {
            return ReliabilityLevel.NORMAL;
        }
        return ReliabilityLevel.valueOf(payload.reliabilityLevel());
    }
}
