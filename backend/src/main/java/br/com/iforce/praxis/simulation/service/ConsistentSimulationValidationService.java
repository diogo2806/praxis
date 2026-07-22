package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.dto.ValidationIssueResponse;
import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aplica as convenções usadas pelos editores sobre o diagnóstico estrutural.
 *
 * <p>No contrato atual, uma alternativa sem {@code nextNodeId} encerra a
 * participação e o seletor a apresenta como "Vai para FIM". Da mesma forma,
 * uma etapa temporizada sem {@code timeoutNextNodeId} encerra a participação
 * quando o tempo acaba, enquanto uma etapa sem limite não precisa de transição
 * de timeout.</p>
 */
@Primary
@Service
public class ConsistentSimulationValidationService extends SimulationValidationService {

    private static final String OPTION_WITHOUT_DESTINATION = "Uma resposta está sem destino.";
    private static final String TIMEOUT_WITHOUT_DESTINATION =
            "Esta etapa continua o teste, mas não tem destino para tempo esgotado.";

    public ConsistentSimulationValidationService(PraxisProperties praxisProperties) {
        super(praxisProperties);
    }

    @Override
    public SimulationValidationResponse validate(SimulationVersionEntity simulationVersionEntity) {
        SimulationValidationResponse original = super.validate(simulationVersionEntity);
        Map<String, SimulationNodeEntity> nodesById = simulationVersionEntity.getNodes().stream()
                .collect(Collectors.toMap(SimulationNodeEntity::getNodeId, Function.identity(), (first, ignored) -> first));

        List<ValidationIssueResponse> issues = original.issues().stream()
                .filter(issue -> isRelevant(issue, nodesById))
                .toList();

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
                issues
        );
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
