package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.dto.ValidationIssueResponse;
import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aplica as convenções usadas pelos editores sobre o diagnóstico estrutural.
 *
 * <p>No contrato atual, uma alternativa sem {@code nextNodeId} encerra a
 * participação e o seletor a apresenta como "Vai para FIM". Esse encerramento
 * direto precisa ter texto de relatório no {@code auditNote} da alternativa.
 * Da mesma forma, uma etapa temporizada sem {@code timeoutNextNodeId} encerra a
 * participação quando o tempo acaba e precisa ter o relatório no próprio nó,
 * enquanto uma etapa sem limite não precisa de transição de timeout.</p>
 */
@Primary
@Service
public class ConsistentSimulationValidationService extends SimulationValidationService {

    private static final String OPTION_WITHOUT_DESTINATION = "Uma resposta está sem destino.";
    private static final String TIMEOUT_WITHOUT_DESTINATION =
            "Esta etapa continua o teste, mas não tem destino para tempo esgotado.";

    private final SimulationPathComparabilityService pathComparabilityService;

    @Autowired
    public ConsistentSimulationValidationService(
            PraxisProperties praxisProperties,
            SimulationPathComparabilityService pathComparabilityService
    ) {
        super(praxisProperties);
        this.pathComparabilityService = pathComparabilityService;
    }

    /** Construtor mantido para testes unitários isolados. */
    public ConsistentSimulationValidationService(PraxisProperties praxisProperties) {
        this(praxisProperties, new SimulationPathComparabilityService());
    }

    @Override
    public SimulationValidationResponse validate(SimulationVersionEntity simulationVersionEntity) {
        SimulationValidationResponse original = super.validate(simulationVersionEntity);
        Map<String, SimulationNodeEntity> nodesById = simulationVersionEntity.getNodes().stream()
                .collect(Collectors.toMap(SimulationNodeEntity::getNodeId, Function.identity(), (first, ignored) -> first));

        List<ValidationIssueResponse> issues = new ArrayList<>(original.issues().stream()
                .filter(issue -> isRelevant(issue, nodesById))
                .toList());
        appendDirectEndReportIssues(simulationVersionEntity, issues);
        appendMediaAccessibilityIssues(simulationVersionEntity, issues);

        SimulationPathComparabilityService.PathComparabilityAnalysis pathAnalysis =
                pathComparabilityService.analyze(simulationVersionEntity);
        issues.addAll(pathAnalysis.issues());

        long blockerCount = issues.stream()
                .filter(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER)
                .count();
        long warningCount = issues.stream()
                .filter(issue -> issue.severity() == ValidationIssueSeverity.WARNING)
                .count();
        int qualityScore = Math.max(0, 100 - (int) blockerCount * 30 - (int) warningCount * 10);

        return new SimulationValidationResponse(
                original.simulationId(),
                original.versionNumber(),
                blockerCount == 0,
                blockerCount,
                warningCount,
                qualityScore,
                List.copyOf(issues),
                pathAnalysis.routes()
        );
    }

    private void appendDirectEndReportIssues(
            SimulationVersionEntity simulationVersionEntity,
            List<ValidationIssueResponse> issues
    ) {
        for (SimulationNodeEntity node : simulationVersionEntity.getNodes()) {
            if (node.isFinal()) {
                continue;
            }
            if (hasTimeLimit(node)
                    && node.getTimeoutNextNodeId() == null
                    && (node.getReportText() == null || node.getReportText().isBlank())) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "O tempo esgotado encerra a avaliação, mas está sem texto de relatório. "
                                + "No Editor de diálogo, selecione \"Vai para FIM\" em \"Quando o tempo acabar\" "
                                + "e preencha o relatório do encerramento."
                ));
            }
            for (SimulationOptionEntity option : node.getOptions()) {
                if (option.getNextNodeId() == null
                        && (option.getAuditNote() == null || option.getAuditNote().isBlank())) {
                    issues.add(new ValidationIssueResponse(
                            ValidationIssueSeverity.BLOCKER,
                            node.getNodeId(),
                            "Esta alternativa encerra a avaliação, mas está sem texto de relatório. "
                                    + "No Editor de diálogo, selecione \"Vai para FIM\" e preencha o relatório do encerramento."
                    ));
                }
            }
        }
    }

    private void appendMediaAccessibilityIssues(
            SimulationVersionEntity simulationVersionEntity,
            List<ValidationIssueResponse> issues
    ) {
        for (SimulationNodeEntity node : simulationVersionEntity.getNodes()) {
            validateMedia(node.getNodeId(), "etapa", node.getMediaType(), node.getMediaUrl(),
                    node.getPlainTextDescription(), node.getMediaTranscript(),
                    node.getMediaCaptionsUrl(), node.getMediaVersion(), issues);
            for (SimulationOptionEntity option : node.getOptions()) {
                validateMedia(node.getNodeId(), "alternativa " + option.getOptionId(), option.getMediaType(),
                        option.getMediaUrl(), option.getPlainTextDescription(), option.getMediaTranscript(),
                        option.getMediaCaptionsUrl(), option.getMediaVersion(), issues);
            }
        }
    }

    private void validateMedia(
            String nodeId, String label, br.com.iforce.praxis.shared.model.MediaType mediaType,
            String mediaUrl, String equivalentText, String transcript, String captionsUrl,
            String mediaVersion, List<ValidationIssueResponse> issues
    ) {
        if (mediaUrl == null || mediaUrl.isBlank()) return;
        if (mediaType == null) {
            addMediaBlocker(nodeId, "A " + label + " possui mídia sem tipo identificado.", issues);
            return;
        }
        if (!mediaUrl.startsWith("https://")) addMediaBlocker(nodeId, "A mídia da " + label + " deve usar URL HTTPS.", issues);
        if (mediaVersion == null || mediaVersion.isBlank()) addMediaBlocker(nodeId, "A mídia da " + label + " precisa de uma versão imutável.", issues);
        if (equivalentText == null || equivalentText.isBlank()) addMediaBlocker(nodeId, "A mídia da " + label + " precisa de texto equivalente acessível.", issues);
        if ((mediaType == br.com.iforce.praxis.shared.model.MediaType.AUDIO || mediaType == br.com.iforce.praxis.shared.model.MediaType.VIDEO)
                && (transcript == null || transcript.isBlank())) addMediaBlocker(nodeId, "Áudio e vídeo da " + label + " precisam de transcrição.", issues);
        if (mediaType == br.com.iforce.praxis.shared.model.MediaType.VIDEO && (captionsUrl == null || captionsUrl.isBlank()))
            addMediaBlocker(nodeId, "O vídeo da " + label + " precisa de legenda WebVTT.", issues);
    }

    private void addMediaBlocker(String nodeId, String message, List<ValidationIssueResponse> issues) {
        issues.add(new ValidationIssueResponse(ValidationIssueSeverity.BLOCKER, nodeId, message));
    }

    private boolean isRelevant(
            ValidationIssueResponse issue,
            Map<String, SimulationNodeEntity> nodesById
    ) {
        if (issue.message().startsWith(OPTION_WITHOUT_DESTINATION)
                || issue.message().startsWith(TIMEOUT_WITHOUT_DESTINATION)) {
            return false;
        }

        SimulationNodeEntity node = nodesById.get(issue.nodeId());
        if (node == null) {
            return true;
        }

        if (node.getTimeoutNextNodeId() == null && isTimeoutIssue(issue.message())) {
            return false;
        }

        if (hasTimeLimit(node)) {
            return true;
        }

        return !isTimeoutIssue(issue.message());
    }

    private boolean isTimeoutIssue(String message) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains("tempo esgotado")
                || normalizedMessage.contains("tempo acaba")
                || normalizedMessage.contains("timeout");
    }

    private boolean hasTimeLimit(SimulationNodeEntity node) {
        return node.getTimeLimitSeconds() != null && node.getTimeLimitSeconds() > 0;
    }
}
