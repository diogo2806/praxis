package br.com.iforce.praxis.featureflag.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.EvaluationRequest;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.EvaluationResponse;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.FrontendFlagsResponse;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.GovernanceSummary;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.MetricRequest;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.MetricResponse;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.Response;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.UpsertRequest;
import br.com.iforce.praxis.featureflag.persistence.entity.FeatureFlagEntity;
import br.com.iforce.praxis.featureflag.persistence.entity.FeatureFlagMetricEntity;
import br.com.iforce.praxis.featureflag.persistence.repository.FeatureFlagMetricRepository;
import br.com.iforce.praxis.featureflag.persistence.repository.FeatureFlagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FeatureFlagService {

    private static final Pattern SCORING_KEY_PATTERN = Pattern.compile(
            ".*(score|scoring|pontuacao|gabarito|answer-key|answer_key).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern METRIC_PATTERN = Pattern.compile("[a-z][a-z0-9._-]{1,59}");

    private final FeatureFlagRepository featureFlagRepository;
    private final FeatureFlagMetricRepository metricRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;
    private final String currentEnvironment;

    public FeatureFlagService(
            FeatureFlagRepository featureFlagRepository,
            FeatureFlagMetricRepository metricRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper,
            @Value("${praxis.environment:${spring.profiles.active:default}}") String currentEnvironment
    ) {
        this.featureFlagRepository = featureFlagRepository;
        this.metricRepository = metricRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
        this.currentEnvironment = normalize(currentEnvironment);
    }

    @Transactional(readOnly = true)
    public GovernanceSummary governance(String search, Boolean active) {
        Instant now = Instant.now();
        String normalizedSearch = normalize(search);
        List<Response> flags = featureFlagRepository.findAllByOrderByKeyAsc().stream()
                .filter(flag -> normalizedSearch == null
                        || normalize(flag.getKey()).contains(normalizedSearch)
                        || normalize(flag.getDescription()).contains(normalizedSearch)
                        || normalize(flag.getOwner()).contains(normalizedSearch))
                .filter(flag -> active == null || flag.isActive() == active)
                .map(flag -> toResponse(flag, now))
                .toList();
        List<Response> expired = flags.stream()
                .filter(flag -> "EXPIRED".equals(flag.status()))
                .toList();
        return new GovernanceSummary(
                flags,
                expired,
                flags.stream().filter(Response::active).count(),
                flags.stream().filter(Response::killSwitch).count(),
                now
        );
    }

    @Transactional
    public Response create(String actorUserId, UpsertRequest request) {
        validateRequest(request, null);
        if (featureFlagRepository.existsByKey(request.key())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma feature flag com esta chave.");
        }

        Instant now = Instant.now();
        FeatureFlagEntity entity = new FeatureFlagEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCreatedBy(actorUserId);
        entity.setCreatedAt(now);
        apply(entity, request, actorUserId, now);
        FeatureFlagEntity saved = featureFlagRepository.save(entity);
        audit(actorUserId, saved, AuditEventType.FEATURE_FLAG_CREATED, "Feature flag criada.");
        return toResponse(saved, now);
    }

    @Transactional
    public Response update(String actorUserId, String flagId, UpsertRequest request) {
        FeatureFlagEntity entity = required(flagId);
        validateRequest(request, entity);
        featureFlagRepository.findByKey(request.key())
                .filter(found -> !found.getId().equals(flagId))
                .ifPresent(found -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma feature flag com esta chave.");
                });

        Instant now = Instant.now();
        apply(entity, request, actorUserId, now);
        FeatureFlagEntity saved = featureFlagRepository.save(entity);
        audit(actorUserId, saved, AuditEventType.FEATURE_FLAG_UPDATED, "Feature flag atualizada.");
        return toResponse(saved, now);
    }

    @Transactional
    public Response setActive(String actorUserId, String flagId, boolean enabled) {
        FeatureFlagEntity entity = required(flagId);
        entity.setActive(enabled);
        entity.setUpdatedBy(actorUserId);
        entity.setUpdatedAt(Instant.now());
        FeatureFlagEntity saved = featureFlagRepository.save(entity);
        audit(
                actorUserId,
                saved,
                enabled ? AuditEventType.FEATURE_FLAG_ACTIVATED : AuditEventType.FEATURE_FLAG_DEACTIVATED,
                enabled ? "Feature flag ativada." : "Feature flag desativada."
        );
        return toResponse(saved, Instant.now());
    }

    @Transactional
    public Response setKillSwitch(String actorUserId, String flagId, boolean enabled) {
        FeatureFlagEntity entity = required(flagId);
        entity.setKillSwitch(enabled);
        entity.setUpdatedBy(actorUserId);
        entity.setUpdatedAt(Instant.now());
        FeatureFlagEntity saved = featureFlagRepository.save(entity);
        audit(actorUserId, saved, AuditEventType.FEATURE_FLAG_KILL_SWITCH_CHANGED,
                enabled ? "Kill switch acionado." : "Kill switch liberado.");
        return toResponse(saved, Instant.now());
    }

    @Transactional(readOnly = true)
    public EvaluationResponse evaluate(String flagId, EvaluationRequest request) {
        return evaluateEntity(required(flagId), withDefaultEnvironment(request), Instant.now());
    }

    @Transactional(readOnly = true)
    public FrontendFlagsResponse frontendFlags(EvaluationRequest request) {
        Instant now = Instant.now();
        EvaluationRequest context = withDefaultEnvironment(request);
        Map<String, Boolean> flags = new LinkedHashMap<>();
        featureFlagRepository.findAllByOrderByKeyAsc().stream()
                .filter(FeatureFlagEntity::isFrontendExposed)
                .forEach(flag -> flags.put(flag.getKey(), evaluateEntity(flag, context, now).enabled()));
        return new FrontendFlagsResponse(Map.copyOf(flags), now);
    }

    @Transactional
    public MetricResponse recordMetric(String flagId, MetricRequest request) {
        FeatureFlagEntity flag = required(flagId);
        String variant = request.variant() == null ? "" : request.variant().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ON", "OFF").contains(variant)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A variante deve ser ON ou OFF.");
        }
        String metric = normalize(request.metric());
        if (metric == null || !METRIC_PATTERN.matcher(metric).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Métrica inválida.");
        }

        FeatureFlagMetricEntity entity = metricRepository
                .findByFlagKeyAndVariantAndMetric(flag.getKey(), variant, metric)
                .orElseGet(FeatureFlagMetricEntity::new);
        entity.setFlagKey(flag.getKey());
        entity.setVariant(variant);
        entity.setMetric(metric);
        entity.setSampleCount(entity.getSampleCount() + 1);
        entity.setTotalValue(entity.getTotalValue() + request.value());
        entity.setUpdatedAt(Instant.now());
        return toMetricResponse(metricRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<MetricResponse> metrics(String flagId) {
        FeatureFlagEntity flag = required(flagId);
        return metricRepository.findByFlagKeyOrderByMetricAscVariantAsc(flag.getKey()).stream()
                .map(FeatureFlagService::toMetricResponse)
                .toList();
    }

    private EvaluationResponse evaluateEntity(
            FeatureFlagEntity flag,
            EvaluationRequest request,
            Instant now
    ) {
        if (!flag.isActive()) {
            return result(flag, flag.isDefaultEnabled(), "INACTIVE_DEFAULT", -1, now);
        }
        if (isExpired(flag, now)) {
            return result(flag, flag.isDefaultEnabled(), "EXPIRED_DEFAULT", -1, now);
        }
        if (flag.isKillSwitch()) {
            return result(flag, false, "KILL_SWITCH", -1, now);
        }
        if (contains(flag.getUserTargets(), request.userId())) {
            return result(flag, true, "USER", -1, now);
        }
        if (contains(flag.getCompanyTargets(), request.companyId())) {
            return result(flag, true, "COMPANY", -1, now);
        }
        if (matchesAny(flag.getRoleTargets(), request.roles())) {
            return result(flag, true, "ROLE", -1, now);
        }
        if (contains(flag.getPlanTargets(), request.plan())) {
            return result(flag, true, "PLAN", -1, now);
        }
        if (contains(flag.getEnvironmentTargets(), request.environment())) {
            return result(flag, true, "ENVIRONMENT", -1, now);
        }

        int bucket = rolloutBucket(flag.getKey(), request.stableIdentifier(), request.companyId(), request.userId());
        if (flag.getRolloutPercentage() > 0 && bucket >= 0) {
            boolean enabled = bucket < flag.getRolloutPercentage() * 100;
            return result(flag, enabled, "ROLLOUT", bucket, now);
        }
        if (flag.getGlobalOverride() != null) {
            return result(flag, flag.getGlobalOverride(), "GLOBAL", -1, now);
        }
        return result(flag, flag.isDefaultEnabled(), "DEFAULT", -1, now);
    }

    private static EvaluationResponse result(
            FeatureFlagEntity flag,
            boolean enabled,
            String reason,
            int bucket,
            Instant now
    ) {
        return new EvaluationResponse(flag.getKey(), enabled, reason, enabled ? "ON" : "OFF", bucket, now);
    }

    private EvaluationRequest withDefaultEnvironment(EvaluationRequest request) {
        if (request == null) {
            return new EvaluationRequest(null, null, null, Set.of(), currentEnvironment, null);
        }
        return new EvaluationRequest(
                request.companyId(),
                request.plan(),
                request.userId(),
                request.roles() == null ? Set.of() : request.roles(),
                request.environment() == null || request.environment().isBlank()
                        ? currentEnvironment
                        : request.environment(),
                request.stableIdentifier()
        );
    }

    private void apply(FeatureFlagEntity entity, UpsertRequest request, String actorUserId, Instant now) {
        entity.setKey(request.key().trim().toLowerCase(Locale.ROOT));
        entity.setDescription(request.description().trim());
        entity.setOwner(request.owner().trim());
        entity.setDefaultEnabled(request.defaultEnabled());
        entity.setGlobalOverride(request.globalOverride());
        entity.setActive(request.active());
        entity.setKillSwitch(request.killSwitch());
        entity.setFrontendExposed(request.frontendExposed());
        entity.setTemporary(request.temporary());
        entity.setExpiresAt(request.expiresAt());
        entity.setRemovalPlan(trimToNull(request.removalPlan()));
        entity.setEnvironmentTargets(join(request.environments()));
        entity.setCompanyTargets(join(request.companyIds()));
        entity.setPlanTargets(join(request.plans()));
        entity.setUserTargets(join(request.userIds()));
        entity.setRoleTargets(join(request.roles()));
        entity.setRolloutPercentage(request.rolloutPercentage());
        entity.setAffectsScoring(false);
        entity.setUpdatedBy(actorUserId);
        entity.setUpdatedAt(now);
    }

    private void validateRequest(UpsertRequest request, FeatureFlagEntity existing) {
        if (request.affectsScoring() || SCORING_KEY_PATTERN.matcher(request.key()).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Feature flags não podem alterar pontuação, gabarito ou tratamento de avaliações publicadas."
            );
        }
        if (request.temporary() && request.expiresAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Flags temporárias exigem data de expiração.");
        }
        if (request.temporary() && trimToNull(request.removalPlan()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Flags temporárias exigem plano de remoção.");
        }
        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())
                && (existing == null || !request.expiresAt().equals(existing.getExpiresAt()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A expiração deve estar no futuro.");
        }
    }

    private FeatureFlagEntity required(String flagId) {
        return featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature flag não encontrada."));
    }

    private void audit(
            String actorUserId,
            FeatureFlagEntity flag,
            AuditEventType eventType,
            String message
    ) {
        auditEventService.auditAdminAction(
                actorUserId,
                auditTarget(flag),
                eventType,
                message,
                metadata(flag)
        );
    }

    private String auditTarget(FeatureFlagEntity flag) {
        Set<String> companies = split(flag.getCompanyTargets());
        return companies.size() == 1 ? companies.iterator().next() : AuditEventService.PLATFORM_EMPRESA_ID;
    }

    private String metadata(FeatureFlagEntity flag) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("featureFlagId", flag.getId());
        metadata.put("key", flag.getKey());
        metadata.put("owner", flag.getOwner());
        metadata.put("active", flag.isActive());
        metadata.put("killSwitch", flag.isKillSwitch());
        metadata.put("rolloutPercentage", flag.getRolloutPercentage());
        metadata.put("expiresAt", flag.getExpiresAt());
        metadata.put("companyTargets", split(flag.getCompanyTargets()));
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar auditoria.", exception);
        }
    }

    private static Response toResponse(FeatureFlagEntity flag, Instant now) {
        return new Response(
                flag.getId(),
                flag.getKey(),
                flag.getDescription(),
                flag.getOwner(),
                flag.isDefaultEnabled(),
                flag.getGlobalOverride(),
                flag.isActive(),
                flag.isKillSwitch(),
                flag.isFrontendExposed(),
                flag.isTemporary(),
                flag.getExpiresAt(),
                flag.getRemovalPlan(),
                split(flag.getEnvironmentTargets()),
                split(flag.getCompanyTargets()),
                split(flag.getPlanTargets()),
                split(flag.getUserTargets()),
                split(flag.getRoleTargets()),
                flag.getRolloutPercentage(),
                flag.isAffectsScoring(),
                status(flag, now),
                flag.getCreatedBy(),
                flag.getUpdatedBy(),
                flag.getCreatedAt(),
                flag.getUpdatedAt(),
                flag.getVersion()
        );
    }

    private static MetricResponse toMetricResponse(FeatureFlagMetricEntity entity) {
        double average = entity.getSampleCount() == 0 ? 0 : entity.getTotalValue() / entity.getSampleCount();
        return new MetricResponse(
                entity.getFlagKey(),
                entity.getVariant(),
                entity.getMetric(),
                entity.getSampleCount(),
                entity.getTotalValue(),
                average,
                entity.getUpdatedAt()
        );
    }

    private static String status(FeatureFlagEntity flag, Instant now) {
        if (isExpired(flag, now)) {
            return "EXPIRED";
        }
        if (!flag.isActive()) {
            return "INACTIVE";
        }
        if (flag.isKillSwitch()) {
            return "KILLED";
        }
        return "ACTIVE";
    }

    private static boolean isExpired(FeatureFlagEntity flag, Instant now) {
        return flag.isTemporary() && flag.getExpiresAt() != null && !flag.getExpiresAt().isAfter(now);
    }

    private static int rolloutBucket(String key, String stableIdentifier, String companyId, String userId) {
        String identifier = firstNonBlank(stableIdentifier, userId, companyId);
        if (identifier == null) {
            return -1;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((key + ":" + identifier).getBytes(StandardCharsets.UTF_8));
            return Math.floorMod(ByteBuffer.wrap(digest).getInt(), 10_000);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    private static String firstNonBlank(String... values) {
        return Arrays.stream(values)
                .map(FeatureFlagService::trimToNull)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }

    private static boolean matchesAny(String stored, Set<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        Set<String> targets = split(stored);
        return values.stream().map(FeatureFlagService::normalize).anyMatch(targets::contains);
    }

    private static boolean contains(String stored, String value) {
        String normalizedValue = normalize(value);
        return normalizedValue != null && split(stored).contains(normalizedValue);
    }

    private static String join(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .map(FeatureFlagService::normalize)
                .filter(value -> value != null)
                .sorted()
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    private static Set<String> split(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(FeatureFlagService::normalize)
                .filter(item -> item != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
