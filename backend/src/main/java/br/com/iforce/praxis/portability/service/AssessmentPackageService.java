package br.com.iforce.praxis.portability.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.AssessmentContent;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.CompetencyContent;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.ImportPackageRequest;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.ImportPackageResponse;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.MediaAsset;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.NodeContent;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.OptionContent;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageEnvelope;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageManifest;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageOrigin;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageValidationResponse;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.VersionContent;
import br.com.iforce.praxis.shared.model.MediaType;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.FORMAT_VERSION;

@Service
public class AssessmentPackageService {

    private final SimulationVersionRepository simulationVersionRepository;
    private final SimulationRepository simulationRepository;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final AuditEventService auditEventService;
    private final AssessmentPackageValidator validator;
    private final ObjectMapper objectMapper;

    public AssessmentPackageService(
            SimulationVersionRepository simulationVersionRepository,
            SimulationRepository simulationRepository,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            AuditEventService auditEventService,
            AssessmentPackageValidator validator,
            ObjectMapper objectMapper
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.simulationRepository = simulationRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.auditEventService = auditEventService;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PackageEnvelope exportPackage(String simulationId, int versionNumber) {
        SimulationVersionEntity version = loadVersion(simulationId, versionNumber);
        Instant exportedAt = Instant.now();
        PackageManifest manifest = new PackageManifest(
                new PackageOrigin(
                        "Práxis",
                        simulationId,
                        versionNumber,
                        currentUserService.requiredUserId(),
                        exportedAt
                ),
                new AssessmentContent(
                        version.getSimulation().getName(),
                        version.getSimulation().getDescription(),
                        version.getSimulation().getCriticalSituation(),
                        version.getSimulation().getResultUse()
                ),
                mapVersion(version),
                collectMediaAssets(version)
        );
        return new PackageEnvelope(
                FORMAT_VERSION,
                exportedAt,
                validator.calculateHash(manifest),
                manifest
        );
    }

    @Transactional(readOnly = true)
    public PackageValidationResponse validate(PackageEnvelope envelope) {
        return validator.validate(envelope);
    }

    @Transactional
    public ImportPackageResponse importPackage(ImportPackageRequest request) {
        PackageValidationResponse validation = validator.validate(request.packageEnvelope());
        if (!validation.valid()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O pacote possui erros de integridade ou compatibilidade. Execute a validação prévia e corrija o manifesto."
            );
        }
        if (!request.confirmed()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Confirme explicitamente a importação após revisar o diagnóstico."
            );
        }
        if (!validation.competenciesRequiringConfirmation().isEmpty() && !request.confirmCompetencies()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Confirme a criação ou equivalência das competências declaradas no pacote."
            );
        }

        String empresaId = currentEmpresaService.requiredEmpresaId();
        String importer = currentUserService.requiredUserId();
        PackageManifest manifest = request.packageEnvelope().manifest();
        String simulationId = generateDeterministicId(request.newAssessmentName(), validation.calculatedHash());
        Instant createdAt = Instant.now();

        SimulationEntity simulation = new SimulationEntity();
        simulation.setId(simulationId);
        simulation.setEmpresaId(empresaId);
        simulation.setName(request.newAssessmentName().trim());
        simulation.setDescription(manifest.assessment().description().trim());
        simulation.setCriticalSituation(trimToNull(manifest.assessment().criticalSituation()));
        simulation.setResultUse(trimToNull(manifest.assessment().resultUse()));
        simulation.setCreatedAt(createdAt);

        SimulationVersionEntity version = importVersion(manifest.version(), simulation, createdAt);
        simulation.getVersions().add(version);
        simulationRepository.save(simulation);

        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put(manifest.origin().sourceAssessmentId(), simulationId);
        manifest.version().nodes().forEach(node -> mapping.put(
                manifest.origin().sourceAssessmentId() + ":node:" + node.id(),
                simulationId + ":node:" + node.id()
        ));
        auditEventService.appendSimulationVersionEvent(
                empresaId,
                simulationId,
                1,
                AuditEventType.SIMULATION_VERSION_DRAFT_CREATED,
                "Avaliação importada de pacote portátil validado.",
                toJson(Map.of(
                        "formatVersion", request.packageEnvelope().formatVersion(),
                        "packageHash", validation.calculatedHash(),
                        "sourceAssessmentId", manifest.origin().sourceAssessmentId(),
                        "sourceVersionNumber", manifest.origin().sourceVersionNumber(),
                        "sourceSystem", manifest.origin().sourceSystem(),
                        "sourceExportedBy", manifest.origin().exportedBy(),
                        "importedBy", importer,
                        "idMapping", mapping
                ))
        );

        return new ImportPackageResponse(
                simulationId,
                1,
                SimulationVersionStatus.DRAFT.name(),
                manifest.origin().sourceAssessmentId(),
                manifest.origin().sourceVersionNumber(),
                validation.calculatedHash(),
                Map.copyOf(mapping),
                validation.competenciesRequiringConfirmation()
        );
    }

    private SimulationVersionEntity loadVersion(String simulationId, int versionNumber) {
        return simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                        currentEmpresaService.requiredEmpresaId(),
                        simulationId,
                        versionNumber
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação ou versão não encontrada."));
    }

    private VersionContent mapVersion(SimulationVersionEntity version) {
        List<CompetencyContent> competencies = version.getCompetencies().stream()
                .sorted(Comparator.comparing(SimulationCompetencyEntity::getName))
                .map(competency -> new CompetencyContent(
                        competency.getName(),
                        competency.getWeight(),
                        competency.getTargetScore(),
                        competency.getTier() == null ? null : competency.getTier().getDescricao()
                ))
                .toList();
        List<NodeContent> nodes = version.getNodes().stream()
                .sorted(Comparator.comparingInt(SimulationNodeEntity::getTurnIndex)
                        .thenComparing(SimulationNodeEntity::getNodeId))
                .map(this::mapNode)
                .toList();
        return new VersionContent(version.getRootNodeId(), competencies, nodes);
    }

    private NodeContent mapNode(SimulationNodeEntity node) {
        List<OptionContent> options = node.getOptions().stream()
                .sorted(Comparator.comparing(SimulationOptionEntity::getOptionId))
                .map(this::mapOption)
                .toList();
        return new NodeContent(
                node.getNodeId(),
                node.getTurnIndex(),
                node.getSpeaker(),
                node.getMessage(),
                node.getTimeLimitSeconds(),
                node.getTimeoutNextNodeId(),
                node.isFinal(),
                node.getReportText(),
                node.getPositionX(),
                node.getPositionY(),
                node.getPlainTextDescription(),
                node.getAudioDescriptionUrl(),
                node.getMediaUrl(),
                node.getMediaType() == null ? null : node.getMediaType().name(),
                node.getMediaTranscript(),
                node.getMediaCaptionsUrl(),
                node.getMediaVersion(),
                options
        );
    }

    private OptionContent mapOption(SimulationOptionEntity option) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        option.getCompetencyScores().stream()
                .sorted(Comparator.comparing(OptionCompetencyScoreEntity::getCompetencyName))
                .forEach(score -> scores.put(score.getCompetencyName(), score.getScore()));
        return new OptionContent(
                option.getOptionId(),
                option.getText(),
                option.getNextNodeId(),
                option.isCritical(),
                option.getAuditNote(),
                option.getPlainTextDescription(),
                option.getAudioDescriptionUrl(),
                option.getMediaUrl(),
                option.getMediaType() == null ? null : option.getMediaType().name(),
                option.getMediaTranscript(),
                option.getMediaCaptionsUrl(),
                option.getMediaVersion(),
                Map.copyOf(scores)
        );
    }

    private List<MediaAsset> collectMediaAssets(SimulationVersionEntity version) {
        Map<String, String> media = new LinkedHashMap<>();
        version.getNodes().stream()
                .sorted(Comparator.comparing(SimulationNodeEntity::getNodeId))
                .forEach(node -> {
                    registerMedia(media, node.getMediaUrl(), node.getMediaType() == null ? "UNKNOWN" : node.getMediaType().name());
                    registerMedia(media, node.getAudioDescriptionUrl(), "AUDIO_DESCRIPTION");
                    registerMedia(media, node.getMediaCaptionsUrl(), "CAPTIONS");
                    node.getOptions().stream()
                            .sorted(Comparator.comparing(SimulationOptionEntity::getOptionId))
                            .forEach(option -> {
                                registerMedia(media, option.getMediaUrl(), option.getMediaType() == null ? "UNKNOWN" : option.getMediaType().name());
                                registerMedia(media, option.getAudioDescriptionUrl(), "AUDIO_DESCRIPTION");
                                registerMedia(media, option.getMediaCaptionsUrl(), "CAPTIONS");
                            });
                });
        List<MediaAsset> assets = new ArrayList<>();
        media.forEach((url, type) -> {
            String hash = validator.hashText(url);
            assets.add(new MediaAsset(
                    "asset-" + hash.substring(0, 12),
                    url,
                    type,
                    0,
                    hash,
                    "not-declared",
                    resolveOrigin(url),
                    false
            ));
        });
        return List.copyOf(assets);
    }

    private void registerMedia(Map<String, String> media, String url, String type) {
        if (url != null && !url.isBlank()) {
            media.putIfAbsent(url.trim(), type);
        }
    }

    private String resolveOrigin(String url) {
        if (url.startsWith("/")) {
            return "internal";
        }
        try {
            String host = new URI(url).getHost();
            return host == null ? "unknown" : host;
        } catch (URISyntaxException exception) {
            return "invalid";
        }
    }

    private SimulationVersionEntity importVersion(
            VersionContent content,
            SimulationEntity simulation,
            Instant createdAt
    ) {
        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setSimulation(simulation);
        version.setVersionNumber(1);
        version.setStatus(SimulationVersionStatus.DRAFT);
        version.setRootNodeId(content.rootNodeId());
        version.setCreatedAt(createdAt);
        version.setPublishedAt(null);

        for (CompetencyContent source : content.competencies()) {
            SimulationCompetencyEntity competency = new SimulationCompetencyEntity();
            competency.setSimulationVersion(version);
            competency.setName(source.name().trim());
            competency.setWeight(source.weight());
            competency.setTargetScore(source.targetScore());
            competency.setTier(source.tier() == null || source.tier().isBlank()
                    ? null
                    : ResultTier.fromString(source.tier()));
            version.getCompetencies().add(competency);
        }
        for (NodeContent source : content.nodes()) {
            version.getNodes().add(importNode(source, version));
        }
        return version;
    }

    private SimulationNodeEntity importNode(NodeContent source, SimulationVersionEntity version) {
        SimulationNodeEntity node = new SimulationNodeEntity();
        node.setSimulationVersion(version);
        node.setNodeId(source.id());
        node.setTurnIndex(source.turnIndex());
        node.setSpeaker(source.speaker());
        node.setMessage(source.message());
        node.setTimeLimitSeconds(source.timeLimitSeconds());
        node.setTimeoutNextNodeId(trimToNull(source.timeoutNextNodeId()));
        node.setFinal(source.terminal());
        node.setReportText(trimToNull(source.reportText()));
        node.setPositionX(source.positionX());
        node.setPositionY(source.positionY());
        node.setPlainTextDescription(trimToNull(source.plainTextDescription()));
        node.setAudioDescriptionUrl(trimToNull(source.audioDescriptionUrl()));
        node.setMediaUrl(trimToNull(source.mediaUrl()));
        node.setMediaType(parseMediaType(source.mediaType()));
        node.setMediaTranscript(trimToNull(source.mediaTranscript()));
        node.setMediaCaptionsUrl(trimToNull(source.mediaCaptionsUrl()));
        node.setMediaVersion(trimToNull(source.mediaVersion()));
        for (OptionContent option : source.options()) {
            node.getOptions().add(importOption(option, node));
        }
        return node;
    }

    private SimulationOptionEntity importOption(OptionContent source, SimulationNodeEntity node) {
        SimulationOptionEntity option = new SimulationOptionEntity();
        option.setSimulationNode(node);
        option.setOptionId(source.id());
        option.setText(source.text());
        option.setNextNodeId(trimToNull(source.nextNodeId()));
        option.setCritical(source.critical());
        option.setAuditNote(source.behavioralJustification() == null ? "" : source.behavioralJustification().trim());
        option.setPlainTextDescription(trimToNull(source.plainTextDescription()));
        option.setAudioDescriptionUrl(trimToNull(source.audioDescriptionUrl()));
        option.setMediaUrl(trimToNull(source.mediaUrl()));
        option.setMediaType(parseMediaType(source.mediaType()));
        option.setMediaTranscript(trimToNull(source.mediaTranscript()));
        option.setMediaCaptionsUrl(trimToNull(source.mediaCaptionsUrl()));
        option.setMediaVersion(trimToNull(source.mediaVersion()));
        source.competencyScores().forEach((name, value) -> {
            OptionCompetencyScoreEntity score = new OptionCompetencyScoreEntity();
            score.setSimulationOption(option);
            score.setCompetencyName(name);
            score.setScore(value);
            option.getCompetencyScores().add(score);
        });
        return option;
    }

    private MediaType parseMediaType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return MediaType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Tipo de mídia incompatível: " + value);
        }
    }

    private String generateDeterministicId(String name, String hash) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String base = normalized.isBlank() ? "avaliacao-importada" : normalized;
        if (base.length() > 80) {
            base = base.substring(0, 80).replaceAll("-$", "");
        }
        String candidate = base + "-" + hash.substring(0, 8);
        int suffix = 2;
        while (simulationRepository.existsById(candidate)) {
            candidate = base + "-" + hash.substring(0, 8) + "-" + suffix++;
        }
        return candidate;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível registrar a origem do pacote.", exception);
        }
    }
}
