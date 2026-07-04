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

import br.com.iforce.praxis.shared.integration.IntegrationManagementService;

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
 * Configura e entrega webhooks genéricos do cliente na integração
 * {@code CUSTOM_API}.
 *
 * <p>Na visão do processo, este serviço é o responsável por permitir que uma
 * empresa cliente receba automaticamente os resultados gerados na Práxis em um
 * sistema próprio. Ele salva a URL informada pelo cliente, mantém o segredo de
 * assinatura, envia eventos de teste e entrega o evento real quando um resultado
 * fica pronto.</p>
 *
 * <p>O envio é assinado com HMAC para que o cliente confirme a origem da
 * mensagem. Falhas de entrega são registradas na configuração da integração,
 * mas não interrompem outros fluxos de entrega, como a integração com Gupy.</p>
 */
@Slf4j
@Service
public class GenericWebhookDeliveryService {

    /**
     * Evento que informa ao cliente que um resultado já pode ser consumido.
     *
     * <p>Hoje é o evento entregue pelo webhook genérico quando uma tentativa de
     * avaliação termina e o resultado fica disponível.</p>
     */
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
    private final IntegrationManagementService integrationManagementService;

    /**
     * Prepara o serviço com os componentes necessários para configurar, assinar
     * e enviar webhooks para sistemas externos dos clientes.
     *
     * @param empresaIntegrationRepository repositório que guarda a configuração do webhook
     * @param empresaRepository repositório usado para validar a empresa dona da integração
     * @param currentEmpresaService serviço que identifica a empresa do usuário logado
     * @param hmacSignatureService serviço que assina cada mensagem enviada ao cliente
     * @param outboundUrlValidator validador que protege o sistema contra URLs inseguras
     * @param praxisProperties propriedades usadas para montar links públicos de resultado
     * @param objectMapper conversor entre objetos do sistema e JSON de configuração ou payload
     * @param restClientBuilder construtor do cliente HTTP usado no envio do webhook
     * @param integrationManagementService serviço que registra atividade bem-sucedida da integração
     */
    public GenericWebhookDeliveryService(
            EmpresaIntegrationRepository empresaIntegrationRepository,
            EmpresaRepository empresaRepository,
            CurrentEmpresaService currentEmpresaService,
            HmacSignatureService hmacSignatureService,
            GupyOutboundUrlValidator outboundUrlValidator,
            PraxisProperties praxisProperties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            IntegrationManagementService integrationManagementService
    ) {
        this.empresaIntegrationRepository = empresaIntegrationRepository;
        this.empresaRepository = empresaRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.hmacSignatureService = hmacSignatureService;
        this.outboundUrlValidator = outboundUrlValidator;
        this.praxisProperties = praxisProperties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.integrationManagementService = integrationManagementService;
    }

    // --- Configuração ---------------------------------------------------------

    /**
     * Salva ou atualiza a configuração do webhook da empresa logada.
     *
     * <p>Fluxo do processo: o cliente informa a URL que receberá os eventos e,
     * opcionalmente, quais eventos deseja acompanhar. O sistema valida a URL,
     * mantém o segredo já existente ou cria um novo quando for a primeira
     * configuração, marca a integração como conectada e devolve a configuração
     * pronta para a tela.</p>
     *
     * @param request URL e eventos escolhidos pelo cliente na tela de integrações
     * @return configuração atualizada, incluindo a prévia segura do segredo
     */
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

    /**
     * Consulta a configuração atual de webhook da empresa logada.
     *
     * <p>Fluxo do processo: quando a tela de integrações abre, o sistema usa este
     * método para mostrar se o webhook já está configurado, qual URL está ativa,
     * quais eventos serão enviados, quando ocorreu a última entrega e se houve
     * erro recente. Quando não existe configuração, a tela recebe um estado vazio
     * e marcado como não configurado.</p>
     *
     * @return estado atual do webhook do cliente ou resposta vazia quando ainda não configurado
     */
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
     * Troca o segredo usado para assinar os webhooks da empresa logada.
     *
     * <p>Fluxo do processo: se o cliente suspeita que o segredo atual vazou ou
     * quer fazer uma rotação preventiva, o sistema gera um novo segredo, invalida
     * o anterior e devolve o valor completo apenas uma vez. A URL e os eventos já
     * configurados são preservados.</p>
     *
     * @return novo segredo completo e sua prévia mascarada para exibição futura
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

    /**
     * Envia um evento fictício para a URL configurada pelo cliente.
     *
     * <p>Fluxo do processo: após configurar a URL do webhook, o cliente pode
     * disparar um teste para confirmar se o sistema dele recebe a mensagem e
     * valida a assinatura corretamente. O payload usa dados de exemplo e não
     * representa uma avaliação real.</p>
     *
     * @return resultado da tentativa de entrega, com status HTTP e trecho da resposta
     * @throws ResponseStatusException se o webhook ainda não tiver URL e segredo configurados
     */
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
     * Verifica se uma empresa tem webhook de resultado pronto ativo.
     *
     * <p>Fluxo do processo: antes de enfileirar uma entrega, o sistema consulta
     * se o cliente realmente configurou uma URL, se a integração está conectada
     * e se o evento {@code RESULT_READY} está habilitado. Assim, o processamento
     * evita criar tarefas de envio para clientes que não usam o webhook genérico.</p>
     *
     * @param empresaId empresa cujo webhook será verificado
     * @return {@code true} quando há webhook ativo para resultado pronto; caso contrário, {@code false}
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
     * Entrega o resultado real de uma tentativa ao webhook configurado pelo cliente.
     *
     * <p>Fluxo do processo: quando uma avaliação termina e o resultado fica
     * pronto, o processamento de saída chama este método. Se a empresa tiver
     * webhook ativo para {@code RESULT_READY}, o sistema monta o payload, assina
     * a mensagem, envia para a URL do cliente e registra sucesso ou erro na
     * configuração da integração.</p>
     *
     * <p>Este método trabalha em melhor esforço: falhas são registradas, mas não
     * são propagadas, para que um problema no webhook personalizado não bloqueie
     * outras integrações ou o restante do processamento.</p>
     *
     * @param empresaId empresa dona do resultado e da configuração de webhook
     * @param attempt tentativa de avaliação cujo resultado será enviado
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

    /**
     * Monta a mensagem de resultado pronto enviada ao sistema do cliente.
     *
     * <p>Uso interno e de teste: transforma os dados da tentativa, como nota,
     * decisão, competências avaliadas e link público do resultado, em um mapa
     * ordenado que será convertido para JSON e enviado pelo webhook.</p>
     *
     * @param empresaId empresa dona da tentativa
     * @param attempt tentativa concluída que originou o resultado
     * @return payload do evento {@code RESULT_READY}
     */
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

    /**
     * Envia um payload assinado para a URL configurada no webhook.
     *
     * <p>Uso interno: valida novamente a URL, serializa a mensagem para JSON,
     * gera a assinatura HMAC e faz o POST para o sistema do cliente. A resposta é
     * reduzida a sucesso/falha, status HTTP e um pequeno trecho do corpo para
     * facilitar diagnóstico na tela de integrações.</p>
     *
     * @param settings configuração ativa do webhook
     * @param payload mensagem que será enviada ao cliente
     * @return resumo da entrega realizada
     */
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

    /**
     * Registra o resultado da última tentativa de entrega do webhook.
     *
     * <p>Uso interno: quando a entrega funciona, atualiza a data da última
     * entrega e limpa o erro anterior. Quando falha, preserva a última entrega
     * bem-sucedida e grava uma mensagem curta de erro para apoio ao cliente.</p>
     *
     * @param empresaId empresa dona da integração atualizada
     * @param result resumo da tentativa de envio realizada
     */
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
                    if (result.delivered()) {
                        integrationManagementService.recordActivity(empresaId, IntegrationProvider.CUSTOM_API);
                    }
                });
    }

    // --- Helpers --------------------------------------------------------------

    /**
     * Converte a configuração interna do webhook no formato devolvido para a tela.
     *
     * <p>Uso interno: mascara o segredo, normaliza a lista de eventos e converte
     * a data da última entrega para que a interface apresente uma visão segura e
     * compreensível ao usuário.</p>
     *
     * @param settings configuração persistida no cadastro da integração
     * @param status situação atual da integração
     * @return resposta segura para exibição na tela de API e webhooks
     */
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

    /**
     * Localiza a configuração de webhook da empresa ou cria um registro inicial.
     *
     * <p>Uso interno para permitir que a primeira configuração do webhook seja
     * feita sem depender de cadastro prévio de integração. O registro nasce como
     * não configurado e só fica conectado quando a URL e o segredo são salvos.</p>
     *
     * @param empresaId empresa dona da integração
     * @return configuração existente ou nova configuração inicial
     * @throws ResponseStatusException se a empresa não existir
     */
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

    /**
     * Exige que já exista uma configuração de webhook para a empresa.
     *
     * <p>Uso interno em operações que só fazem sentido depois da primeira
     * configuração, como rotacionar segredo ou enviar evento de teste.</p>
     *
     * @param empresaId empresa cuja integração deve existir
     * @return configuração de integração encontrada
     * @throws ResponseStatusException se o cliente ainda não configurou webhook personalizado
     */
    private EmpresaIntegrationEntity requireIntegration(String empresaId) {
        return empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Webhook personalizado ainda não configurado."
                ));
    }

    /**
     * Lê a configuração salva em JSON dentro da integração.
     *
     * <p>Uso interno: converte o conteúdo persistido em uma estrutura simples
     * para o restante do fluxo. Quando a configuração ainda não existe ou está
     * inválida, devolve um estado vazio para evitar quebra da tela e permitir uma
     * nova configuração.</p>
     *
     * @param entity cadastro de integração que contém o JSON de configurações
     * @return configurações normalizadas do webhook
     */
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

    /**
     * Grava a configuração do webhook em formato JSON para persistência.
     *
     * <p>Uso interno: centraliza a conversão antes de salvar URL, eventos,
     * segredo e histórico de entrega no cadastro da integração.</p>
     *
     * @param settings configuração normalizada do webhook
     * @return JSON pronto para gravação
     * @throws ResponseStatusException se a configuração não puder ser convertida para JSON
     */
    private String writeSettings(WebhookSettings settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível salvar a configuração.");
        }
    }

    /**
     * Converte o evento que será enviado ao cliente em JSON.
     *
     * <p>Uso interno: garante que o corpo assinado seja exatamente o mesmo corpo
     * transmitido no POST do webhook.</p>
     *
     * @param payload dados do evento de teste ou de resultado pronto
     * @return corpo JSON usado no envio e na assinatura
     * @throws ResponseStatusException se o evento não puder ser convertido para JSON
     */
    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível serializar o evento.");
        }
    }

    /**
     * Monta o link público do resultado que será enviado no webhook.
     *
     * <p>Uso interno: permite que o sistema do cliente receba não apenas os dados
     * principais do resultado, mas também uma URL para consulta visual do detalhe
     * da tentativa.</p>
     *
     * @param attemptId identificador da tentativa de avaliação
     * @return URL pública do resultado da tentativa
     */
    private String resultUrl(String attemptId) {
        String base = praxisProperties.publicBaseUrl();
        if (base != null && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/results/" + attemptId;
    }

    /**
     * Lê um pequeno trecho da resposta devolvida pelo sistema do cliente.
     *
     * <p>Uso interno: limita a quantidade de texto guardada para diagnóstico,
     * evitando salvar respostas grandes demais no histórico da integração.</p>
     *
     * @param input corpo da resposta HTTP do cliente
     * @return trecho inicial da resposta ou {@code null} quando não houver corpo legível
     */
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

    /**
     * Limita textos de erro ou resposta ao tamanho aceito para diagnóstico.
     *
     * <p>Uso interno para manter o registro da integração objetivo e seguro,
     * preservando apenas o trecho necessário para entender a falha.</p>
     *
     * @param value texto original que pode estar grande demais
     * @return texto original ou versão cortada no limite configurado
     */
    private static String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= RESPONSE_SNIPPET_LIMIT ? value : value.substring(0, RESPONSE_SNIPPET_LIMIT);
    }

    /**
     * Gera um novo segredo de webhook no formato exibido ao cliente.
     *
     * <p>Uso interno e de teste: cria o segredo que será compartilhado com o
     * cliente para validar as assinaturas HMAC dos eventos recebidos.</p>
     *
     * @return segredo completo com prefixo {@code whsec_}
     */
    static String generateSecret() {
        return "whsec_" + randomToken();
    }

    /**
     * Cria a parte aleatória e imprevisível do segredo de webhook.
     *
     * <p>Uso interno para garantir que cada segredo emitido ao cliente seja único
     * e difícil de adivinhar.</p>
     *
     * @return trecho aleatório codificado em Base64 URL-safe
     */
    private static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Monta a versão mascarada do segredo para exibição segura na tela.
     *
     * <p>Uso interno: permite que o cliente reconheça qual segredo está em uso
     * sem revelar o valor completo depois da geração.</p>
     *
     * @param secret segredo completo do webhook
     * @return prévia mascarada ou {@code null} quando ainda não houver segredo
     */
    private static String secretPreview(String secret) {
        if (secret == null) {
            return null;
        }
        String suffix = secret.length() <= 4 ? secret : secret.substring(secret.length() - 4);
        return "whsec_••••" + suffix;
    }

    /**
     * Estrutura interna que representa tudo que o cliente configurou para o
     * webhook genérico.
     *
     * <p>Ela é persistida como JSON dentro da integração {@code CUSTOM_API} e
     * concentra URL, eventos habilitados, segredo de assinatura e o último estado
     * conhecido de entrega.</p>
     *
     * @param webhookUrl URL do sistema do cliente que receberá os eventos
     * @param events eventos que o cliente habilitou para entrega
     * @param secret segredo usado para assinar os webhooks
     * @param lastDeliveryAt data da última entrega bem-sucedida
     * @param lastError último erro conhecido de entrega, quando houver
     */
    record WebhookSettings(
            String webhookUrl,
            List<String> events,
            String secret,
            String lastDeliveryAt,
            String lastError
    ) {
    }
}
