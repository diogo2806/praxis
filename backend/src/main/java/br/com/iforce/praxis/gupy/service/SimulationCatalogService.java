package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;

import br.com.iforce.praxis.gupy.model.ScenarioNode;

import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;

import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;

import br.com.iforce.praxis.simulation.service.SimulationMapperService;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;


import java.util.LinkedHashMap;

import java.util.List;

import java.util.Locale;

import java.util.Map;

import java.util.Optional;


/**
 * Catálogo de consulta das provas (simulações) publicadas de cada empresa.
 *
 * <p>Na visão do processo, é a "vitrine" somente leitura das provas que estão
 * no ar: lista as provas publicadas, permite buscar por texto, contar e
 * localizar uma prova específica (pela sua versão publicada mais recente). É
 * usado tanto pelas telas internas quanto pela integração com a Gupy para
 * mostrar quais avaliações estão disponíveis.</p>
 */
@Service
public class SimulationCatalogService {

    private final SimulationVersionRepository simulationVersionRepository;
    private final SimulationMapperService simulationMapperService;

    public SimulationCatalogService(
            SimulationVersionRepository simulationVersionRepository,
            SimulationMapperService simulationMapperService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.simulationMapperService = simulationMapperService;
    }

    /**
     * Lista as simulações publicadas do empresa, com no máximo uma versão por simulação
     * (a publicada mais recente), evitando duplicidade quando há histórico de versões publicadas.
     */
    @Transactional(readOnly = true)
    public List<PublishedSimulation> findPublished(String empresaId) {
        Map<String, PublishedSimulation> latestBySimulationId = new LinkedHashMap<>();

        simulationVersionRepository
                .findBySimulationEmpresaIdAndStatusOrderByPublishedAtDesc(
                        empresaId,
                        SimulationVersionStatus.PUBLISHED
                )
                .stream()
                .map(simulationMapperService::toPublishedSimulation)
                .forEach(simulation -> latestBySimulationId.putIfAbsent(simulation.id(), simulation));

        return List.copyOf(latestBySimulationId.values());
    }

    /**
     * Lista as provas publicadas da empresa filtrando por texto e paginando.
     *
     * <p>A busca considera o identificador, o nome e a descrição da prova.</p>
     *
     * @param empresaId empresa dona das provas
     * @param searchString texto opcional de busca
     * @param offset a partir de qual posição começar (paginação)
     * @param limit quantas provas trazer
     * @return a página de provas publicadas que atendem ao filtro
     */
    @Transactional(readOnly = true)
    public List<PublishedSimulation> findPublished(String empresaId, String searchString, int offset, int limit) {
        int normalizedOffset = Math.max(offset, 0);
        int normalizedLimit = Math.max(limit, 1);

        return filteredPublished(empresaId, searchString).stream()
                .skip(normalizedOffset)
                .limit(normalizedLimit)
                .toList();
    }

    /**
     * Conta quantas provas publicadas da empresa atendem ao filtro de busca.
     *
     * <p>Usado para informar o total ao paginar a listagem.</p>
     *
     * @param empresaId empresa dona das provas
     * @param searchString texto opcional de busca
     * @return o total de provas publicadas que atendem ao filtro
     */
    @Transactional(readOnly = true)
    public int countPublished(String empresaId, String searchString) {
        return filteredPublished(empresaId, searchString).size();
    }

    /**
     * Localiza uma prova publicada pela sua versão publicada mais recente.
     *
     * @param empresaId empresa dona da prova
     * @param simulationId identificador da prova
     * @return a prova publicada, se existir
     */
    @Transactional(readOnly = true)
    public Optional<PublishedSimulation> findPublishedById(String empresaId, String simulationId) {
        return simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndStatusOrderByPublishedAtDesc(
                        empresaId,
                        simulationId,
                        SimulationVersionStatus.PUBLISHED
                )
                .stream()
                .findFirst()
                .map(simulationMapperService::toPublishedSimulation);
    }

    /**
     * Localiza uma prova por uma versão específica (exata) dela.
     *
     * <p>Importante para reconstruir exatamente a prova que um candidato
     * respondeu, mesmo que a prova já tenha sido republicada depois.</p>
     *
     * @param simulationVersionId identificador da versão da prova
     * @return a prova daquela versão, se existir
     */
    @Transactional(readOnly = true)
    public Optional<PublishedSimulation> findByVersionId(Long simulationVersionId) {
        return simulationVersionRepository.findById(simulationVersionId)
                .map(simulationMapperService::toPublishedSimulation);
    }

    /**
     * Encontra uma etapa (nó) específica dentro de uma prova.
     *
     * @param simulation a prova onde procurar
     * @param nodeId identificador da etapa
     * @return a etapa correspondente, se existir
     */
    public Optional<ScenarioNode> findNode(PublishedSimulation simulation, String nodeId) {
        return simulation.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .findFirst();
    }

    private List<PublishedSimulation> filteredPublished(String empresaId, String searchString) {
        String normalizedSearch = normalize(searchString);
        return findPublished(empresaId).stream()
                .filter(simulation -> normalizedSearch.isBlank()
                        || normalize(simulation.id()).contains(normalizedSearch)
                        || normalize(simulation.name()).contains(normalizedSearch)
                        || normalize(simulation.description()).contains(normalizedSearch))
                .toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }
}
