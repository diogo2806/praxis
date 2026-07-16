package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.simulation.dto.QuickStartCreatedResponse;
import br.com.iforce.praxis.simulation.dto.QuickStartTemplateSummaryResponse;
import br.com.iforce.praxis.simulation.model.QuickStartCategory;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Atalho de criação "começar rápido": a partir de um modelo pronto (template
 * estático em JSON, por categoria), cria uma simulação completa em rascunho —
 * com competências, etapas, respostas e pesos já preenchidos — para a pessoa
 * seguir diretamente à revisão antes de publicar.
 *
 * <p>Os templates são carregados do classpath na inicialização e validados
 * estruturalmente, de modo que cada um gera uma versão que passa no validador.</p>
 */
@Service
public class SimulationQuickStartService {

    private static final String REDIRECT_TO = "/nova/validador";

    private final SimulationRepository simulationRepository;
    private final CurrentEmpresaService currentEmpresaService;
    private final AuditEventService auditEventService;
    private final Map<QuickStartCategory, QuickStartTemplate> templates;

    public SimulationQuickStartService(
            SimulationRepository simulationRepository,
            CurrentEmpresaService currentEmpresaService,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.simulationRepository = simulationRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.auditEventService = auditEventService;
        this.templates = loadTemplates(objectMapper);
    }

    private static Map<QuickStartCategory, QuickStartTemplate> loadTemplates(ObjectMapper objectMapper) {
        Map<QuickStartCategory, QuickStartTemplate> loaded = new LinkedHashMap<>();
        for (QuickStartCategory category : QuickStartCategory.values()) {
            String path = "quickstart-templates/" + category.templateFileName() + ".json";
            try (InputStream input = new ClassPathResource(path).getInputStream()) {
                QuickStartTemplate template = objectMapper.readValue(input, QuickStartTemplate.class);
                if (template.category() != category) {
                    throw new IllegalStateException(
                            "Template " + path + " declara categoria " + template.category() + " (esperado " + category + ")."
                    );
                }
                loaded.put(category, template);
            } catch (IOException exception) {
                throw new IllegalStateException("Não foi possível carregar o template " + path + ".", exception);
            }
        }
        return loaded;
    }

    /** Lista os modelos prontos disponíveis para a grade do "começar rápido". */
    @Transactional(readOnly = true)
    public List<QuickStartTemplateSummaryResponse> listTemplates() {
        return templates.values().stream()
                .map(template -> new QuickStartTemplateSummaryResponse(
                        template.category(),
                        template.title(),
                        template.description(),
                        (int) template.nodes().stream().filter(node -> !node.isFinal()).count()
                ))
                .toList();
    }

    /**
     * Cria uma simulação em rascunho a partir do modelo pronto da categoria.
     *
     * @param category categoria do modelo
     * @return o identificador, a versão criada e a rota para onde seguir
     */
    @Transactional
    public QuickStartCreatedResponse createFromTemplate(QuickStartCategory category) {
        QuickStartTemplate template = templates.get(category);
        if (template == null) {
            throw new IllegalArgumentException("Categoria de modelo não suportada: " + category);
        }

        Instant createdAt = Instant.now();
        String empresaId = currentEmpresaService.requiredEmpresaId();

        SimulationVersionEntity version = buildDraftVersion(template, createdAt);
        SimulationEntity simulation = version.getSimulation();
        simulation.setId(generateSimulationId(template.name()));
        simulation.setEmpresaId(empresaId);

        SimulationEntity saved = simulationRepository.save(simulation);

        auditEventService.appendSimulationVersionEvent(
                saved.getEmpresaId(),
                saved.getId(),
                version.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_DRAFT_CREATED,
                "Rascunho criado a partir do modelo pronto " + category + ".",
                "{\"status\":\"draft\",\"quickStartCategory\":\"" + category + "\"}"
        );

        return new QuickStartCreatedResponse(saved.getId(), version.getVersionNumber(), REDIRECT_TO);
    }

    /**
     * Constrói a entidade de versão (com a simulação associada) a partir do
     * template, sem id nem empresa nem persistência. Isolado para teste de que
     * cada modelo gera uma versão estruturalmente válida.
     */
    SimulationVersionEntity buildDraftVersion(QuickStartTemplate template, Instant createdAt) {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setName(template.name().trim());
        simulation.setDescription(buildDescription(template));
        simulation.setCriticalSituation(template.criticalSituation());
        simulation.setResultUse(template.resultUse());
        simulation.setCreatedAt(createdAt);

        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setSimulation(simulation);
        version.setVersionNumber(1);
        version.setStatus(SimulationVersionStatus.DRAFT);
        version.setRootNodeId(template.rootNodeId());
        version.setCreatedAt(createdAt);
        simulation.getVersions().add(version);

        for (TemplateCompetency competency : template.competencies()) {
            SimulationCompetencyEntity competencyEntity = new SimulationCompetencyEntity();
            competencyEntity.setSimulationVersion(version);
            competencyEntity.setName(competency.name().trim());
            competencyEntity.setWeight(competency.weight());
            competencyEntity.setTargetScore(competency.targetScore() == null ? 70 : competency.targetScore());
            competencyEntity.setTier(competency.tier() == null ? ResultTier.MAJOR : competency.tier());
            version.getCompetencies().add(competencyEntity);
        }

        for (TemplateNode node : template.nodes()) {
            SimulationNodeEntity nodeEntity = new SimulationNodeEntity();
            nodeEntity.setSimulationVersion(version);
            nodeEntity.setNodeId(node.nodeId());
            nodeEntity.setTurnIndex(node.turnIndex());
            nodeEntity.setSpeaker(node.speaker() == null ? "Cliente" : node.speaker());
            nodeEntity.setMessage(node.message() == null ? "" : node.message());
            nodeEntity.setTimeLimitSeconds(node.timeLimitSeconds());
            nodeEntity.setFinal(node.isFinal());
            nodeEntity.setTimeoutNextNodeId(node.isFinal() ? null : node.timeoutNextNodeId());
            nodeEntity.setReportText(node.reportText());

            if (node.options() != null) {
                for (TemplateOption option : node.options()) {
                    SimulationOptionEntity optionEntity = new SimulationOptionEntity();
                    optionEntity.setSimulationNode(nodeEntity);
                    optionEntity.setOptionId(option.optionId());
                    optionEntity.setText(option.text());
                    optionEntity.setNextNodeId(option.nextNodeId());
                    optionEntity.setCritical(option.critical());
                    optionEntity.setAuditNote(option.auditNote() == null ? "" : option.auditNote());
                    if (option.competencyScores() != null) {
                        option.competencyScores().forEach((name, score) -> {
                            OptionCompetencyScoreEntity scoreEntity = new OptionCompetencyScoreEntity();
                            scoreEntity.setSimulationOption(optionEntity);
                            scoreEntity.setCompetencyName(name.trim());
                            scoreEntity.setScore(score);
                            optionEntity.getCompetencyScores().add(scoreEntity);
                        });
                    }
                    nodeEntity.getOptions().add(optionEntity);
                }
            }
            version.getNodes().add(nodeEntity);
        }

        return version;
    }

    /** Acesso aos templates carregados, para validação/testes. */
    Map<QuickStartCategory, QuickStartTemplate> templates() {
        return templates;
    }

    private String buildDescription(QuickStartTemplate template) {
        String description = "Modelo pronto: " + template.title();
        if (template.criticalSituation() != null && !template.criticalSituation().isBlank()) {
            description += " — " + template.criticalSituation().trim();
        }
        return description.length() > 1000 ? description.substring(0, 1000) : description;
    }

    private String generateSimulationId(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String base = normalized.isBlank() ? "simulacao" : normalized;
        if (base.length() > 80) {
            base = base.substring(0, 80).replaceAll("-$", "");
        }
        String id;
        do {
            id = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        } while (simulationRepository.existsById(id));
        return id;
    }

    record QuickStartTemplate(
            QuickStartCategory category,
            String title,
            String description,
            String name,
            String criticalSituation,
            String resultUse,
            String rootNodeId,
            List<TemplateCompetency> competencies,
            List<TemplateNode> nodes
    ) {
    }

    record TemplateCompetency(String name, double weight, Integer targetScore, ResultTier tier) {
    }

    record TemplateNode(
            String nodeId,
            int turnIndex,
            String speaker,
            String message,
            Integer timeLimitSeconds,
            String timeoutNextNodeId,
            boolean isFinal,
            String reportText,
            List<TemplateOption> options
    ) {
    }

    record TemplateOption(
            String optionId,
            String text,
            String nextNodeId,
            boolean critical,
            String auditNote,
            Map<String, Integer> competencyScores
    ) {
    }
}
