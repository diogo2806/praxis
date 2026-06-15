package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SimulationCatalogService {

    private static final String SIMULATION_ID = "sim-atendimento-caos";
    private static final String ROOT_NODE_ID = "turno-1";

    private static final List<PublishedSimulation> PUBLISHED_SIMULATIONS = List.of(
            new PublishedSimulation(
                    SIMULATION_ID,
                    "O Dia do Caos",
                    "Avaliação situacional determinística para priorização, comunicação e decisão em contexto.",
                    List.of("Empatia", "Resolução de conflito", "Aderência à política"),
                    ROOT_NODE_ID,
                    List.of(
                            new ScenarioNode(
                                    ROOT_NODE_ID,
                                    1,
                                    "Cliente fictício",
                                    "Chegou quebrado. Quero meu dinheiro de volta agora.",
                                    45,
                                    List.of(
                                            new ScenarioOption(
                                                    "opcao-promete-estorno",
                                                    "Peço desculpas e já prometo o estorno para acalmar a situação.",
                                                    null,
                                                    Map.of(
                                                            "Empatia", 82,
                                                            "Resolução de conflito", 42,
                                                            "Aderência à política", 15
                                                    ),
                                                    true,
                                                    "Promete estorno sem validar a política. Exige revisão humana."
                                            ),
                                            new ScenarioOption(
                                                    "opcao-processo-frio",
                                                    "Informo que existe uma política e peço o número do pedido antes de qualquer encaminhamento.",
                                                    null,
                                                    Map.of(
                                                            "Empatia", 48,
                                                            "Resolução de conflito", 70,
                                                            "Aderência à política", 88
                                                    ),
                                                    false,
                                                    "Segue processo, mas reduz acolhimento em situação de tensão."
                                            ),
                                            new ScenarioOption(
                                                    "opcao-equilibrada",
                                                    "Acolho a frustração, peço os dados mínimos e explico que vou validar a política antes do próximo passo.",
                                                    null,
                                                    Map.of(
                                                            "Empatia", 86,
                                                            "Resolução de conflito", 78,
                                                            "Aderência à política", 92
                                                    ),
                                                    false,
                                                    "Equilibra acolhimento, coleta mínima e limite de alçada."
                                            )
                                    )
                            )
                    )
            )
    );

    public List<PublishedSimulation> findPublished() {
        return PUBLISHED_SIMULATIONS;
    }

    public Optional<PublishedSimulation> findPublishedById(String simulationId) {
        return PUBLISHED_SIMULATIONS.stream()
                .filter(simulation -> simulation.id().equals(simulationId))
                .findFirst();
    }

    public Optional<ScenarioNode> findNode(PublishedSimulation simulation, String nodeId) {
        return simulation.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .findFirst();
    }
}
