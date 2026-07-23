from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    if old not in text:
        raise RuntimeError(f"Padrão não encontrado em {path}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))


def replace_all(path: str, old: str, new: str) -> None:
    text = read(path)
    if old not in text:
        raise RuntimeError(f"Padrão não encontrado em {path}: {old[:120]!r}")
    write(path, text.replace(old, new))


def regex_once(path: str, pattern: str, replacement: str, flags: int = 0) -> None:
    text = read(path)
    updated, count = re.subn(pattern, replacement, text, count=1, flags=flags)
    if count != 1:
        raise RuntimeError(f"Regex esperava 1 ocorrência em {path}, encontrou {count}: {pattern}")
    write(path, updated)


# 1. Contrato de mídia e upload seguro.
replace_once(
    "backend/src/main/java/br/com/iforce/praxis/shared/model/MediaType.java",
    "    IMAGE,\n    AUDIO\n",
    "    IMAGE,\n    AUDIO,\n    VIDEO\n",
)

media_storage = "backend/src/main/java/br/com/iforce/praxis/media/service/MediaStorageService.java"
replace_once(
    media_storage,
    "    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;",
    "    private static final long MAX_STANDARD_FILE_SIZE_BYTES = 10L * 1024 * 1024;\n"
    "    private static final long MAX_VIDEO_FILE_SIZE_BYTES = 100L * 1024 * 1024;",
)
replace_once(
    media_storage,
    "    private static final Map<String, String> AUDIO_EXTENSIONS = Map.ofEntries(\n"
    "            Map.entry(\"audio/mpeg\", \".mp3\"),\n"
    "            Map.entry(\"audio/mp3\", \".mp3\"),\n"
    "            Map.entry(\"audio/wav\", \".wav\"),\n"
    "            Map.entry(\"audio/x-wav\", \".wav\"),\n"
    "            Map.entry(\"audio/vnd.wave\", \".wav\"),\n"
    "            Map.entry(\"audio/ogg\", \".ogg\"),\n"
    "            Map.entry(\"audio/webm\", \".webm\"),\n"
    "            Map.entry(\"audio/mp4\", \".m4a\"),\n"
    "            Map.entry(\"audio/x-m4a\", \".m4a\"),\n"
    "            Map.entry(\"audio/aac\", \".aac\")\n"
    "    );",
    "    private static final Map<String, String> AUDIO_EXTENSIONS = Map.ofEntries(\n"
    "            Map.entry(\"audio/mpeg\", \".mp3\"),\n"
    "            Map.entry(\"audio/mp3\", \".mp3\"),\n"
    "            Map.entry(\"audio/wav\", \".wav\"),\n"
    "            Map.entry(\"audio/x-wav\", \".wav\"),\n"
    "            Map.entry(\"audio/vnd.wave\", \".wav\"),\n"
    "            Map.entry(\"audio/ogg\", \".ogg\"),\n"
    "            Map.entry(\"audio/webm\", \".webm\"),\n"
    "            Map.entry(\"audio/mp4\", \".m4a\"),\n"
    "            Map.entry(\"audio/x-m4a\", \".m4a\"),\n"
    "            Map.entry(\"audio/aac\", \".aac\")\n"
    "    );\n\n"
    "    private static final Map<String, String> VIDEO_EXTENSIONS = Map.ofEntries(\n"
    "            Map.entry(\"video/mp4\", \".mp4\"),\n"
    "            Map.entry(\"video/webm\", \".webm\"),\n"
    "            Map.entry(\"video/ogg\", \".ogv\"),\n"
    "            Map.entry(\"video/quicktime\", \".mov\")\n"
    "    );",
)
replace_once(
    media_storage,
    "        if (file.getSize() > MAX_FILE_SIZE_BYTES) {\n"
    "            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, \"Mídia excede o tamanho máximo de 10MB.\");\n"
    "        }\n\n"
    "        byte[] bytes = readBytes(file);\n"
    "        String detectedContentType = normalizeContentType(TIKA.detect(bytes));\n"
    "        MediaType mediaType = resolveMediaType(detectedContentType);",
    "        if (file.getSize() > MAX_VIDEO_FILE_SIZE_BYTES) {\n"
    "            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, \"Mídia excede o tamanho máximo de 100MB.\");\n"
    "        }\n\n"
    "        byte[] bytes = readBytes(file);\n"
    "        String detectedContentType = normalizeContentType(TIKA.detect(bytes));\n"
    "        MediaType mediaType = resolveMediaType(detectedContentType);\n"
    "        if (mediaType != MediaType.VIDEO && bytes.length > MAX_STANDARD_FILE_SIZE_BYTES) {\n"
    "            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, \"Imagem ou áudio excede o tamanho máximo de 10MB.\");\n"
    "        }",
)
replace_once(
    media_storage,
    "        if (AUDIO_EXTENSIONS.containsKey(contentType)) {\n"
    "            return MediaType.AUDIO;\n"
    "        }\n"
    "        throw new ResponseStatusException(\n"
    "                HttpStatus.UNSUPPORTED_MEDIA_TYPE,\n"
    "                \"Apenas imagens ou áudios são suportados.\"\n"
    "        );",
    "        if (AUDIO_EXTENSIONS.containsKey(contentType)) {\n"
    "            return MediaType.AUDIO;\n"
    "        }\n"
    "        if (VIDEO_EXTENSIONS.containsKey(contentType)) {\n"
    "            return MediaType.VIDEO;\n"
    "        }\n"
    "        throw new ResponseStatusException(\n"
    "                HttpStatus.UNSUPPORTED_MEDIA_TYPE,\n"
    "                \"Apenas imagens, áudios ou vídeos nos formatos permitidos são suportados.\"\n"
    "        );",
)
replace_once(
    media_storage,
    "        Map<String, String> extensions = mediaType == MediaType.IMAGE ? IMAGE_EXTENSIONS : AUDIO_EXTENSIONS;\n"
    "        return extensions.get(contentType);",
    "        return switch (mediaType) {\n"
    "            case IMAGE -> IMAGE_EXTENSIONS.get(contentType);\n"
    "            case AUDIO -> AUDIO_EXTENSIONS.get(contentType);\n"
    "            case VIDEO -> VIDEO_EXTENSIONS.get(contentType);\n"
    "        };",
)
replace_all(media_storage, "imagens e áudios", "imagens, áudios e vídeos")
replace_all(media_storage, "imagem ou áudio", "imagem, áudio ou vídeo")

# 2. Persistência e contratos de autoria/publicação.
entity_fields = (
    "\n\n    @Column(name = \"media_transcript\", length = 8000)\n"
    "    private String mediaTranscript;\n\n"
    "    @Column(name = \"media_captions_url\", length = 1000)\n"
    "    private String mediaCaptionsUrl;\n\n"
    "    @Column(name = \"media_version\", length = 120)\n"
    "    private String mediaVersion;"
)
for path in [
    "backend/src/main/java/br/com/iforce/praxis/simulation/persistence/entity/SimulationNodeEntity.java",
    "backend/src/main/java/br/com/iforce/praxis/simulation/persistence/entity/SimulationOptionEntity.java",
]:
    replace_once(path, "    private MediaType mediaType;", "    private MediaType mediaType;" + entity_fields)

request_fields = (
    "        @Schema(description = \"Tipo da mídia anexada (IMAGE, AUDIO ou VIDEO).\", nullable = true)\n"
    "        MediaType mediaType,\n\n"
    "        @Size(max = 8000)\n"
    "        @Schema(description = \"Transcrição textual acessível do áudio ou vídeo.\", nullable = true)\n"
    "        String mediaTranscript,\n\n"
    "        @Size(max = 1000)\n"
    "        @Schema(description = \"URL pública da legenda WebVTT do vídeo.\", nullable = true)\n"
    "        String mediaCaptionsUrl,\n\n"
    "        @Size(max = 120)\n"
    "        @Schema(description = \"Versão imutável do conteúdo multimídia apresentada ao candidato.\", nullable = true)\n"
    "        String mediaVersion\n"
)
for path in [
    "backend/src/main/java/br/com/iforce/praxis/simulation/dto/CreateNodeRequest.java",
    "backend/src/main/java/br/com/iforce/praxis/simulation/dto/UpdateNodeRequest.java",
    "backend/src/main/java/br/com/iforce/praxis/simulation/dto/CreateOptionRequest.java",
    "backend/src/main/java/br/com/iforce/praxis/simulation/dto/UpdateOptionRequest.java",
]:
    text = read(path)
    pattern = r"        @Schema\(description = \"Tipo da mídia anexada \(IMAGE ou AUDIO\)\.\", nullable = true\)\n        MediaType mediaType\n"
    updated, count = re.subn(pattern, request_fields, text, count=1)
    if count != 1:
        raise RuntimeError(f"Contrato de mídia não encontrado em {path}")
    write(path, updated)

response_path = "backend/src/main/java/br/com/iforce/praxis/simulation/dto/SimulationVersionDetailResponse.java"
replace_all(
    response_path,
    "            String mediaUrl,\n            MediaType mediaType",
    "            String mediaUrl,\n            MediaType mediaType,\n            String mediaTranscript,\n            String mediaCaptionsUrl,\n            String mediaVersion",
)

scenario_node = "backend/src/main/java/br/com/iforce/praxis/gupy/model/ScenarioNode.java"
replace_once(
    scenario_node,
    "        String mediaUrl,\n        MediaType mediaType,\n        List<ScenarioOption> options",
    "        String mediaUrl,\n        MediaType mediaType,\n        String mediaTranscript,\n        String mediaCaptionsUrl,\n        String mediaVersion,\n        List<ScenarioOption> options",
)
replace_once(
    scenario_node,
    "        this(id, turnIndex, speaker, message, timeLimitSeconds, null, false, null, null, null, null, null, options);",
    "        this(id, turnIndex, speaker, message, timeLimitSeconds, null, false, null, null, null, null, null, null, null, null, options);",
)

scenario_option = "backend/src/main/java/br/com/iforce/praxis/gupy/model/ScenarioOption.java"
replace_once(
    scenario_option,
    "        String mediaUrl,\n        MediaType mediaType\n",
    "        String mediaUrl,\n        MediaType mediaType,\n        String mediaTranscript,\n        String mediaCaptionsUrl,\n        String mediaVersion\n",
)
replace_once(
    scenario_option,
    "        this(id, text, nextNodeId, competencyScores, critical, auditNote, null, null, null, null);",
    "        this(id, text, nextNodeId, competencyScores, critical, auditNote, null, null, null, null, null, null, null);",
)

mapper = "backend/src/main/java/br/com/iforce/praxis/simulation/service/SimulationMapperService.java"
replace_all(
    mapper,
    "                simulationNodeEntity.getMediaType(),\n                options",
    "                simulationNodeEntity.getMediaType(),\n                simulationNodeEntity.getMediaTranscript(),\n                simulationNodeEntity.getMediaCaptionsUrl(),\n                simulationNodeEntity.getMediaVersion(),\n                options",
)
replace_all(
    mapper,
    "                simulationOptionEntity.getMediaUrl(),\n                simulationOptionEntity.getMediaType()\n",
    "                simulationOptionEntity.getMediaUrl(),\n                simulationOptionEntity.getMediaType(),\n                simulationOptionEntity.getMediaTranscript(),\n                simulationOptionEntity.getMediaCaptionsUrl(),\n                simulationOptionEntity.getMediaVersion()\n",
)

admin = "backend/src/main/java/br/com/iforce/praxis/simulation/service/SimulationAdminService.java"
replace_once(
    admin,
    "        applyMedia(request.mediaUrl(), request.mediaType(), nodeEntity::setMediaUrl, nodeEntity::setMediaType);\n        versionEntity.getNodes().add(nodeEntity);",
    "        applyMedia(request.mediaUrl(), request.mediaType(), nodeEntity::setMediaUrl, nodeEntity::setMediaType);\n"
    "        nodeEntity.setMediaTranscript(trimToNull(request.mediaTranscript()));\n"
    "        nodeEntity.setMediaCaptionsUrl(trimToNull(request.mediaCaptionsUrl()));\n"
    "        nodeEntity.setMediaVersion(resolveMediaVersion(request.mediaVersion(), nodeEntity.getMediaUrl()));\n"
    "        versionEntity.getNodes().add(nodeEntity);",
)
replace_once(
    admin,
    "        if (request.mediaUrl() != null) {\n            applyMedia(request.mediaUrl(), request.mediaType(), nodeEntity::setMediaUrl, nodeEntity::setMediaType);\n        }\n        if (!nodeEntity.isFinal()",
    "        if (request.mediaUrl() != null) {\n            applyMedia(request.mediaUrl(), request.mediaType(), nodeEntity::setMediaUrl, nodeEntity::setMediaType);\n        }\n"
    "        if (request.mediaTranscript() != null) {\n            nodeEntity.setMediaTranscript(trimToNull(request.mediaTranscript()));\n        }\n"
    "        if (request.mediaCaptionsUrl() != null) {\n            nodeEntity.setMediaCaptionsUrl(trimToNull(request.mediaCaptionsUrl()));\n        }\n"
    "        if (request.mediaVersion() != null || request.mediaUrl() != null) {\n            nodeEntity.setMediaVersion(resolveMediaVersion(request.mediaVersion(), nodeEntity.getMediaUrl()));\n        }\n"
    "        if (!nodeEntity.isFinal()",
)
replace_once(
    admin,
    "        applyMedia(request.mediaUrl(), request.mediaType(), optionEntity::setMediaUrl, optionEntity::setMediaType);\n        applyCompetencyScores(optionEntity, request.competencyLevels());",
    "        applyMedia(request.mediaUrl(), request.mediaType(), optionEntity::setMediaUrl, optionEntity::setMediaType);\n"
    "        optionEntity.setMediaTranscript(trimToNull(request.mediaTranscript()));\n"
    "        optionEntity.setMediaCaptionsUrl(trimToNull(request.mediaCaptionsUrl()));\n"
    "        optionEntity.setMediaVersion(resolveMediaVersion(request.mediaVersion(), optionEntity.getMediaUrl()));\n"
    "        applyCompetencyScores(optionEntity, request.competencyLevels());",
)
replace_once(
    admin,
    "        if (request.mediaUrl() != null) {\n            applyMedia(request.mediaUrl(), request.mediaType(), optionEntity::setMediaUrl, optionEntity::setMediaType);\n        }\n        if (request.competencyLevels() != null)",
    "        if (request.mediaUrl() != null) {\n            applyMedia(request.mediaUrl(), request.mediaType(), optionEntity::setMediaUrl, optionEntity::setMediaType);\n        }\n"
    "        if (request.mediaTranscript() != null) {\n            optionEntity.setMediaTranscript(trimToNull(request.mediaTranscript()));\n        }\n"
    "        if (request.mediaCaptionsUrl() != null) {\n            optionEntity.setMediaCaptionsUrl(trimToNull(request.mediaCaptionsUrl()));\n        }\n"
    "        if (request.mediaVersion() != null || request.mediaUrl() != null) {\n            optionEntity.setMediaVersion(resolveMediaVersion(request.mediaVersion(), optionEntity.getMediaUrl()));\n        }\n"
    "        if (request.competencyLevels() != null)",
)
replace_once(
    admin,
    "        clonedNodeEntity.setMediaType(sourceNodeEntity.getMediaType());",
    "        clonedNodeEntity.setMediaType(sourceNodeEntity.getMediaType());\n"
    "        clonedNodeEntity.setMediaTranscript(sourceNodeEntity.getMediaTranscript());\n"
    "        clonedNodeEntity.setMediaCaptionsUrl(sourceNodeEntity.getMediaCaptionsUrl());\n"
    "        clonedNodeEntity.setMediaVersion(sourceNodeEntity.getMediaVersion());",
)
replace_once(
    admin,
    "        clonedOptionEntity.setMediaType(sourceOptionEntity.getMediaType());",
    "        clonedOptionEntity.setMediaType(sourceOptionEntity.getMediaType());\n"
    "        clonedOptionEntity.setMediaTranscript(sourceOptionEntity.getMediaTranscript());\n"
    "        clonedOptionEntity.setMediaCaptionsUrl(sourceOptionEntity.getMediaCaptionsUrl());\n"
    "        clonedOptionEntity.setMediaVersion(sourceOptionEntity.getMediaVersion());",
)
replace_once(
    admin,
    "    private int nextVersionNumber(String simulationId) {",
    "    private String resolveMediaVersion(String requestedVersion, String mediaUrl) {\n"
    "        if (mediaUrl == null || mediaUrl.isBlank()) {\n            return null;\n        }\n"
    "        String normalized = trimToNull(requestedVersion);\n"
    "        return normalized == null ? \"media-\" + UUID.randomUUID() : normalized;\n"
    "    }\n\n"
    "    private int nextVersionNumber(String simulationId) {",
)

# Serviço de duplicação independente também precisa preservar a equivalência.
duplicate = "backend/src/main/java/br/com/iforce/praxis/simulation/service/SimulationDuplicateService.java"
replace_all(
    duplicate,
    "        target.setMediaType(source.getMediaType());",
    "        target.setMediaType(source.getMediaType());\n"
    "        target.setMediaTranscript(source.getMediaTranscript());\n"
    "        target.setMediaCaptionsUrl(source.getMediaCaptionsUrl());\n"
    "        target.setMediaVersion(source.getMediaVersion());",
)

# 3. Bloqueios de publicação por falta de equivalência acessível.
validation = "backend/src/main/java/br/com/iforce/praxis/simulation/service/ConsistentSimulationValidationService.java"
replace_once(
    validation,
    "        appendDirectEndReportIssues(simulationVersionEntity, issues);",
    "        appendDirectEndReportIssues(simulationVersionEntity, issues);\n        appendMediaAccessibilityIssues(simulationVersionEntity, issues);",
)
replace_once(
    validation,
    "    private boolean isRelevant(",
    "    private void appendMediaAccessibilityIssues(\n"
    "            SimulationVersionEntity simulationVersionEntity,\n"
    "            List<ValidationIssueResponse> issues\n"
    "    ) {\n"
    "        for (SimulationNodeEntity node : simulationVersionEntity.getNodes()) {\n"
    "            validateMedia(node.getNodeId(), \"etapa\", node.getMediaType(), node.getMediaUrl(),\n"
    "                    node.getPlainTextDescription(), node.getMediaTranscript(),\n"
    "                    node.getMediaCaptionsUrl(), node.getMediaVersion(), issues);\n"
    "            for (SimulationOptionEntity option : node.getOptions()) {\n"
    "                validateMedia(node.getNodeId(), \"alternativa \" + option.getOptionId(), option.getMediaType(),\n"
    "                        option.getMediaUrl(), option.getPlainTextDescription(), option.getMediaTranscript(),\n"
    "                        option.getMediaCaptionsUrl(), option.getMediaVersion(), issues);\n"
    "            }\n"
    "        }\n"
    "    }\n\n"
    "    private void validateMedia(\n"
    "            String nodeId, String label, br.com.iforce.praxis.shared.model.MediaType mediaType,\n"
    "            String mediaUrl, String equivalentText, String transcript, String captionsUrl,\n"
    "            String mediaVersion, List<ValidationIssueResponse> issues\n"
    "    ) {\n"
    "        if (mediaUrl == null || mediaUrl.isBlank()) return;\n"
    "        if (mediaType == null) {\n            addMediaBlocker(nodeId, \"A \" + label + \" possui mídia sem tipo identificado.\", issues);\n            return;\n        }\n"
    "        if (!mediaUrl.startsWith(\"https://\")) addMediaBlocker(nodeId, \"A mídia da \" + label + \" deve usar URL HTTPS.\", issues);\n"
    "        if (mediaVersion == null || mediaVersion.isBlank()) addMediaBlocker(nodeId, \"A mídia da \" + label + \" precisa de uma versão imutável.\", issues);\n"
    "        if (equivalentText == null || equivalentText.isBlank()) addMediaBlocker(nodeId, \"A mídia da \" + label + \" precisa de texto equivalente acessível.\", issues);\n"
    "        if ((mediaType == br.com.iforce.praxis.shared.model.MediaType.AUDIO || mediaType == br.com.iforce.praxis.shared.model.MediaType.VIDEO)\n"
    "                && (transcript == null || transcript.isBlank())) addMediaBlocker(nodeId, \"Áudio e vídeo da \" + label + \" precisam de transcrição.\", issues);\n"
    "        if (mediaType == br.com.iforce.praxis.shared.model.MediaType.VIDEO && (captionsUrl == null || captionsUrl.isBlank()))\n"
    "            addMediaBlocker(nodeId, \"O vídeo da \" + label + \" precisa de legenda WebVTT.\", issues);\n"
    "    }\n\n"
    "    private void addMediaBlocker(String nodeId, String message, List<ValidationIssueResponse> issues) {\n"
    "        issues.add(new ValidationIssueResponse(ValidationIssueSeverity.BLOCKER, nodeId, message));\n"
    "    }\n\n"
    "    private boolean isRelevant(",
)

# 4. Exposição ao candidato e snapshot do formato servido.
for path in [
    "backend/src/main/java/br/com/iforce/praxis/candidate/dto/EtapaAtualResponse.java",
    "backend/src/main/java/br/com/iforce/praxis/candidate/dto/RespostaResponse.java",
]:
    text = read(path)
    for accent in ["midia", "mídia"]:
        old = f"        @Schema(description = \"Tipo da {accent} (IMAGE ou AUDIO).\", nullable = true)\n        MediaType tipoMidia,"
        if old in text:
            text = text.replace(old, f"        @Schema(description = \"Tipo da {accent} (IMAGE, AUDIO ou VIDEO).\", nullable = true)\n"
                "        MediaType tipoMidia,\n\n"
                "        @Schema(description = \"Transcrição textual acessível.\", nullable = true)\n"
                "        String transcricaoMidia,\n\n"
                "        @Schema(description = \"URL da legenda WebVTT.\", nullable = true)\n"
                "        String legendaMidiaUrl,\n\n"
                "        @Schema(description = \"Versão imutável da mídia apresentada.\", nullable = true)\n"
                "        String versaoMidia,")
    write(path, text)

candidate_mapper = "backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptMapper.java"
replace_once(
    candidate_mapper,
    "                        option.mediaUrl(),\n                        option.mediaType()\n",
    "                        option.mediaUrl(),\n                        option.mediaType(),\n"
    "                        option.mediaTranscript(),\n                        option.mediaCaptionsUrl(),\n"
    "                        option.mediaVersion()\n",
)
replace_once(
    candidate_mapper,
    "                node.mediaUrl(),\n                node.mediaType(),\n                alternativas",
    "                node.mediaUrl(),\n                node.mediaType(),\n"
    "                node.mediaTranscript(),\n                node.mediaCaptionsUrl(),\n"
    "                node.mediaVersion(),\n                alternativas",
)

serve_entity = "backend/src/main/java/br/com/iforce/praxis/gupy/persistence/entity/AttemptNodeServeEntity.java"
replace_once(
    serve_entity,
    "import jakarta.persistence.JoinColumn;",
    "import br.com.iforce.praxis.shared.model.MediaType;\nimport jakarta.persistence.EnumType;\nimport jakarta.persistence.Enumerated;\nimport jakarta.persistence.JoinColumn;",
)
replace_once(
    serve_entity,
    "    @Column(name = \"served_at\", nullable = false)\n    private Instant servedAt;",
    "    @Column(name = \"served_at\", nullable = false)\n    private Instant servedAt;\n\n"
    "    @Enumerated(EnumType.STRING)\n    @Column(name = \"media_type\", length = 16)\n    private MediaType mediaType;\n\n"
    "    @Column(name = \"media_version\", length = 120)\n    private String mediaVersion;",
)

attempt_service = "backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java"
replace_all(
    attempt_service,
    "        savedAttempt = persist(savedAttempt, candidateAttemptEntity);\n\n        return new ParticipacaoResponse(",
    "        savedAttempt = persist(savedAttempt, candidateAttemptEntity);\n"
    "        snapshotServedMedia(candidateAttemptEntity, currentNode);\n"
    "        candidateAttemptRepository.save(candidateAttemptEntity);\n\n"
    "        return new ParticipacaoResponse(",
)
replace_all(
    attempt_service,
    "        savedAttempt = persist(savedAttempt, candidateAttemptEntity);\n\n        return new RegistrarRespostaResponse(",
    "        savedAttempt = persist(savedAttempt, candidateAttemptEntity);\n"
    "        snapshotServedMedia(candidateAttemptEntity, nextNode);\n"
    "        candidateAttemptRepository.save(candidateAttemptEntity);\n\n"
    "        return new RegistrarRespostaResponse(",
)
replace_once(
    attempt_service,
    "    /** Agrupa uma participação do candidato com a respectiva prova aplicada. */",
    "    private void snapshotServedMedia(CandidateAttemptEntity entity, ScenarioNode node) {\n"
    "        if (node == null) return;\n"
    "        entity.getNodeServes().stream()\n"
    "                .filter(serve -> node.id().equals(serve.getNodeId()))\n"
    "                .findFirst()\n"
    "                .ifPresent(serve -> {\n"
    "                    serve.setMediaType(node.mediaType());\n"
    "                    serve.setMediaVersion(node.mediaVersion());\n"
    "                });\n"
    "    }\n\n"
    "    /** Agrupa uma participação do candidato com a respectiva prova aplicada. */",
)

# 5. Migration.
write(
    "backend/src/main/resources/db/migration/V1026__accessible_video_and_media_equivalence.sql",
    """ALTER TABLE simulation_nodes ADD COLUMN IF NOT EXISTS media_transcript VARCHAR(8000);
ALTER TABLE simulation_nodes ADD COLUMN IF NOT EXISTS media_captions_url VARCHAR(1000);
ALTER TABLE simulation_nodes ADD COLUMN IF NOT EXISTS media_version VARCHAR(120);

ALTER TABLE simulation_options ADD COLUMN IF NOT EXISTS media_transcript VARCHAR(8000);
ALTER TABLE simulation_options ADD COLUMN IF NOT EXISTS media_captions_url VARCHAR(1000);
ALTER TABLE simulation_options ADD COLUMN IF NOT EXISTS media_version VARCHAR(120);

ALTER TABLE attempt_node_serves ADD COLUMN IF NOT EXISTS media_type VARCHAR(16);
ALTER TABLE attempt_node_serves ADD COLUMN IF NOT EXISTS media_version VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_attempt_node_serves_media_quality
    ON attempt_node_serves (media_type, media_version);
""",
)

# 6. Frontend: contrato, autoria, renderização e controles acessíveis.
api = "frontend/src/lib/api/praxis-legacy.ts"
replace_once(api, 'export type MediaType = "IMAGE" | "AUDIO";', 'export type MediaType = "IMAGE" | "AUDIO" | "VIDEO";')
replace_all(
    api,
    "  tipoMidia?: MediaType | null;",
    "  tipoMidia?: MediaType | null;\n  transcricaoMidia?: string | null;\n  legendaMidiaUrl?: string | null;\n  versaoMidia?: string | null;",
)
replace_all(
    api,
    "  mediaType: MediaType | null;",
    "  mediaType: MediaType | null;\n  mediaTranscript: string | null;\n  mediaCaptionsUrl: string | null;\n  mediaVersion: string | null;",
)
replace_all(
    api,
    "  mediaType?: MediaType | null;",
    "  mediaType?: MediaType | null;\n  mediaTranscript?: string | null;\n  mediaCaptionsUrl?: string | null;\n  mediaVersion?: string | null;",
)

candidate_ui = "frontend/src/features/candidate/candidate-experience.tsx"
replace_once(candidate_ui, 'import { useCallback, useEffect, useMemo, useState, type CSSProperties } from "react";', 'import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from "react";')
regex_once(
    candidate_ui,
    r"function CandidateMedia\(\{.*?\n\}\n\nfunction formatTimer",
    '''function CandidateMedia({
  mediaUrl,
  mediaType,
  accessibleDescription,
  transcript,
  captionsUrl,
  mediaVersion,
  audioLabel,
  unsupportedAudio,
}: {
  mediaUrl: string;
  mediaType: MediaType | null;
  accessibleDescription?: string | null;
  transcript?: string | null;
  captionsUrl?: string | null;
  mediaVersion?: string | null;
  audioLabel: string;
  unsupportedAudio: string;
}) {
  const [playbackRate, setPlaybackRate] = useState(1);
  const mediaRef = useRef<HTMLMediaElement>(null);

  useEffect(() => {
    if (mediaRef.current) mediaRef.current.playbackRate = playbackRate;
  }, [playbackRate]);

  if (mediaType === "AUDIO") {
    return (
      <div className="space-y-2">
        <audio ref={mediaRef as React.RefObject<HTMLAudioElement>} controls preload="metadata" src={mediaUrl} className="w-full" aria-label={audioLabel} onClick={(event) => event.stopPropagation()}>
          {unsupportedAudio}
        </audio>
        <PlaybackRateControl value={playbackRate} onChange={setPlaybackRate} />
        {transcript && <details><summary>Transcrição</summary><p className="mt-2 whitespace-pre-wrap">{transcript}</p></details>}
      </div>
    );
  }

  if (mediaType === "VIDEO") {
    return (
      <div className="space-y-2" data-media-version={mediaVersion ?? undefined}>
        <video ref={mediaRef as React.RefObject<HTMLVideoElement>} controls preload="metadata" playsInline className="max-h-[28rem] w-full rounded-md border border-border bg-black" aria-label={accessibleDescription?.trim() || "Vídeo do cenário"} onClick={(event) => event.stopPropagation()}>
          <source src={mediaUrl} />
          {captionsUrl && <track kind="captions" src={captionsUrl} srcLang="pt-BR" label="Português" default />}
          Seu navegador não suporta vídeo.
        </video>
        <PlaybackRateControl value={playbackRate} onChange={setPlaybackRate} />
        {transcript && <details><summary>Transcrição</summary><p className="mt-2 whitespace-pre-wrap">{transcript}</p></details>}
      </div>
    );
  }

  return <img src={mediaUrl} alt={accessibleDescription?.trim() || ""} className="max-h-48 w-auto rounded-md border border-border object-contain" data-media-version={mediaVersion ?? undefined} />;
}

function PlaybackRateControl({ value, onChange }: { value: number; onChange: (value: number) => void }) {
  return (
    <label className="inline-flex items-center gap-2 text-sm">
      Velocidade
      <select value={value} onChange={(event) => onChange(Number(event.target.value))} aria-label="Velocidade de reprodução">
        {[0.5, 0.75, 1, 1.25, 1.5, 2].map((rate) => <option key={rate} value={rate}>{rate}×</option>)}
      </select>
    </label>
  );
}

function formatTimer''',
    flags=re.S,
)
replace_all(
    candidate_ui,
    "                    accessibleDescription={currentNode.descricaoAcessivel}\n                    audioLabel=",
    "                    accessibleDescription={currentNode.descricaoAcessivel}\n                    transcript={currentNode.transcricaoMidia}\n                    captionsUrl={currentNode.legendaMidiaUrl}\n                    mediaVersion={currentNode.versaoMidia}\n                    audioLabel=",
)
replace_all(
    candidate_ui,
    "                            accessibleDescription={option.descricaoAcessivel}\n                            audioLabel=",
    "                            accessibleDescription={option.descricaoAcessivel}\n                            transcript={option.transcricaoMidia}\n                            captionsUrl={option.legendaMidiaUrl}\n                            mediaVersion={option.versaoMidia}\n                            audioLabel=",
)

editor = "frontend/src/routes/nova.dialogo.tsx"
replace_once(editor, "import { GitBranch, ImagePlus, Music, Plus, Save, Trash2, X } from \"lucide-react\";", "import { Film, GitBranch, ImagePlus, Music, Plus, Save, Trash2, X } from \"lucide-react\";")
replace_all(editor, "Anexar imagem ou áudio", "Anexar imagem, áudio ou vídeo")
replace_once(
    editor,
    "  mediaType,\n  onChange,\n  disabled,\n  label = \"Anexar imagem, áudio ou vídeo\",",
    "  mediaType,\n  mediaTranscript,\n  mediaCaptionsUrl,\n  mediaVersion,\n  onChange,\n  disabled,\n  label = \"Anexar imagem, áudio ou vídeo\",",
)
replace_once(
    editor,
    "  mediaType: MediaType | null;\n  onChange: (next: { mediaUrl: string; mediaType: MediaType } | null) => void;",
    "  mediaType: MediaType | null;\n  mediaTranscript?: string | null;\n  mediaCaptionsUrl?: string | null;\n  mediaVersion?: string | null;\n  onChange: (next: { mediaUrl: string; mediaType: MediaType; mediaTranscript?: string | null; mediaCaptionsUrl?: string | null; mediaVersion?: string | null } | null) => void;",
)
replace_once(
    editor,
    "    if (!file.type.startsWith(\"image/\") && !file.type.startsWith(\"audio/\")) {\n      setError(\"Apenas imagens ou áudios são suportados.\");",
    "    if (!file.type.startsWith(\"image/\") && !file.type.startsWith(\"audio/\") && !file.type.startsWith(\"video/\")) {\n      setError(\"Apenas imagens, áudios ou vídeos são suportados.\");",
)
replace_once(editor, '        accept="image/*,audio/*"', '        accept="image/*,audio/*,video/mp4,video/webm,video/ogg,video/quicktime"')
replace_once(
    editor,
    "      {error && <p className=\"mt-2 text-xs text-danger\">{error}</p>}\n    </div>",
    "      {mediaUrl && (mediaType === \"AUDIO\" || mediaType === \"VIDEO\") && (\n        <label className=\"mt-3 block text-xs\">\n          Transcrição acessível\n          <textarea className=\"input mt-1 min-h-20\" defaultValue={mediaTranscript ?? \"\"} onBlur={(event) => onChange({ mediaUrl, mediaType: mediaType!, mediaTranscript: event.target.value, mediaCaptionsUrl, mediaVersion })} />\n        </label>\n      )}\n      {mediaUrl && mediaType === \"VIDEO\" && (\n        <label className=\"mt-3 block text-xs\">\n          URL da legenda WebVTT\n          <input className=\"input mt-1\" type=\"url\" defaultValue={mediaCaptionsUrl ?? \"\"} onBlur={(event) => onChange({ mediaUrl, mediaType, mediaTranscript, mediaCaptionsUrl: event.target.value, mediaVersion })} />\n        </label>\n      )}\n      {mediaUrl && (\n        <label className=\"mt-3 block text-xs\">\n          Versão da mídia\n          <input className=\"input mt-1\" defaultValue={mediaVersion ?? \"\"} placeholder=\"Gerada automaticamente quando vazia\" onBlur={(event) => onChange({ mediaUrl, mediaType: mediaType!, mediaTranscript, mediaCaptionsUrl, mediaVersion: event.target.value })} />\n        </label>\n      )}\n      {error && <p className=\"mt-2 text-xs text-danger\">{error}</p>}\n    </div>",
)
replace_once(
    editor,
    "  if (mediaType === \"AUDIO\") {",
    "  if (mediaType === \"VIDEO\") {\n    return (\n      <video controls preload=\"metadata\" className=\"max-h-64 w-full rounded-md border border-border bg-black\">\n        <source src={mediaUrl} />\n        Seu navegador não suporta vídeo.\n      </video>\n    );\n  }\n  if (mediaType === \"AUDIO\") {",
)
replace_all(editor, "<Music className=\"h-3.5 w-3.5\" />", "{mediaType === \"VIDEO\" ? <Film className=\"h-3.5 w-3.5\" /> : <Music className=\"h-3.5 w-3.5\" />}")
replace_all(
    editor,
    "                    mediaType={selected.mediaType}\n                    disabled=",
    "                    mediaType={selected.mediaType}\n                    mediaTranscript={selected.mediaTranscript}\n                    mediaCaptionsUrl={selected.mediaCaptionsUrl}\n                    mediaVersion={selected.mediaVersion}\n                    disabled=",
)
replace_all(
    editor,
    "                          mediaType={option.mediaType}\n                          disabled=",
    "                          mediaType={option.mediaType}\n                          mediaTranscript={option.mediaTranscript}\n                          mediaCaptionsUrl={option.mediaCaptionsUrl}\n                          mediaVersion={option.mediaVersion}\n                          disabled=",
)
replace_all(
    editor,
    "                        mediaType: next?.mediaType ?? null,\n",
    "                        mediaType: next?.mediaType ?? null,\n                        mediaTranscript: next?.mediaTranscript ?? \"\",\n                        mediaCaptionsUrl: next?.mediaCaptionsUrl ?? \"\",\n                        mediaVersion: next?.mediaVersion ?? \"\",\n",
)
# Tipos e payloads das mutations.
replace_all(editor, "      mediaType: MediaType | null;\n    }) =>", "      mediaType: MediaType | null;\n      mediaTranscript?: string | null;\n      mediaCaptionsUrl?: string | null;\n      mediaVersion?: string | null;\n    }) =>")
replace_all(editor, "      mediaType?: MediaType | null;\n    }) =>", "      mediaType?: MediaType | null;\n      mediaTranscript?: string | null;\n      mediaCaptionsUrl?: string | null;\n      mediaVersion?: string | null;\n    }) =>")
replace_all(editor, "        mediaType,\n      });", "        mediaType,\n        mediaTranscript,\n        mediaCaptionsUrl,\n        mediaVersion,\n      });")

# Manual contextual completo para a tela alterada.
manual = "frontend/src/lib/screen-manual-overrides.ts"
replace_once(
    manual,
    "export const SCREEN_MANUAL_OVERRIDES: ScreenManualDefinition[] = [",
    "export const SCREEN_MANUAL_OVERRIDES: ScreenManualDefinition[] = [\n  {\n    id: \"editor-dialogo-multimidia\",\n    title: \"Editor de diálogo e mídia acessível\",\n    purpose: \"Criar etapas, alternativas e versões equivalentes de imagem, áudio e vídeo sem alterar silenciosamente o construto avaliado.\",\n    flow: [\"Selecione a etapa.\", \"Anexe a mídia.\", \"Preencha texto equivalente e transcrição.\", \"Para vídeo, informe legenda WebVTT.\", \"Revise os bloqueios no Validador antes de publicar.\"],\n    fields: [\n      { name: \"Mídia\", description: \"Imagem, áudio ou vídeo validado por MIME type e tamanho.\" },\n      { name: \"Texto equivalente\", description: \"Descrição que permite responder sem depender do formato visual ou sonoro.\" },\n      { name: \"Transcrição\", description: \"Conteúdo textual integral de áudio ou vídeo.\" },\n      { name: \"Legenda WebVTT\", description: \"Legenda sincronizada obrigatória para vídeo.\" },\n      { name: \"Versão da mídia\", description: \"Identificador imutável gravado na tentativa para auditoria e comparação.\" },\n    ],\n    permissions: [\"Perfil EMPRESA ou especialista autorizado com permissão de editar rascunhos.\"],\n    states: [\"Sem mídia\", \"Enviando\", \"Mídia anexada\", \"Acessibilidade incompleta\", \"Pronta para validação\", \"Erro\"],\n    blocks: [\"Versão publicada ou arquivada.\", \"Formato ou tamanho não permitido.\", \"URL sem HTTPS.\", \"Texto equivalente ausente.\", \"Áudio ou vídeo sem transcrição.\", \"Vídeo sem legenda WebVTT.\"],\n    examples: [\"Vídeo de atendimento com legenda, transcrição e versão textual equivalente.\", \"Áudio de cliente com transcrição integral e controles de velocidade.\"],\n    shortcuts: [\"Use Tab para alcançar os controles.\", \"Use Espaço para reproduzir ou pausar.\", \"Abra o Validador para ver todos os bloqueios.\", \"Consulte o processo completo na Central de manuais.\"],\n    matches: (pathname) => pathname === \"/nova/dialogo\",\n  },",
)

# 7. Testes de contrato e documentação técnica.
write(
    "frontend/scripts/test-accessible-video.mjs",
    """import fs from 'node:fs';

const api = fs.readFileSync(new URL('../src/lib/api/praxis-legacy.ts', import.meta.url), 'utf8');
const candidate = fs.readFileSync(new URL('../src/features/candidate/candidate-experience.tsx', import.meta.url), 'utf8');
const editor = fs.readFileSync(new URL('../src/routes/nova.dialogo.tsx', import.meta.url), 'utf8');

for (const token of ['\"VIDEO\"', 'mediaTranscript', 'mediaCaptionsUrl', 'mediaVersion']) {
  if (!api.includes(token)) throw new Error(`Contrato ausente: ${token}`);
}
for (const token of ['<video', 'kind=\"captions\"', 'Velocidade de reprodução', 'Transcrição', 'preload=\"metadata\"']) {
  if (!candidate.includes(token)) throw new Error(`Experiência do candidato sem ${token}`);
}
for (const token of ['video/mp4', 'URL da legenda WebVTT', 'Transcrição acessível', 'Versão da mídia']) {
  if (!editor.includes(token)) throw new Error(`Editor sem ${token}`);
}
if (candidate.includes('autoPlay')) throw new Error('Vídeo não pode iniciar automaticamente.');
console.log('Contrato de vídeo acessível validado.');
""",
)
write(
    "docs/arquitetura/midia-acessivel.md",
    """# Mídia acessível e equivalência entre formatos

O Práxis aceita `IMAGE`, `AUDIO` e `VIDEO`. Formatos diferentes não são considerados equivalentes apenas por conterem a mesma situação. A autoria deve fornecer texto equivalente, transcrição para áudio/vídeo e legenda WebVTT para vídeo, e a publicação é bloqueada quando esses recursos faltam.

Cada mídia recebe uma versão imutável. O tipo e a versão apresentados são copiados para `attempt_node_serves`, permitindo separar amostras incompatíveis. A mídia nunca altera a pontuação e não substitui automaticamente o texto.

Vídeos usam controles nativos acessíveis por teclado, tela cheia, volume, pausa e controle adicional de velocidade. Reprodução automática com som não é utilizada. Uploads são verificados pelo conteúdo real com Apache Tika; imagens e áudios aceitam até 10 MB e vídeos até 100 MB nos formatos MP4, WebM, Ogg ou QuickTime.
""",
)

print("Issue #500 aplicada com sucesso")
