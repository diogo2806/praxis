package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.simulation.dto.CompetencyWeightDto;
import br.com.iforce.praxis.simulation.dto.UpdateBlueprintRequest;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.dto.ValidationIssueResponse;
import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifica a qualidade e a integridade de uma prova antes da publicação.
 *
 * <p>Na visão do processo, é o "revisor automático" da prova. Ele percorre
 * todo o cenário e aponta problemas, separando-os em <b>impedimentos</b>
 * (que bloqueiam a publicação — por exemplo, uma etapa inicial inexistente,
 * caminhos sem saída, ciclos que prendem o candidato ou pesos de competência
 * inválidos) e <b>avisos</b> (que não bloqueiam, mas merecem atenção, como um
 * cenário muito grande). Ao final, calcula uma nota de qualidade e diz se a
 * prova está liberada para publicar. Assim, a equipe corrige os problemas
 * antes que a prova chegue a um candidato real.</p>
 */
@Service
public class SimulationValidationService {

    private static final int MAX_DEPTH_TURNS = 10;
    private static final int LARGE_GRAPH_NODE_THRESHOLD = 8;

    private final PraxisProperties praxisProperties;

    public SimulationValidationService(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

    /**
     * Revisa uma versão da prova e relata todos os problemas encontrados.
     *
     * <p>Fluxo do processo: confere a etapa inicial, valida cada etapa e suas
     * respostas, verifica os pesos das competências e analisa o cenário como
     * um todo (caminhos sem saída, ciclos, profundidade excessiva e equilíbrio
     * de pontuação). Devolve a lista de impedimentos e avisos, uma nota de
     * qualidade e a informação de se a prova pode ou não ser publicada.</p>
     *
     * @param simulationVersionEntity a versão da prova a revisar
     * @return o relatório de validação (impedimentos, avisos, nota e se é publicável)
     */
    public SimulationValidationResponse validate(SimulationVersionEntity simulationVersionEntity) {
        List<ValidationIssueResponse> issues = new ArrayList<>();
        Map<String, SimulationNodeEntity> nodesById = buildNodeMap(simulationVersionEntity, issues);

        if (!nodesById.containsKey(simulationVersionEntity.getRootNodeId())) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    simulationVersionEntity.getRootNodeId(),
                    "A primeira etapa do teste não foi encontrada. Escolha uma etapa inicial válida."
            ));
        }

        for (SimulationNodeEntity node : simulationVersionEntity.getNodes()) {
            validateNode(node, nodesById, issues);
        }

        validateCompetencyCoverage(simulationVersionEntity, issues);
        validateCompetencyWeights(simulationVersionEntity, issues);
        warnLargeGraph(simulationVersionEntity, issues);

        if (nodesById.containsKey(simulationVersionEntity.getRootNodeId())) {
            detectCycles(simulationVersionEntity.getRootNodeId(), nodesById, issues);
            validateReachability(simulationVersionEntity.getRootNodeId(), nodesById, issues);
            validateDepth(simulationVersionEntity.getRootNodeId(), nodesById, issues);
            validatePathCompetencyCoverage(simulationVersionEntity.getRootNodeId(), nodesById, simulationVersionEntity, issues);
        }

        long blockerCount = issues.stream()
                .filter(issue -> issue.severity() == ValidationIssueSeverity.BLOCKER)
                .count();
        long warningCount = issues.stream()
                .filter(issue -> issue.severity() == ValidationIssueSeverity.WARNING)
                .count();
        boolean publishable = blockerCount == 0;
        int qualityScore = Math.max(0, 100 - (int) blockerCount * 30 - (int) warningCount * 10);

        return new SimulationValidationResponse(
                simulationVersionEntity.getSimulation().getId(),
                simulationVersionEntity.getVersionNumber(),
                publishable,
                blockerCount,
                warningCount,
                qualityScore,
                issues
        );
    }

    private Map<String, SimulationNodeEntity> buildNodeMap(
            SimulationVersionEntity simulationVersionEntity,
            List<ValidationIssueResponse> issues
    ) {
        Map<String, SimulationNodeEntity> nodesById = new HashMap<>();
        for (SimulationNodeEntity node : simulationVersionEntity.getNodes()) {
            SimulationNodeEntity previousNode = nodesById.put(node.getNodeId(), node);
            if (previousNode != null) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "Há etapas duplicadas com o mesmo identificador. Revise as etapas repetidas."
                ));
            }
        }
        return nodesById;
    }

    private void validateNode(
            SimulationNodeEntity node,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        if (node.isFinal()) {
            validateFinalNode(node, issues);
            return;
        }

        if (node.getMessage() == null || node.getMessage().isBlank()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Esta etapa está sem fala para o candidato. Abra a etapa e escreva a mensagem que será exibida antes das respostas."
            ));
        }

        if (node.getOptions().size() < 2 || node.getOptions().size() > 4) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Esta etapa tem "
                            + node.getOptions().size()
                            + " resposta(s). Adicione ou remova respostas para ficar entre 2 e 4 alternativas."
            ));
        }

        for (SimulationOptionEntity option : node.getOptions()) {
            validateOption(node, option, nodesById, issues);
        }

        validateTimeoutTransition(node, nodesById, issues);
    }

    private void validateFinalNode(
            SimulationNodeEntity node,
            List<ValidationIssueResponse> issues
    ) {
        if (!node.getOptions().isEmpty()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Esta etapa está marcada como encerramento, então não pode ter respostas. Remova as respostas ou transforme a etapa em uma etapa normal."
            ));
        }
        if (node.getTimeoutNextNodeId() != null) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Esta etapa está marcada como encerramento, então não deve ter destino de tempo esgotado. Remova a ligação de timeout."
            ));
        }
        if (node.getReportText() == null || node.getReportText().isBlank()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Esta etapa de encerramento está sem texto de relatório. Preencha o resumo que a equipe responsável verá ao fim desse caminho."
            ));
        }
    }

    private void validateTimeoutTransition(
            SimulationNodeEntity node,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        String timeoutNextNodeId = node.getTimeoutNextNodeId();
        if (timeoutNextNodeId == null) {
            boolean hasNonTerminalOption = node.getOptions().stream()
                    .anyMatch(option -> option.getNextNodeId() != null);
            if (hasNonTerminalOption) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "Esta etapa continua o teste, mas não tem destino para tempo esgotado. No campo \"Tempo acaba\", escolha uma próxima etapa ou um encerramento."
                ));
            }
            return;
        }

        SimulationNodeEntity nextNode = nodesById.get(timeoutNextNodeId);
        if (nextNode == null) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "O destino de tempo esgotado aponta para uma etapa que não existe. Escolha uma etapa existente no campo \"Tempo acaba\"."
            ));
            return;
        }

        if (nextNode.getTurnIndex() <= node.getTurnIndex()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "O destino de tempo esgotado volta para esta etapa ou para uma etapa anterior. Altere para uma etapa posterior ou para um encerramento."
            ));
        }
    }

    private void validateOption(
            SimulationNodeEntity node,
            SimulationOptionEntity option,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        if (option.getCompetencyScores().isEmpty()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Uma resposta está sem pontuação de competência. Abra a resposta e atribua notas de 0 a 100 para as competências configuradas."
            ));
        }

        for (OptionCompetencyScoreEntity score : option.getCompetencyScores()) {
            if (score.getScore() < 0 || score.getScore() > 100) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "Uma pontuação de competência está fora do intervalo permitido. Ajuste a nota da resposta para um valor entre 0 e 100."
                ));
            }
        }

        if (option.getNextNodeId() == null) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Uma resposta está sem destino. Escolha para onde ela leva: outra etapa posterior ou uma etapa de encerramento."
            ));
        }

        if (option.getNextNodeId() != null && !nodesById.containsKey(option.getNextNodeId())) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    node.getNodeId(),
                    "Uma resposta aponta para uma etapa que não existe. Edite o destino da resposta e selecione uma etapa válida."
            ));
        }

        if (option.getNextNodeId() != null) {
            SimulationNodeEntity nextNode = nodesById.get(option.getNextNodeId());
            if (nextNode != null && nextNode.getTurnIndex() <= node.getTurnIndex()) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        node.getNodeId(),
                        "Uma resposta leva para esta etapa ou para uma etapa anterior. Altere o destino para uma etapa posterior ou para um encerramento."
                ));
            }
        }
    }

    private void validateCompetencyWeights(
            SimulationVersionEntity simulationVersionEntity,
            List<ValidationIssueResponse> issues
    ) {
        Set<SimulationCompetencyEntity> competencies = simulationVersionEntity.getCompetencies();
        if (competencies.isEmpty()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    simulationVersionEntity.getRootNodeId(),
                    "O teste não tem competências configuradas. Volte ao passo Avaliação e adicione pelo menos uma competência com peso."
            ));
            return;
        }

        double weightSum = 0.0;
        for (SimulationCompetencyEntity competency : competencies) {
            if (competency.getWeight() < 0) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        competency.getName(),
                        "Uma competência está com peso negativo. Volte ao passo Avaliação e ajuste o peso para zero ou mais."
                ));
            }
            weightSum += competency.getWeight();
        }

        if (Math.abs(weightSum - 1.0) > praxisProperties.competencyWeightTolerance()) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    simulationVersionEntity.getRootNodeId(),
                    "Os pesos das competências precisam somar 100%. Soma atual: "
                            + (weightSum * 100)
                            + "%. Volte ao passo Avaliação e ajuste os pesos."
            ));
        }
    }

    private void validateCompetencyCoverage(
            SimulationVersionEntity version,
            List<ValidationIssueResponse> issues
    ) {
        Set<String> configured = new HashSet<>();
        for (SimulationCompetencyEntity competency : version.getCompetencies()) {
            configured.add(competency.getName());
        }

        Set<String> scored = new HashSet<>();
        for (SimulationNodeEntity node : version.getNodes()) {
            for (SimulationOptionEntity option : node.getOptions()) {
                for (OptionCompetencyScoreEntity score : option.getCompetencyScores()) {
                    String competencyName = score.getCompetencyName();
                    if (!configured.contains(competencyName)) {
                        issues.add(new ValidationIssueResponse(
                                ValidationIssueSeverity.BLOCKER,
                                node.getNodeId(),
                                "Uma resposta pontua a competência \""
                                        + competencyName
                                        + "\", mas ela não está configurada no teste. Adicione essa competência no passo Avaliação ou remova a pontuação dessa resposta."
                        ));
                    }
                    scored.add(competencyName);
                }
            }
        }

        for (String competencyName : configured) {
            if (!scored.contains(competencyName)) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.BLOCKER,
                        competencyName,
                        "A competência \""
                                + competencyName
                                + "\" tem peso, mas nenhuma resposta pontua essa competência. Inclua uma nota positiva para ela em pelo menos uma resposta."
                ));
            }
        }
    }

    /**
     * Confere se os pesos das competências de uma prova são válidos.
     *
     * <p>Usado ao criar/editar a prova para garantir que os pesos fazem
     * sentido (por exemplo, somam o esperado e não têm valores inválidos),
     * recusando o cadastro caso contrário.</p>
     *
     * @param competencies as competências com seus pesos
     */
    public void validateWeights(List<CompetencyWeightDto> competencies) {
        validateWeightValues(
                competencies.stream()
                        .map(CompetencyWeightDto::weight)
                        .toList()
        );
    }

    /**
     * Confere se os pesos das competências de um plano de avaliação são válidos.
     *
     * <p>Mesma verificação de {@link #validateWeights}, aplicada aos dados que
     * chegam na atualização do plano da avaliação.</p>
     *
     * @param competencies as competências do plano com seus pesos
     */
    public void validateBlueprintWeights(List<UpdateBlueprintRequest.CompetencyRequest> competencies) {
        validateWeightValues(
                competencies.stream()
                        .map(UpdateBlueprintRequest.CompetencyRequest::weight)
                        .toList()
        );
    }

    private void validateWeightValues(List<Double> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ao menos uma competência é obrigatória.");
        }

        double sum = 0.0;
        for (Double weight : weights) {
            if (weight == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Peso de competência não pode ser nulo.");
            }
            if (weight < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Peso de competência não pode ser negativo.");
            }
            sum += weight;
        }
        if (Math.abs(sum - 1.0) > praxisProperties.competencyWeightTolerance()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os pesos precisam somar 100%."
            );
        }
    }

    private void warnLargeGraph(
            SimulationVersionEntity simulationVersionEntity,
            List<ValidationIssueResponse> issues
    ) {
        if (simulationVersionEntity.getNodes().size() > LARGE_GRAPH_NODE_THRESHOLD) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.WARNING,
                    simulationVersionEntity.getRootNodeId(),
                    "Este teste tem muitas etapas. A validação pode demorar um pouco mais; se estiver difícil revisar, divida o fluxo ou remova etapas redundantes."
            ));
        }
    }

    private void validatePathCompetencyCoverage(
            String rootNodeId,
            Map<String, SimulationNodeEntity> nodesById,
            SimulationVersionEntity simulationVersionEntity,
            List<ValidationIssueResponse> issues
    ) {
        List<List<SimulationNodeEntity>> paths = new ArrayList<>();
        collectTerminalNodePaths(rootNodeId, nodesById, new HashSet<>(), new ArrayList<>(), paths);

        for (List<SimulationNodeEntity> path : paths) {
            SimulationNodeEntity terminalNode = path.getLast();
            for (SimulationCompetencyEntity competency : simulationVersionEntity.getCompetencies()) {
                int maxForCompetency = calculateMaxPathScoreForCompetency(path, competency.getName());
                if (maxForCompetency <= 0) {
                    issues.add(new ValidationIssueResponse(
                            ValidationIssueSeverity.BLOCKER,
                            terminalNode.getNodeId(),
                            "O caminho que termina em "
                                    + terminalNode.getNodeId()
                                    + " não pontua a competência \""
                                    + competency.getName()
                                    + "\". Em alguma resposta desse caminho, atribua uma nota maior que zero para essa competência."
                    ));
                }
            }
        }
    }

    private int calculateMaxPathScoreForCompetency(List<SimulationNodeEntity> path, String competencyName) {
        return path.stream()
                .mapToInt(node -> node.getOptions().stream()
                        .mapToInt(option -> option.getCompetencyScores().stream()
                                .filter(score -> competencyName.equals(score.getCompetencyName()))
                                .mapToInt(OptionCompetencyScoreEntity::getScore)
                                .max()
                                .orElse(0))
                        .max()
                        .orElse(0))
                .sum();
    }

    private void collectTerminalNodePaths(
            String nodeId,
            Map<String, SimulationNodeEntity> nodesById,
            Set<String> visiting,
            List<SimulationNodeEntity> currentPath,
            List<List<SimulationNodeEntity>> paths
    ) {
        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null || !visiting.add(nodeId)) {
            return;
        }

        currentPath.add(node);
        List<String> nextNodeIds = nextNodeIds(node, nodesById);
        if (nextNodeIds.isEmpty()) {
            paths.add(new ArrayList<>(currentPath));
        } else {
            for (String nextNodeId : nextNodeIds) {
                collectTerminalNodePaths(nextNodeId, nodesById, visiting, currentPath, paths);
            }
        }

        currentPath.removeLast();
        visiting.remove(nodeId);
    }

    private void validateDepth(
            String rootNodeId,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        int longestPath = longestPathLength(rootNodeId, nodesById, new HashMap<>(), new HashSet<>());
        if (longestPath > MAX_DEPTH_TURNS) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    rootNodeId,
                    "O teste passa do limite de " + MAX_DEPTH_TURNS + " etapas em um único caminho. Total atual: " + longestPath + "."
            ));
        }
    }

    private int longestPathLength(
            String nodeId,
            Map<String, SimulationNodeEntity> nodesById,
            Map<String, Integer> memo,
            Set<String> visiting
    ) {
        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null || !visiting.add(nodeId)) {
            // Nó inexistente ou ciclo (já sinalizado por detectCycles): interrompe a recursão.
            return 0;
        }

        Integer cached = memo.get(nodeId);
        if (cached != null) {
            visiting.remove(nodeId);
            return cached;
        }

        int longestChild = 0;
        for (String nextNodeId : nextNodeIds(node, nodesById)) {
            longestChild = Math.max(longestChild, longestPathLength(nextNodeId, nodesById, memo, visiting));
        }

        int longest = 1 + longestChild;
        memo.put(nodeId, longest);
        visiting.remove(nodeId);
        return longest;
    }

    private void detectCycles(
            String rootNodeId,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        visitForCycles(rootNodeId, nodesById, visiting, visited, issues);
    }

    private void visitForCycles(
            String nodeId,
            Map<String, SimulationNodeEntity> nodesById,
            Set<String> visiting,
            Set<String> visited,
            List<ValidationIssueResponse> issues
    ) {
        if (visited.contains(nodeId)) {
            return;
        }
        if (visiting.contains(nodeId)) {
            issues.add(new ValidationIssueResponse(
                    ValidationIssueSeverity.BLOCKER,
                    nodeId,
                    "Uma resposta leva de volta a uma etapa anterior e cria um caminho sem fim. Confira para onde cada resposta aponta."
            ));
            return;
        }

        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null) {
            return;
        }

        visiting.add(nodeId);
        for (String nextNodeId : nextNodeIds(node, nodesById)) {
            visitForCycles(nextNodeId, nodesById, visiting, visited, issues);
        }
        visiting.remove(nodeId);
        visited.add(nodeId);
    }

    private void validateReachability(
            String rootNodeId,
            Map<String, SimulationNodeEntity> nodesById,
            List<ValidationIssueResponse> issues
    ) {
        Set<String> reachable = new HashSet<>();
        collectReachable(rootNodeId, nodesById, reachable);

        for (String nodeId : nodesById.keySet()) {
            if (!reachable.contains(nodeId)) {
                issues.add(new ValidationIssueResponse(
                        ValidationIssueSeverity.WARNING,
                        nodeId,
                        "Existe uma etapa que nenhum candidato chega a ver, porque nenhuma resposta leva até ela."
                ));
            }
        }
    }

    private void collectReachable(String nodeId, Map<String, SimulationNodeEntity> nodesById, Set<String> reachable) {
        if (!reachable.add(nodeId)) {
            return;
        }

        SimulationNodeEntity node = nodesById.get(nodeId);
        if (node == null) {
            return;
        }

        for (String nextNodeId : nextNodeIds(node, nodesById)) {
            collectReachable(nextNodeId, nodesById, reachable);
        }
    }

    private List<String> nextNodeIds(SimulationNodeEntity node, Map<String, SimulationNodeEntity> nodesById) {
        if (node.isFinal()) {
            return List.of();
        }
        List<String> nextNodeIds = new ArrayList<>(node.getOptions().stream()
                .map(SimulationOptionEntity::getNextNodeId)
                .filter(nextNodeId -> nextNodeId != null && nodesById.containsKey(nextNodeId))
                .distinct()
                .toList());
        if (node.getTimeoutNextNodeId() != null && nodesById.containsKey(node.getTimeoutNextNodeId())
                && !nextNodeIds.contains(node.getTimeoutNextNodeId())) {
            nextNodeIds.add(node.getTimeoutNextNodeId());
        }
        return nextNodeIds;
    }
}
