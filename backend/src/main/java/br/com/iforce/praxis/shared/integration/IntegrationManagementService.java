package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.shared.integration.dto.ConfigureIntegrationRequest;
import br.com.iforce.praxis.shared.integration.dto.IntegrationResponse;
import br.com.iforce.praxis.shared.integration.model.IntegrationAction;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.persistence.entity.TenantIntegrationEntity;
import br.com.iforce.praxis.shared.integration.persistence.repository.TenantIntegrationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IntegrationManagementService {

    private final CurrentTenantService currentTenantService;
    private final CurrentUserService currentUserService;
    private final TenantRepository tenantRepository;
    private final TenantIntegrationRepository tenantIntegrationRepository;
    private final IntegrationTokenAdminService integrationTokenAdminService;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public IntegrationManagementService(
            CurrentTenantService currentTenantService,
            CurrentUserService currentUserService,
            TenantRepository tenantRepository,
            TenantIntegrationRepository tenantIntegrationRepository,
            IntegrationTokenAdminService integrationTokenAdminService,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.currentTenantService = currentTenantService;
        this.currentUserService = currentUserService;
        this.tenantRepository = tenantRepository;
        this.tenantIntegrationRepository = tenantIntegrationRepository;
        this.integrationTokenAdminService = integrationTokenAdminService;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<IntegrationResponse> listIntegrations() {
        String tenantId = currentTenantService.requiredTenantId();
        Map<IntegrationProvider, TenantIntegrationEntity> configured = tenantIntegrationRepository
                .findByTenantIdOrderByProviderAsc(tenantId)
                .stream()
                .collect(Collectors.toMap(TenantIntegrationEntity::getProvider, Function.identity()));

        return IntegrationCatalog.definitions().stream()
                .map(definition -> toResponse(definition, configured.get(definition.provider())))
                .toList();
    }

    @Transactional
    public IntegrationResponse configure(String provider, ConfigureIntegrationRequest request) {
        String tenantId = currentTenantService.requiredTenantId();
        String actorUserId = currentUserService.requiredUserId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente nao encontrado."));

        TenantIntegrationEntity entity = tenantIntegrationRepository
                .findFirstByTenantIdAndProvider(tenantId, definition.provider())
                .orElseGet(() -> {
                    TenantIntegrationEntity created = new TenantIntegrationEntity();
                    created.setTenant(tenant);
                    created.setProvider(definition.provider());
                    created.setType(definition.type());
                    created.setCreatedAt(Instant.now());
                    return created;
                });

        IntegrationStatus previousStatus = entity.getStatus() == null
                ? IntegrationStatus.NAO_CONFIGURADA
                : entity.getStatus();
        Instant now = Instant.now();

        validateConfigureRequest(definition, request);
        if (usesAccessToken(definition.provider())) {
            entity.setCredentialsHash(sha256(integrationTokenAdminService.rotateToken(definition.tokenProvider()).token()));
        } else {
            entity.setCredentialsHash(hashJson(request == null ? null : request.credentials()));
        }

        entity.setType(definition.type());
        entity.setStatus(nextConfiguredStatus(definition.provider()));
        entity.setSettingsJson(toJson(request == null ? null : request.settings()));
        entity.setConfiguredAt(now);
        entity.setDisabledAt(null);
        entity.setLastErrorMessage(null);
        entity.setUpdatedAt(now);

        TenantIntegrationEntity saved = tenantIntegrationRepository.save(entity);
        appendAudit(
                tenantId,
                actorUserId,
                definition.provider(),
                previousStatus,
                saved.getStatus(),
                previousStatus == IntegrationStatus.NAO_CONFIGURADA
                        ? AuditEventType.INTEGRATION_CONFIGURED
                        : previousStatus == IntegrationStatus.DESATIVADA
                        ? AuditEventType.INTEGRATION_REACTIVATED
                        : AuditEventType.INTEGRATION_UPDATED,
                null
        );
        return toResponse(definition, saved);
    }

    @Transactional
    public IntegrationResponse disconnect(String provider) {
        String tenantId = currentTenantService.requiredTenantId();
        String actorUserId = currentUserService.requiredUserId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        TenantIntegrationEntity entity = requireIntegration(tenantId, definition.provider());
        IntegrationStatus previousStatus = entity.getStatus();
        Instant now = Instant.now();

        if (usesAccessToken(definition.provider())) {
            integrationTokenAdminService.revokeToken(definition.tokenProvider());
        }
        entity.setStatus(IntegrationStatus.DESATIVADA);
        entity.setDisabledAt(now);
        entity.setUpdatedAt(now);
        entity.setLastErrorMessage(null);
        TenantIntegrationEntity saved = tenantIntegrationRepository.save(entity);

        appendAudit(
                tenantId,
                actorUserId,
                definition.provider(),
                previousStatus,
                saved.getStatus(),
                AuditEventType.INTEGRATION_DISABLED,
                null
        );
        return toResponse(definition, saved);
    }

    @Transactional
    public IntegrationResponse sync(String provider) {
        String tenantId = currentTenantService.requiredTenantId();
        String actorUserId = currentUserService.requiredUserId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        TenantIntegrationEntity entity = requireIntegration(tenantId, definition.provider());

        if (!definition.supportsManualSync()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este provedor nao suporta sincronizacao manual.");
        }
        if (entity.getStatus() == IntegrationStatus.NAO_CONFIGURADA || entity.getStatus() == IntegrationStatus.DESATIVADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Configure a integracao antes de sincronizar.");
        }

        IntegrationStatus previousStatus = entity.getStatus();
        appendAudit(
                tenantId,
                actorUserId,
                definition.provider(),
                previousStatus,
                previousStatus,
                AuditEventType.INTEGRATION_SYNC_STARTED,
                null
        );

        entity.setLastSyncAt(Instant.now());
        entity.setStatus(IntegrationStatus.CONECTADA);
        entity.setLastErrorMessage(null);
        entity.setUpdatedAt(Instant.now());
        TenantIntegrationEntity saved = tenantIntegrationRepository.save(entity);

        appendAudit(
                tenantId,
                actorUserId,
                definition.provider(),
                previousStatus,
                saved.getStatus(),
                AuditEventType.INTEGRATION_SYNC_COMPLETED,
                null
        );
        return toResponse(definition, saved);
    }

    private TenantIntegrationEntity requireIntegration(String tenantId, IntegrationProvider provider) {
        return tenantIntegrationRepository.findFirstByTenantIdAndProvider(tenantId, provider)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Integracao ainda nao configurada para este cliente."
                ));
    }

    private IntegrationResponse toResponse(
            IntegrationCatalog.Definition definition,
            TenantIntegrationEntity entity
    ) {
        IntegrationStatus status = entity == null ? IntegrationStatus.NAO_CONFIGURADA : entity.getStatus();
        return new IntegrationResponse(
                definition.provider(),
                definition.name(),
                definition.description(),
                definition.type(),
                status,
                entity == null ? null : entity.getLastSyncAt(),
                entity == null ? null : entity.getConfiguredAt(),
                entity == null ? null : entity.getLastErrorMessage(),
                actionsFor(status, definition.supportsManualSync())
        );
    }

    private static List<IntegrationAction> actionsFor(IntegrationStatus status, boolean supportsManualSync) {
        return switch (status) {
            case NAO_CONFIGURADA -> List.of(IntegrationAction.CONFIGURE);
            case PENDENTE -> List.of(IntegrationAction.CONFIGURE);
            case CONECTADA -> supportsManualSync
                    ? List.of(IntegrationAction.VIEW, IntegrationAction.SYNC, IntegrationAction.DISCONNECT)
                    : List.of(IntegrationAction.VIEW, IntegrationAction.DISCONNECT);
            case ERRO -> List.of(IntegrationAction.VIEW_ERROR, IntegrationAction.RETRY, IntegrationAction.EDIT);
            case DESATIVADA -> List.of(IntegrationAction.REACTIVATE);
        };
    }

    private static IntegrationStatus nextConfiguredStatus(IntegrationProvider provider) {
        return provider == IntegrationProvider.CUSTOM_API ? IntegrationStatus.PENDENTE : IntegrationStatus.CONECTADA;
    }

    private static boolean usesAccessToken(IntegrationProvider provider) {
        return provider == IntegrationProvider.GUPY || provider == IntegrationProvider.RECRUTEI;
    }

    private static void validateConfigureRequest(
            IntegrationCatalog.Definition definition,
            ConfigureIntegrationRequest request
    ) {
        if (definition.provider() == IntegrationProvider.CUSTOM_API) {
            Map<String, Object> settings = request == null ? null : request.settings();
            Object baseUrl = settings == null ? null : settings.get("baseUrl");
            if (!(baseUrl instanceof String value) || value.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a URL base da API propria.");
            }
        }
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuracao da integracao invalida.");
        }
    }

    private String hashJson(Map<String, Object> value) {
        String json = toJson(value);
        return json == null ? null : sha256(json);
    }

    private void appendAudit(
            String tenantId,
            String actorUserId,
            IntegrationProvider provider,
            IntegrationStatus previousStatus,
            IntegrationStatus nextStatus,
            AuditEventType eventType,
            String error
    ) {
        auditEventService.appendIntegrationEvent(
                tenantId,
                actorUserId,
                provider.name(),
                eventType,
                "Integracao " + provider.name() + " atualizada.",
                auditMetadata(provider, previousStatus, nextStatus, error)
        );
    }

    private String auditMetadata(
            IntegrationProvider provider,
            IntegrationStatus previousStatus,
            IntegrationStatus nextStatus,
            String error
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tenantId", currentTenantService.requiredTenantId());
        metadata.put("provider", provider.name());
        metadata.put("statusAnterior", previousStatus.name());
        metadata.put("statusNovo", nextStatus.name());
        metadata.put("dataHora", Instant.now().toString());
        if (error != null) {
            metadata.put("erro", error);
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            return "{\"provider\":\"" + provider.name() + "\"}";
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel.", exception);
        }
    }
}
