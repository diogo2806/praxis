package br.com.iforce.praxis.shared.integration.service;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.config.PraxisProperties;

import br.com.iforce.praxis.gupy.delivery.service.GupyOutboundUrlValidator;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;

import br.com.iforce.praxis.shared.integration.dto.ConfigureGenericWebhookRequest;

import br.com.iforce.praxis.shared.integration.dto.GenericWebhookConfigResponse;

import br.com.iforce.praxis.shared.integration.dto.WebhookSecretResponse;

import br.com.iforce.praxis.shared.integration.dto.WebhookTestResponse;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;

import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;

import br.com.iforce.praxis.shared.integration.model.IntegrationType;

import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;

import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.client.RestClient;

import org.springframework.web.server.ResponseStatusException;


import java.net.URI;

import java.nio.charset.StandardCharsets;

import java.security.SecureRandom;

import java.time.Instant;

import java.util.ArrayList;

import java.util.Base64;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;


/**
 * Entrega genérica de resultados a um webhook do cliente (integração
 * {@code CUSTOM_API}), assinada por HMAC. Espelha a confiabilidade do envio à
 * Gupy ({@code ResultWebhookClient}), mas lê a URL e o segredo do próprio
 * cliente a partir do {@code settingsJson} da integração.
 *
 * <p>Também concentra a configuração do webhook (URL, eventos e segredo) e o
 * envio de um evento de teste, usados pela tela de API e webhooks.</p>
 */
@Slf4j
@Service
public class GenericWebhookDeliveryService {

    /** Evento de resultado pronto, único evento entregue por ora. */
    public static final String RESULT_READY_EVENT = "RESULT_READY";

    private static final int RESPONSE_SNIPPET_LIMIT = 300;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmpresaIntegrationRepository empresaIntegrationRepository;
    private final EmpresaRepository empresaRepository;
    private final CurrentEmpresaService currentEmpresaService;
    private final HmacSignatureService hmacSignatureService;
    private final GupyOutboundUrlValidator outboundUrlValidator;
    private final PraxisProperties praxisProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GenericWebhookDeliveryService(
            EmpresaIntegrationRepository empresaIntegrationRepository,
            EmpresaRepository empresaRepository,
            CurrentEmpresaService currentEmpresaService,
            HmacSignatureService hmacSignatureService,
            GupyOutboundUrlValidator outboundUrlValidator,
            PraxisProperties praxisProperties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder
    ) {
        this.empresaIntegrationRepository = empresaIntegrationRepository;
        this.empresaRepository = empresaRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.hmacSignatureService = hmacSignatureService;
        this.outboundUrlValidator = outboundUrlValidator;
        this.praxisProperties = praxisProperties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    // --- Configuração ---------------------------------------------------------

    /** Salva a URL/eventos do webhook do cliente, gerando o segredo se ainda não houver. */
    @Transactional
    public GenericWebhookConfigResponse configure(ConfigureGenericWebhookRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        outboundUrlValidator.validate(request.webhookUrl());

        EmpresaIntegrationEntity entity = findOrCreate(empresaId);
        WebhookSettings current = readSettings(entity);
        String secret = current.secret() == null ? generateSecret() : current.secret();
        List<String> events = request.events() == null || request.events().isEmpty()
                ? List.of(RESULT_READY_EVENT)
                : request.events();

        WebhookSettings updated = new WebhookSettings(request.webhookUrl().trim(), events, secret, null, null);
        entity.setSettingsJson(writeSettings(updated));
        entity.setType(IntegrationType.WEBHOOK);
        entity.setStatus(IntegrationStatus.CONECTADA);
        entity.setConfiguredAt(entity.getConfiguredAt() == null ? Instant.now() : entity.getConfiguredAt());
        entity.setDisabledAt(null);
        entity.setLastErrorMessage(null);
        entity.setUpdatedAt(Instant.now());
        empresaIntegrationRepository.save(entity);

        return toResponse(updated, entity.getStatus());
    }

    /** Lê a configuração atual do webhook (ou um estado vazio quando não configurado). */
    @Transactional(readOnly = true)
    public GenericWebhookConfigResponse getConfig() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        EmpresaIntegrationEntity entity = empresaIntegrationRepository
                .findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .orElse(null);
        if (entity == null) {
            return new GenericWebhookConfigResponse(null, null, List.of(), IntegrationStatus.NAO_CONFIGURADA, null, null);
        }
        WebhookSettings settings = readSettings(entity);
        return toResponse(settings, entity.getStatus());
    }

    /**
     * Gera um novo segredo HMAC, invalidando o anterior, e o devolve por
     * completo (única vez em que o valor cheio é exposto).
     */
    @Transactional
    public WebhookSecretResponse rotateSecret() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        EmpresaIntegrationEntity entity = requireIntegration(empresaId);
        WebhookSettings current = readSettings(entity);
        String secret = generateSecret();
        WebhookSettings updated = new WebhookSettings(
                current.webhookUrl(),
                current.events(),
                secret,
                current.lastDeliveryAt(),
                current.lastError()
        );
        entity.setSettingsJson(writeSettings(updated));
        entity.setUpdatedAt(Instant.now());
        empresaIntegrationRepository.save(entity);
        return new WebhookSecretResponse(secret, secretPreview(secret));
    }

    // --- Teste ----------------------------------------------------------------

    /** Envia um evento de teste assinado para a URL configurada. */
    public WebhookTestResponse sendTestEvent() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        EmpresaIntegrationEntity entity = requireIntegration(empresaId);
        WebhookSettings settings = readSettings(entity);
        if (settings.webhookUrl() == null || settings.secret() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Configure o webhook antes de enviar um teste.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", RESULT_READY_EVENT);
        payload.put("tenantId", empresaId);
        payload.put("attemptId", "att_teste");
        payload.put("simulationId", "exemplo");
        payload.put("score", 78);
        payload.put("decision", "RECOMMEND_INTERVIEW");
        payload.put("competencies", List.of(Map.of("name", "Resolução de Conflitos", "score", 82)));
        payload.put("resultUrl", resultUrl("att_teste"));
        payload.put("test", true);

        return post(settings, payload);
    }

    /**
     * Indica se o cliente tem um webhook personalizado ativo e inscrito em
     * {@code RESULT_READY}. Usado para decidir se vale enfileirar a entrega
     * mesmo quando não há webhook da Gupy.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveResultWebhook(String empresaId) {
        return empresaIntegrationRepository
                .findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .filter(entity -> entity.getStatus() == IntegrationStatus.CONECTADA)
                .map(this::readSettings)
                .filter(settings -> settings.webhookUrl() != null && settings.events().contains(RESULT_READY_EVENT))
                .isPresent();
    }

    // --- Entrega real (chamada pelo processamento do outbox) ------------------

    /**
     * Entrega, em melhor esforço, o resultado de uma tentativa ao webhook do
     * cliente, se houver integração {@code CUSTOM_API} ativa inscrita em
     * {@code RESULT_READY}. Nunca lança: registra o erro na própria integração
     * para não interromper a entrega à Gupy.
     */
    public void deliverResultReady(String empresaId, CandidateAttemptEntity attempt) {
        try {
            EmpresaIntegrationEntity entity = empresaIntegrationRepository
                    .findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                    .orElse(null);
            if (entity == null || entity.getStatus() != IntegrationStatus.CONECTADA) {
                return;
            }
            WebhookSettings settings = readSettings(entity);
            if (settings.webhookUrl() == null || settings.secret() == null
                    || !settings.events().contains(RESULT_READY_EVENT)) {
                return;
            }

            Map<String, Object> payload = buildResultReadyPayload(empresaId, attempt);
            WebhookTestResponse result = post(settings, payload);
            recordDelivery(empresaId, result);
        } catch (Exception exception) {
            log.warn("Falha ao entregar webhook genérico para empresa {}: {}", empresaId, exception.getMessage());
            recordDelivery(empresaId, new WebhookTestResponse(false, null, limit(exception.getMessage())));
        }
    }

    Map<String, Object> buildResultReadyPayload(String empresaId, CandidateAttemptEntity attempt) {
        List<Map<String, Object>> competencies = new ArrayList<>();
        for (ResultItemEntity item : attempt.getResultItems()) {
            Map<String, Object> competency = new LinkedHashMap<>();
            competency.put("name", item.getName());
            competency.put("score", item.getScore());
            competencies.add(competency);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", RESULT_READY_EVENT);
        payload.put("tenantId", empresaId);
        payload.put("attemptId", attempt.getId());
        payload.put("simulationId", attempt.getSimulationId());
        payload.put("score", attempt.getScore());
        payload.put("decision", attempt.getDecision() == null ? null : attempt.getDecision().name());
        payload.put("competencies", competencies);
        payload.put("resultUrl", resultUrl(attempt.getId()));
        return payload;
    }

    // --- Envio HTTP -----------------------------------------------------------

    private WebhookTestResponse post(WebhookSettings settings, Map<String, Object> payload) {
        URI uri = outboundUrlValidator.validate(settings.webhookUrl());
        String body = serialize(payload);
        String signature = hmacSignatureService.sign(body, settings.secret());

        return restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HmacSignatureService.SIGNATURE_HEADER, signature)
                .body(body)
                .exchange((req, response) -> {
                    int status = response.getStatusCode().value();
                    String snippet = readSnippet(response.getBody());
                    boolean delivered = response.getStatusCode().is2xxSuccessful();
                    return new WebhookTestResponse(delivered, status, snippet);
                });
    }

    private void recordDelivery(String empresaId, WebhookTestResponse result) {
        empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .ifPresent(entity -> {
                    WebhookSettings current = readSettings(entity);
                    WebhookSettings updated = new WebhookSettings(
                            current.webhookUrl(),
                            current.events(),
                            current.secret(),
                            result.delivered() ? Instant.now().toString() : current.lastDeliveryAt(),
                            result.delivered() ? null : limit(result.responseSnippet())
                    );
                    entity.setSettingsJson(writeSettings(updated));
                    entity.setUpdatedAt(Instant.now());
                    empresaIntegrationRepository.save(entity);
                });
    }

    // --- Helpers --------------------------------------------------------------

    private GenericWebhookConfigResponse toResponse(WebhookSettings settings, IntegrationStatus status) {
        Instant lastDeliveryAt = settings.lastDeliveryAt() == null ? null : Instant.parse(settings.lastDeliveryAt());
        return new GenericWebhookConfigResponse(
                settings.webhookUrl(),
                secretPreview(settings.secret()),
                settings.events() == null ? List.of() : settings.events(),
                status,
                lastDeliveryAt,
                settings.lastError()
        );
    }

    private EmpresaIntegrationEntity findOrCreate(String empresaId) {
        return empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .orElseGet(() -> {
                    EmpresaEntity empresa = empresaRepository.findById(empresaId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
                    EmpresaIntegrationEntity created = new EmpresaIntegrationEntity();
                    created.setEmpresa(empresa);
                    created.setProvider(IntegrationProvider.CUSTOM_API);
                    created.setType(IntegrationType.WEBHOOK);
                    created.setStatus(IntegrationStatus.NAO_CONFIGURADA);
                    created.setCreatedAt(Instant.now());
                    created.setUpdatedAt(Instant.now());
                    return created;
                });
    }

    private EmpresaIntegrationEntity requireIntegration(String empresaId) {
        return empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Webhook personalizado ainda não configurado."
                ));
    }

    private WebhookSettings readSettings(EmpresaIntegrationEntity entity) {
        String json = entity.getSettingsJson();
        if (json == null || json.isBlank()) {
            return new WebhookSettings(null, List.of(), null, null, null);
        }
        try {
            WebhookSettings settings = objectMapper.readValue(json, WebhookSettings.class);
            return new WebhookSettings(
                    settings.webhookUrl(),
                    settings.events() == null ? List.of() : settings.events(),
                    settings.secret(),
                    settings.lastDeliveryAt(),
                    settings.lastError()
            );
        } catch (JsonProcessingException exception) {
            return new WebhookSettings(null, List.of(), null, null, null);
        }
    }

    private String writeSettings(WebhookSettings settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível salvar a configuração.");
        }
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível serializar o evento.");
        }
    }

    private String resultUrl(String attemptId) {
        String base = praxisProperties.publicBaseUrl();
        if (base != null && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/results/" + attemptId;
    }

    private static String readSnippet(java.io.InputStream input) {
        if (input == null) {
            return null;
        }
        try {
            byte[] bytes = input.readNBytes(RESPONSE_SNIPPET_LIMIT);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return null;
        }
    }

    private static String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= RESPONSE_SNIPPET_LIMIT ? value : value.substring(0, RESPONSE_SNIPPET_LIMIT);
    }

    static String generateSecret() {
        return "whsec_" + randomToken();
    }

    private static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String secretPreview(String secret) {
        if (secret == null) {
            return null;
        }
        String suffix = secret.length() <= 4 ? secret : secret.substring(secret.length() - 4);
        return "whsec_••••" + suffix;
    }

    /** Estrutura persistida em {@code settingsJson} da integração CUSTOM_API. */
    record WebhookSettings(
            String webhookUrl,
            List<String> events,
            String secret,
            String lastDeliveryAt,
            String lastError
    ) {
    }
}
