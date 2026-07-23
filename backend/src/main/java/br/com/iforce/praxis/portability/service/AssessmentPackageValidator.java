package br.com.iforce.praxis.portability.service;

import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.MediaAsset;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.NodeContent;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.OptionContent;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageEnvelope;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageManifest;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageValidationResponse;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.ValidationProblem;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.VersionContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.FORMAT_VERSION;

@Component
public class AssessmentPackageValidator {

    private static final int MAX_NODES = 500;
    private static final int MAX_MEDIA_ASSETS = 1000;
    private static final long MAX_MEDIA_SIZE_BYTES = 50L * 1024L * 1024L;
    private static final Set<String> EXECUTABLE_EXTENSIONS = Set.of(
            ".exe", ".com", ".bat", ".cmd", ".sh", ".ps1", ".jar", ".msi", ".dll", ".js"
    );

    private final ObjectMapper canonicalMapper;

    public AssessmentPackageValidator(ObjectMapper objectMapper) {
        this.canonicalMapper = objectMapper.copy()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public PackageValidationResponse validate(PackageEnvelope envelope) {
        List<ValidationProblem> errors = new ArrayList<>();
        List<ValidationProblem> warnings = new ArrayList<>();
        if (envelope == null) {
            errors.add(problem("$", "PACKAGE_REQUIRED", "O pacote deve ser informado."));
            return response(null, errors, warnings, List.of(), Map.of());
        }
        if (!FORMAT_VERSION.equals(envelope.formatVersion())) {
            errors.add(problem(
                    "$.formatVersion",
                    "UNSUPPORTED_FORMAT",
                    "Formato incompatível. Versão suportada: " + FORMAT_VERSION + "."
            ));
        }
        if (envelope.manifest() == null) {
            errors.add(problem("$.manifest", "MANIFEST_REQUIRED", "O manifesto é obrigatório."));
            return response(null, errors, warnings, List.of(), Map.of());
        }

        String calculatedHash = calculateHash(envelope.manifest());
        if (!calculatedHash.equalsIgnoreCase(nullToEmpty(envelope.contentHash()))) {
            errors.add(problem(
                    "$.contentHash",
                    "HASH_MISMATCH",
                    "O conteúdo foi alterado ou corrompido após a geração do pacote."
            ));
        }

        validateAssessment(envelope.manifest(), errors);
        validateVersion(envelope.manifest().version(), errors, warnings);
        validateMedia(envelope.manifest(), errors, warnings);

        List<String> competencies = envelope.manifest().version() == null
                || envelope.manifest().version().competencies() == null
                ? List.of()
                : envelope.manifest().version().competencies().stream()
                .map(competency -> competency.name().trim())
                .distinct()
                .sorted()
                .toList();
        Map<String, String> mapping = new LinkedHashMap<>();
        if (envelope.manifest().origin() != null) {
            mapping.put(
                    envelope.manifest().origin().sourceAssessmentId(),
                    "imported-" + calculatedHash.substring(0, 8)
            );
        }
        return response(calculatedHash, errors, warnings, competencies, mapping);
    }

    public String calculateHash(PackageManifest manifest) {
        try {
            byte[] canonical = canonicalMapper.writeValueAsBytes(manifest);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("O manifesto não pode ser serializado de forma canônica.", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    public String hashText(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    private void validateAssessment(PackageManifest manifest, List<ValidationProblem> errors) {
        if (manifest.origin() == null) {
            errors.add(problem("$.manifest.origin", "ORIGIN_REQUIRED", "A origem do pacote é obrigatória."));
        }
        if (manifest.assessment() == null) {
            errors.add(problem("$.manifest.assessment", "ASSESSMENT_REQUIRED", "Os dados da avaliação são obrigatórios."));
            return;
        }
        if (isBlank(manifest.assessment().name())) {
            errors.add(problem("$.manifest.assessment.name", "NAME_REQUIRED", "O nome da avaliação é obrigatório."));
        }
        if (isBlank(manifest.assessment().description())) {
            errors.add(problem("$.manifest.assessment.description", "DESCRIPTION_REQUIRED", "A descrição é obrigatória."));
        }
    }

    private void validateVersion(
            VersionContent version,
            List<ValidationProblem> errors,
            List<ValidationProblem> warnings
    ) {
        if (version == null) {
            errors.add(problem("$.manifest.version", "VERSION_REQUIRED", "O conteúdo versionado é obrigatório."));
            return;
        }
        if (version.nodes() == null || version.nodes().isEmpty()) {
            errors.add(problem("$.manifest.version.nodes", "NODES_REQUIRED", "O grafo deve possuir etapas."));
            return;
        }
        if (version.nodes().size() > MAX_NODES) {
            errors.add(problem("$.manifest.version.nodes", "NODE_LIMIT", "O pacote excede o limite de " + MAX_NODES + " etapas."));
        }
        if (version.competencies() == null || version.competencies().isEmpty()) {
            errors.add(problem("$.manifest.version.competencies", "COMPETENCIES_REQUIRED", "Informe ao menos uma competência."));
        }

        Set<String> competencyNames = new HashSet<>();
        double weightSum = 0.0;
        if (version.competencies() != null) {
            for (int index = 0; index < version.competencies().size(); index++) {
                var competency = version.competencies().get(index);
                String path = "$.manifest.version.competencies[" + index + "]";
                if (competency == null || isBlank(competency.name())) {
                    errors.add(problem(path + ".name", "COMPETENCY_NAME_REQUIRED", "A competência deve possuir nome."));
                    continue;
                }
                String normalized = competency.name().trim().toLowerCase(Locale.ROOT);
                if (!competencyNames.add(normalized)) {
                    errors.add(problem(path + ".name", "DUPLICATE_COMPETENCY", "A competência está duplicada."));
                }
                if (competency.weight() < 0.0) {
                    errors.add(problem(path + ".weight", "INVALID_WEIGHT", "O peso não pode ser negativo."));
                }
                weightSum += competency.weight();
            }
        }
        if (Math.abs(weightSum - 1.0) > 0.001) {
            errors.add(problem(
                    "$.manifest.version.competencies",
                    "INVALID_WEIGHT_SUM",
                    "Os pesos das competências devem somar 1,0. Soma encontrada: " + weightSum + "."
            ));
        }

        Map<String, NodeContent> nodes = new HashMap<>();
        for (int nodeIndex = 0; nodeIndex < version.nodes().size(); nodeIndex++) {
            NodeContent node = version.nodes().get(nodeIndex);
            String nodePath = "$.manifest.version.nodes[" + nodeIndex + "]";
            if (node == null || isBlank(node.id())) {
                errors.add(problem(nodePath + ".id", "NODE_ID_REQUIRED", "A etapa deve possuir identificador."));
                continue;
            }
            if (nodes.put(node.id(), node) != null) {
                errors.add(problem(nodePath + ".id", "DUPLICATE_NODE", "O identificador da etapa está duplicado."));
            }
            if (node.turnIndex() < 0) {
                errors.add(problem(nodePath + ".turnIndex", "INVALID_TURN_INDEX", "A ordem da etapa não pode ser negativa."));
            }
            if (isBlank(node.message())) {
                errors.add(problem(nodePath + ".message", "NODE_MESSAGE_REQUIRED", "A etapa deve possuir mensagem."));
            }
            validateOptions(node, nodePath, competencyNames, errors, warnings);
        }

        if (isBlank(version.rootNodeId()) || !nodes.containsKey(version.rootNodeId())) {
            errors.add(problem("$.manifest.version.rootNodeId", "INVALID_ROOT", "A etapa inicial não existe no grafo."));
        }
        for (int nodeIndex = 0; nodeIndex < version.nodes().size(); nodeIndex++) {
            NodeContent node = version.nodes().get(nodeIndex);
            if (node == null || isBlank(node.id())) continue;
            String nodePath = "$.manifest.version.nodes[" + nodeIndex + "]";
            validateDestination(node.timeoutNextNodeId(), nodePath + ".timeoutNextNodeId", nodes, errors);
            if (node.options() == null) continue;
            for (int optionIndex = 0; optionIndex < node.options().size(); optionIndex++) {
                OptionContent option = node.options().get(optionIndex);
                if (option != null) {
                    validateDestination(
                            option.nextNodeId(),
                            nodePath + ".options[" + optionIndex + "].nextNodeId",
                            nodes,
                            errors
                    );
                }
            }
        }
    }

    private void validateOptions(
            NodeContent node,
            String nodePath,
            Set<String> competencyNames,
            List<ValidationProblem> errors,
            List<ValidationProblem> warnings
    ) {
        List<OptionContent> options = node.options() == null ? List.of() : node.options();
        if (node.terminal()) {
            if (!options.isEmpty()) {
                errors.add(problem(nodePath + ".options", "TERMINAL_WITH_OPTIONS", "Etapas finais não podem possuir alternativas."));
            }
            if (isBlank(node.reportText())) {
                errors.add(problem(nodePath + ".reportText", "REPORT_TEXT_REQUIRED", "Etapas finais devem possuir texto de relatório."));
            }
            return;
        }
        if (options.size() < 2 || options.size() > 4) {
            errors.add(problem(nodePath + ".options", "INVALID_OPTION_COUNT", "Etapas normais devem possuir de 2 a 4 alternativas."));
        }
        Set<String> optionIds = new HashSet<>();
        for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
            OptionContent option = options.get(optionIndex);
            String path = nodePath + ".options[" + optionIndex + "]";
            if (option == null || isBlank(option.id())) {
                errors.add(problem(path + ".id", "OPTION_ID_REQUIRED", "A alternativa deve possuir identificador."));
                continue;
            }
            if (!optionIds.add(option.id())) {
                errors.add(problem(path + ".id", "DUPLICATE_OPTION", "O identificador da alternativa está duplicado na etapa."));
            }
            if (isBlank(option.text())) {
                errors.add(problem(path + ".text", "OPTION_TEXT_REQUIRED", "A alternativa deve possuir texto."));
            }
            if (option.competencyScores() == null || option.competencyScores().isEmpty()) {
                warnings.add(problem(path + ".competencyScores", "EMPTY_SCORES", "A alternativa não possui evidência pontuada."));
                continue;
            }
            option.competencyScores().forEach((name, score) -> {
                if (!competencyNames.contains(name.trim().toLowerCase(Locale.ROOT))) {
                    errors.add(problem(path + ".competencyScores." + name, "UNKNOWN_COMPETENCY", "A pontuação referencia competência não declarada."));
                }
                if (score == null || score < 0 || score > 100) {
                    errors.add(problem(path + ".competencyScores." + name, "INVALID_SCORE", "A pontuação deve estar entre 0 e 100."));
                }
            });
        }
    }

    private void validateDestination(
            String destination,
            String path,
            Map<String, NodeContent> nodes,
            List<ValidationProblem> errors
    ) {
        if (destination != null && !destination.isBlank() && !nodes.containsKey(destination)) {
            errors.add(problem(path, "MISSING_DESTINATION", "O destino informado não existe no grafo."));
        }
    }

    private void validateMedia(
            PackageManifest manifest,
            List<ValidationProblem> errors,
            List<ValidationProblem> warnings
    ) {
        List<MediaAsset> assets = manifest.mediaAssets() == null ? List.of() : manifest.mediaAssets();
        if (assets.size() > MAX_MEDIA_ASSETS) {
            errors.add(problem("$.manifest.mediaAssets", "MEDIA_LIMIT", "O pacote excede o limite de mídias."));
        }
        Set<String> urls = new HashSet<>();
        for (int index = 0; index < assets.size(); index++) {
            MediaAsset asset = assets.get(index);
            String path = "$.manifest.mediaAssets[" + index + "]";
            if (asset == null || isBlank(asset.url())) {
                errors.add(problem(path + ".url", "MEDIA_URL_REQUIRED", "A mídia deve possuir URL."));
                continue;
            }
            urls.add(asset.url());
            validateMediaUrl(asset.url(), path + ".url", errors);
            if (asset.declaredSizeBytes() < 0 || asset.declaredSizeBytes() > MAX_MEDIA_SIZE_BYTES) {
                errors.add(problem(path + ".declaredSizeBytes", "MEDIA_SIZE_LIMIT", "A mídia excede o limite permitido de 50 MB."));
            }
            if (!hashText(asset.url()).equalsIgnoreCase(nullToEmpty(asset.sha256()))) {
                errors.add(problem(path + ".sha256", "MEDIA_HASH_MISMATCH", "A referência de mídia foi alterada."));
            }
            if ("not-declared".equalsIgnoreCase(asset.license())) {
                warnings.add(problem(path + ".license", "LICENSE_NOT_DECLARED", "Confirme a licença da mídia antes de publicar."));
            }
            if (asset.embedded()) {
                warnings.add(problem(path + ".embedded", "EMBEDDED_MEDIA_IGNORED", "A versão 1.0 importa somente referências seguras de mídia."));
            }
        }
        if (manifest.version() == null || manifest.version().nodes() == null) return;
        for (int nodeIndex = 0; nodeIndex < manifest.version().nodes().size(); nodeIndex++) {
            NodeContent node = manifest.version().nodes().get(nodeIndex);
            if (node == null) continue;
            List<String> nodeUrls = List.of(
                    nullToEmpty(node.mediaUrl()),
                    nullToEmpty(node.audioDescriptionUrl()),
                    nullToEmpty(node.mediaCaptionsUrl())
            );
            validateReferencedMedia(nodeUrls, urls, "$.manifest.version.nodes[" + nodeIndex + "]", errors);
            if (node.options() == null) continue;
            for (int optionIndex = 0; optionIndex < node.options().size(); optionIndex++) {
                OptionContent option = node.options().get(optionIndex);
                if (option == null) continue;
                List<String> optionUrls = List.of(
                        nullToEmpty(option.mediaUrl()),
                        nullToEmpty(option.audioDescriptionUrl()),
                        nullToEmpty(option.mediaCaptionsUrl())
                );
                validateReferencedMedia(
                        optionUrls,
                        urls,
                        "$.manifest.version.nodes[" + nodeIndex + "].options[" + optionIndex + "]",
                        errors
                );
            }
        }
    }

    private void validateReferencedMedia(
            List<String> references,
            Set<String> declaredUrls,
            String path,
            List<ValidationProblem> errors
    ) {
        for (String reference : references) {
            if (reference.isBlank()) continue;
            validateMediaUrl(reference, path, errors);
            if (!declaredUrls.contains(reference)) {
                errors.add(problem(path, "UNDECLARED_MEDIA", "A referência de mídia não aparece no manifesto de ativos."));
            }
        }
    }

    private void validateMediaUrl(String url, String path, List<ValidationProblem> errors) {
        String normalized = url.toLowerCase(Locale.ROOT).split("[?#]", 2)[0];
        if (!(normalized.startsWith("https://") || normalized.startsWith("/"))) {
            errors.add(problem(path, "INSECURE_MEDIA_URL", "A mídia deve usar HTTPS ou caminho interno absoluto."));
        }
        for (String extension : EXECUTABLE_EXTENSIONS) {
            if (normalized.endsWith(extension)) {
                errors.add(problem(path, "EXECUTABLE_MEDIA", "Arquivos executáveis não são permitidos."));
                break;
            }
        }
    }

    private PackageValidationResponse response(
            String calculatedHash,
            List<ValidationProblem> errors,
            List<ValidationProblem> warnings,
            List<String> competencies,
            Map<String, String> mapping
    ) {
        return new PackageValidationResponse(
                errors.isEmpty(),
                errors.isEmpty(),
                calculatedHash,
                List.copyOf(errors),
                List.copyOf(warnings),
                competencies,
                Map.copyOf(mapping)
        );
    }

    private ValidationProblem problem(String path, String code, String message) {
        return new ValidationProblem(path, code, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
