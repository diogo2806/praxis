package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.auth.service.CurrentUserService;

import br.com.iforce.praxis.shared.integration.dto.ConfigureIntegrationRequest;

import br.com.iforce.praxis.shared.integration.dto.GenerateIntegrationTokenResponse;

import br.com.iforce.praxis.shared.integration.dto.IntegrationResponse;

import br.com.iforce.praxis.shared.integration.model.IntegrationAction;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;

import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;

import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;

import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;

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

    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final EmpresaRepository empresaRepository;
    private final EmpresaIntegrationRepository empresaIntegrationRepository;
    private final IntegrationTokenAdminService integrationTokenAdminService;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public IntegrationManagementService(
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            EmpresaRepository empresaRepository,
            EmpresaIntegrationRepository empresaIntegrationRepository,
            IntegrationTokenAdminService integrationTokenAdminService,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.empresaRepository = empresaRepository;
        this.empresaIntegrationRepository = empresaIntegrationRepository;
        this.integrationTokenAdminService = integrationTokenAdminService;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<IntegrationResponse> listIntegrations() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        Map<IntegrationProvider, EmpresaIntegrationEntity> configured = empresaIntegrationRepository
                .findByEmpresaIdOrderByProviderAsc(empresaId)
                .stream()
                .collect(Collectors.toMap(EmpresaIntegrationEntity::getProvider, Function.identity()));

        return IntegrationCatalog.definitions().stream()
                .map(definition -> toResponse(definition, configured.get(definition.provider())))
                .toList();
    }

    @Transactional
    public IntegrationResponse configure(String provider, ConfigureIntegrationRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        EmpresaEntity empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente nao encontrado."));

        EmpresaIntegrationEntity entity = empresaIntegrationRepository
                .findFirstByEmpresaIdAndProvider(empresaId, definition.provider())
                .orElseGet(() -> {
                    EmpresaIntegrationEntity created = new EmpresaIntegrationEntity();
                    created.setEmpresa(empresa);
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
            String newToken = integrationTokenAdminService.rotateToken(definition.tokenProvider()).token();
            entity.setCredentialsHash(sha256(newToken));
            entity.setTokenPreview(buildTokenPreview(newToken));
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

        EmpresaIntegrationEntity saved = empresaIntegrationRepository.save(entity);
        appendAudit(
                empresaId,
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
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        EmpresaIntegrationEntity entity = requireIntegration(empresaId, definition.provider());
        IntegrationStatus previousStatus = entity.getStatus();
        Instant now = Instant.now();

        if (usesAccessToken(definition.provider())) {
            integrationTokenAdminService.revokeToken(definition.tokenProvider());
        }
        entity.setStatus(IntegrationStatus.DESATIVADA);
        entity.setDisabledAt(now);
        entity.setUpdatedAt(now);
        entity.setLastErrorMessage(null);
        EmpresaIntegrationEntity saved = empresaIntegrationRepository.save(entity);

        appendAudit(
                empresaId,
                actorUserId,
                definition.provider(),
                previousStatus,
                saved.getStatus(),
                AuditEventType.INTEGRATION_DISABLED,
                null
        );
        return toResponse(definition, saved);
    }

    @Transactional(readOnly = true)
    public IntegrationResponse getIntegration(String provider) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        EmpresaIntegrationEntity entity = empresaIntegrationRepository
                .findFirstByEmpresaIdAndProvider(empresaId, definition.provider())
                .orElse(null);
        return toResponse(definition, entity);
    }

    @Transactional
    public IntegrationResponse reactivate(String provider) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        EmpresaIntegrationEntity entity = requireIntegration(empresaId, definition.provider());

        if (entity.getStatus() != IntegrationStatus.DESATIVADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Integração não está desativada.");
        }
        IntegrationStatus previousStatus = entity.getStatus();
        Instant now = Instant.now();

        if (usesAccessToken(definition.provider())) {
            String newToken = integrationTokenAdminService.rotateToken(definition.tokenProvider()).token();
            entity.setCredentialsHash(sha256(newToken));
            entity.setTokenPreview(buildTokenPreview(newToken));
        }
        entity.setStatus(IntegrationStatus.CONECTADA);
        entity.setDisabledAt(null);
        entity.setLastErrorMessage(null);
        entity.setUpdatedAt(now);
        EmpresaIntegrationEntity saved = empresaIntegrationRepository.save(entity);

        appendAudit(empresaId, actorUserId, definition.provider(), previousStatus, saved.getStatus(),
                AuditEventType.INTEGRATION_REACTIVATED, null);
        return toResponse(definition, saved);
    }

    @Transactional
    public IntegrationResponse sync(String provider) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        EmpresaIntegrationEntity entity = requireIntegration(empresaId, definition.provider());

        if (!definition.supportsManualSync()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este provedor nao suporta sincronizacao manual.");
        }
        if (entity.getStatus() == IntegrationStatus.NAO_CONFIGURADA || entity.getStatus() == IntegrationStatus.DESATIVADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Configure a integracao antes de sincronizar.");
        }

        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Sincronizacao manual nao implementada para este provedor.");
    }

    @Transactional
    public IntegrationResponse testConnection(String provider) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        EmpresaIntegrationEntity entity = requireIntegration(empresaId, definition.provider());

        if (!usesAccessToken(definition.provider())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este provedor nao possui teste de conexao.");
        }
        if (entity.getStatus() == IntegrationStatus.NAO_CONFIGURADA || entity.getStatus() == IntegrationStatus.DESATIVADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Configure a integracao antes de testar a conexao.");
        }
        if (entity.getCredentialsHash() == null || entity.getCredentialsHash().isBlank()) {
            entity.setStatus(IntegrationStatus.ERRO);
            entity.setLastErrorMessage("Token de integracao ausente. Gere um novo token e configure o provedor.");
            entity.setUpdatedAt(Instant.now());
            empresaIntegrationRepository.save(entity);
            throw new ResponseStatusException(HttpStatus.CONFLICT, entity.getLastErrorMessage());
        }

        entity.setStatus(IntegrationStatus.CONECTADA);
        entity.setLastErrorMessage(null);
        entity.setUpdatedAt(Instant.now());
        EmpresaIntegrationEntity saved = empresaIntegrationRepository.save(entity);
        return toResponse(definition, saved);
    }

    @Transactional
    public void recordActivity(String empresaId, IntegrationProvider provider) {
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider.name());
        EmpresaIntegrationEntity entity = empresaIntegrationRepository
                .findFirstByEmpresaIdAndProvider(empresaId, provider)
                .orElse(null);
        if (entity == null) {
            return;
        }

        Instant now = Instant.now();
        if (provider != IntegrationProvider.CUSTOM_API) {
            entity.setType(definition.type());
        }
        entity.setStatus(IntegrationStatus.CONECTADA);
        entity.setLastSyncAt(now);
        entity.setLastErrorMessage(null);
        entity.setUpdatedAt(now);
        empresaIntegrationRepository.save(entity);
    }

    @Transactional
    public GenerateIntegrationTokenResponse generateToken(String provider) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        EmpresaEntity empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente nao encontrado."));

        EmpresaIntegrationEntity entity = empresaIntegrationRepository
                .findFirstByEmpresaIdAndProvider(empresaId, definition.provider())
                .orElseGet(() -> {
                    EmpresaIntegrationEntity created = new EmpresaIntegrationEntity();
                    created.setEmpresa(empresa);
                    created.setProvider(definition.provider());
                    created.setType(definition.type());
                    created.setStatus(IntegrationStatus.PENDENTE);
                    created.setCreatedAt(Instant.now());
                    return created;
                });

        boolean isRotation = entity.getCredentialsHash() != null;
        String rawToken = integrationTokenAdminService.rotateToken(definition.tokenProvider()).token();
        String preview = buildTokenPreview(rawToken);
        Instant now = Instant.now();

        entity.setCredentialsHash(sha256(rawToken));
        entity.setTokenPreview(preview);
        entity.setConfiguredAt(entity.getConfiguredAt() != null ? entity.getConfiguredAt() : now);
        entity.setUpdatedAt(now);
        empresaIntegrationRepository.save(entity);

        AuditEventType auditType = isRotation ? AuditEventType.INTEGRATION_TOKEN_ROTATED : AuditEventType.INTEGRATION_TOKEN_CREATED;
        appendAudit(empresaId, actorUserId, definition.provider(), entity.getStatus(), entity.getStatus(), auditType, null);

        return new GenerateIntegrationTokenResponse(definition.provider().name(), rawToken, preview, now);
    }

    @Transactional
    public void revokeProviderToken(String provider) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        IntegrationCatalog.Definition definition = IntegrationCatalog.requireDefinition(provider);
        EmpresaIntegrationEntity entity = requireIntegration(empresaId, definition.provider());
        IntegrationStatus previousStatus = entity.getStatus();
        Instant now = Instant.now();

        integrationTokenAdminService.revokeToken(definition.tokenProvider());
        entity.setCredentialsHash(null);
        entity.setTokenPreview(null);
        entity.setStatus(IntegrationStatus.DESATIVADA);
        entity.setDisabledAt(now);
        entity.setUpdatedAt(now);
        empresaIntegrationRepository.save(entity);

        appendAudit(empresaId, actorUserId, definition.provider(), previousStatus, IntegrationStatus.DESATIVADA,
                AuditEventType.INTEGRATION_TOKEN_REVOKED, null);
    }

    private EmpresaIntegrationEntity requireIntegration(String empresaId, IntegrationProvider provider) {
        return empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(empresaId, provider)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Integracao ainda nao configurada para este cliente."
                ));
    }

    private IntegrationResponse toResponse(
            IntegrationCatalog.Definition definition,
            EmpresaIntegrationEntity entity
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
                entity == null ? null : entity.getTokenPreview(),
                actionsFor(status, definition.provider(), definition.supportsManualSync())
        );
    }

    private static List<IntegrationAction> actionsFor(
            IntegrationStatus status,
            IntegrationProvider provider,
            boolean supportsManualSync
    ) {
        boolean isCustomApi = provider == IntegrationProvider.CUSTOM_API;
        return switch (status) {
            case NAO_CONFIGURADA -> isCustomApi
                    ? List.of(IntegrationAction.VIEW_DOCS, IntegrationAction.GENERATE_TOKEN)
                    : List.of(IntegrationAction.CONFIGURE);
            case PENDENTE -> List.of(IntegrationAction.CONFIGURE);
            case CONECTADA -> supportsManualSync
                    ? List.of(IntegrationAction.VIEW, IntegrationAction.SYNC, IntegrationAction.DISCONNECT)
                    : isCustomApi
                    ? List.of(IntegrationAction.VIEW, IntegrationAction.DISCONNECT)
                    : List.of(IntegrationAction.VIEW, IntegrationAction.TEST_CONNECTION, IntegrationAction.DISCONNECT);
            case ERRO -> isCustomApi
                    ? List.of(IntegrationAction.VIEW_ERROR, IntegrationAction.RETRY, IntegrationAction.EDIT)
                    : List.of(IntegrationAction.VIEW_ERROR, IntegrationAction.TEST_CONNECTION, IntegrationAction.EDIT);
            case DESATIVADA -> List.of(IntegrationAction.REACTIVATE, IntegrationAction.VIEW, IntegrationAction.DISCONNECT);
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
            String empresaId,
            String actorUserId,
            IntegrationProvider provider,
            IntegrationStatus previousStatus,
            IntegrationStatus nextStatus,
            AuditEventType eventType,
            String error
    ) {
        auditEventService.appendIntegrationEvent(
                empresaId,
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
        metadata.put("empresaId", currentEmpresaService.requiredEmpresaId());
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

    private static String buildTokenPreview(String token) {
        if (token == null || token.length() <= 8) return "****";
        return token.substring(0, 8) + "****";
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
