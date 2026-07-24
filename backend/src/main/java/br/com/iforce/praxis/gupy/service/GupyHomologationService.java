package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.persistence.repository.AuditEventRepository;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.dto.GupyHomologationEvidenceRequest;
import br.com.iforce.praxis.gupy.dto.GupyHomologationResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.integration.IntegrationTokenRepository;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;
import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consolida as evidências técnicas que podem ser comprovadas pelo Práxis antes e durante a
 * homologação como provedor externo da Gupy. A aprovação final permanece externa.
 */
@Service
public class GupyHomologationService {

    private static final String GUPY_PROVIDER = "gupy";
    private static final String GUPY_PROVIDER_AUDIT = "GUPY";
    private static final String GET_RESULT_EVIDENCE = "GET /test/result/{resultId}";
    private static final String EVIDENCE_SETTINGS_KEY = "homologationEvidence";
    private static final String STATUS_OK = "OK";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_BLOCKER = "BLOCKER";

    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final IntegrationTokenRepository integrationTokenRepository;
    private final EmpresaIntegrationRepository empresaIntegrationRepository;
    private final SimulationVersionRepository simulationVersionRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditEventRepository auditEventRepository;
    private final AuditEventService auditEventService;
    private final PraxisProperties praxisProperties;
    private final ObjectMapper objectMapper;

    public GupyHomologationService(
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            IntegrationTokenRepository integrationTokenRepository,
            EmpresaIntegrationRepository empresaIntegrationRepository,
            SimulationVersionRepository simulationVersionRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            OutboxEventRepository outboxEventRepository,
            AuditEventRepository auditEventRepository,
            AuditEventService auditEventService,
            PraxisProperties praxisProperties,
            ObjectMapper objectMapper
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.integrationTokenRepository = integrationTokenRepository;
        this.empresaIntegrationRepository = empresaIntegrationRepository;
        this.simulationVersionRepository = simulationVersionRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditEventRepository = auditEventRepository;
        this.auditEventService = auditEventService;
        this.praxisProperties = praxisProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public GupyHomologationResponse getStatus() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        boolean tokenConfigured = integrationTokenRepository
                .findFirstByEmpresaIdAndProvider(empresaId, GUPY_PROVIDER)
                .isPresent();
        EmpresaIntegrationEntity integration = empresaIntegrationRepository
                .findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.GUPY)
                .orElse(null);
        long publishedTests = simulationVersionRepository.countBySimulationEmpresaIdAndStatus(
                empresaId,
                SimulationVersionStatus.PUBLISHED
        );
        long gupyAttempts = candidateAttemptRepository.countByEmpresaIdAndCallbackUrlIsNotNull(empresaId);
        long completedAttempts = candidateAttemptRepository
                .countByEmpresaIdAndCallbackUrlIsNotNullAndStatus(empresaId, AttemptStatus.COMPLETED);
        long attemptsWithWebhook = candidateAttemptRepository
                .countByEmpresaIdAndCallbackUrlIsNotNullAndResultWebhookUrlIsNotNull(empresaId);
        long validPercentageResults = candidateAttemptRepository
                .countGupyCompletedAttemptsWithValidPercentage(empresaId, AttemptStatus.COMPLETED);
        long sentWebhooks = outboxEventRepository.countGupyResultDeliveriesByStatus(empresaId, "SENT");
        long dlqWebhooks = outboxEventRepository.countGupyResultDeliveriesByStatus(empresaId, "DLQ");
        long resultQueries = auditEventRepository.countIntegrationEndpointEvidence(
                empresaId,
                GUPY_PROVIDER_AUDIT,
                GET_RESULT_EVIDENCE
        );
        Instant lastAttemptAt = candidateAttemptRepository.findLastGupyAttemptCreatedAt(empresaId).orElse(null);
        Instant lastAuthenticatedRequestAt = integration == null ? null : integration.getLastSyncAt();
        boolean publicHttps = isHttps(praxisProperties.publicBaseUrl());
        boolean authenticatedRequest = integration != null
                && integration.getStatus() == IntegrationStatus.CONECTADA
                && integration.getLastSyncAt() != null;
        GupyHomologationResponse.ExternalEvidence externalEvidence = readEvidence(integration);

        List<GupyHomologationResponse.Check> checks = new ArrayList<>();
        checks.add(check(
                "PUBLIC_HTTPS",
                "URL pública segura",
                publicHttps ? STATUS_OK : STATUS_BLOCKER,
                publicHttps
                        ? "A URL pública do Práxis usa HTTPS."
                        : "Configure praxis.public-base-url com uma URL HTTPS acessível pela Gupy.",
                false
        ));
        checks.add(check(
                "ACCESS_TOKEN",
                "Token de integração",
                tokenConfigured ? STATUS_OK : STATUS_BLOCKER,
                tokenConfigured
                        ? "Existe token Gupy ativo na mesma fonte usada pelos endpoints /test."
                        : "Gere um token Gupy na Central de Integrações.",
                false
        ));
        checks.add(check(
                "PUBLISHED_CATALOG",
                "Catálogo publicável",
                publishedTests > 0 ? STATUS_OK : STATUS_BLOCKER,
                publishedTests > 0
                        ? publishedTests + " avaliação(ões) publicada(s) disponível(is) em GET /test."
                        : "Publique ao menos uma avaliação antes de iniciar a homologação.",
                false
        ));
        checks.add(check(
                "AUTHENTICATED_REQUEST",
                "Primeira requisição autenticada",
                authenticatedRequest ? STATUS_OK : STATUS_PENDING,
                authenticatedRequest
                        ? "O Práxis já recebeu atividade autenticada da integração Gupy."
                        : "Aguardando a Gupy chamar um endpoint /test com o token configurado.",
                true
        ));
        checks.add(check(
                "TEST_URL",
                "Criação da tentativa e test_url",
                gupyAttempts > 0 ? STATUS_OK : STATUS_PENDING,
                gupyAttempts > 0
                        ? gupyAttempts + " tentativa(s) criada(s) por payload com callback_url da Gupy."
                        : "Execute POST /test/candidate em uma vaga real e confirme test_url e test_result_id.",
                true
        ));
        checks.add(check(
                "REAL_COMPLETION",
                "Conclusão em vaga real",
                completedAttempts > 0 ? STATUS_OK : STATUS_PENDING,
                completedAttempts > 0
                        ? completedAttempts + " tentativa(s) Gupy concluída(s)."
                        : "Conclua ao menos uma avaliação iniciada pela Gupy.",
                true
        ));
        checks.add(check(
                "CALLBACK_CONFIRMED",
                "Callback confirmado pela Gupy",
                externalEvidence.callbackConfirmed() ? STATUS_OK : STATUS_PENDING,
                externalEvidence.callbackConfirmed()
                        ? confirmationDetail("Callback confirmado", externalEvidence.callbackConfirmedAt(), externalEvidence.callbackConfirmedBy())
                        : "Após o redirecionamento da pessoa candidata, registre a confirmação de que a Gupy recebeu o GET no callback_url.",
                true
        ));
        checks.add(check(
                "RESULT_WEBHOOK_CONFIGURED",
                "result_webhook_url recebido",
                attemptsWithWebhook > 0 ? STATUS_OK : STATUS_PENDING,
                attemptsWithWebhook > 0
                        ? attemptsWithWebhook + " tentativa(s) recebeu(ram) URL de retorno de resultado."
                        : "A vaga real precisa enviar result_webhook_url no POST /test/candidate.",
                true
        ));
        checks.add(check(
                "RESULT_WEBHOOK_DELIVERED",
                sentWebhooks > 0 ? "Resultado entregue ao webhook" : "Entrega do resultado",
                dlqWebhooks > 0 ? STATUS_BLOCKER : sentWebhooks > 0 ? STATUS_OK : STATUS_PENDING,
                deliveryDetail(sentWebhooks, dlqWebhooks),
                true
        ));
        checks.add(check(
                "RESULT_ENDPOINT_QUERIED",
                "Consulta do resultado pela Gupy",
                resultQueries > 0 ? STATUS_OK : STATUS_PENDING,
                resultQueries > 0
                        ? resultQueries + " consulta(s) autenticada(s) concluída(s) em GET /test/result/{resultId}."
                        : "Aguardando a Gupy consultar GET /test/result/{resultId} após a conclusão.",
                true
        ));
        checks.add(check(
                "PERCENTAGE_RESULT",
                "Resultado percentual válido",
                validPercentageResults > 0 ? STATUS_OK : STATUS_PENDING,
                validPercentageResults > 0
                        ? validPercentageResults + " resultado(s) concluído(s) com percentual entre 0 e 100."
                        : "Conclua uma tentativa Gupy com normalized_score entre 0 e 100.",
                true
        ));
        checks.add(check(
                "RESULT_PAGES_CONFIRMED",
                "Páginas de resultado validadas",
                externalEvidence.resultPagesConfirmed() ? STATUS_OK : STATUS_PENDING,
                externalEvidence.resultPagesConfirmed()
                        ? confirmationDetail("Páginas de empresa e candidato validadas", externalEvidence.resultPagesConfirmedAt(), externalEvidence.resultPagesConfirmedBy())
                        : "Valide na Gupy as páginas separadas de resultado da empresa e da pessoa candidata e registre a evidência.",
                true
        ));
        checks.add(check(
                "GUPY_APPROVAL",
                "Aprovação formal da Gupy",
                externalEvidence.gupyApproved() ? STATUS_OK : STATUS_PENDING,
                externalEvidence.gupyApproved()
                        ? confirmationDetail("Aprovação da Gupy registrada", externalEvidence.gupyApprovedAt(), externalEvidence.gupyApprovedBy())
                        : "Registre a aprovação formal recebida da Gupy somente após a execução em vaga real.",
                true
        ));
        checks.add(check(
                "CLIENT_APPROVAL",
                "Aprovação formal do cliente",
                externalEvidence.clientApproved() ? STATUS_OK : STATUS_PENDING,
                externalEvidence.clientApproved()
                        ? confirmationDetail("Aprovação do cliente registrada", externalEvidence.clientApprovedAt(), externalEvidence.clientApprovedBy())
                        : "Registre a aprovação formal do cliente participante da homologação.",
                true
        ));

        int readinessPercent = readinessPercent(checks);
        boolean hasBlocker = checks.stream().anyMatch(item -> STATUS_BLOCKER.equals(item.status()));
        boolean technicalEvidenceReady = checks.stream()
                .filter(item -> !isApprovalCheck(item.code()))
                .allMatch(item -> STATUS_OK.equals(item.status()));
        boolean approved = externalEvidence.gupyApproved() && externalEvidence.clientApproved();
        String overallStatus;
        if (hasBlocker) {
            overallStatus = "BLOCKED";
        } else if (technicalEvidenceReady && approved) {
            overallStatus = "HOMOLOGATED";
        } else if (technicalEvidenceReady) {
            overallStatus = "EVIDENCE_READY";
        } else {
            overallStatus = "READY_FOR_EXTERNAL_VALIDATION";
        }

        return new GupyHomologationResponse(
                overallStatus,
                readinessPercent,
                !"HOMOLOGATED".equals(overallStatus),
                normalizeBaseUrl(praxisProperties.publicBaseUrl()),
                Instant.now(),
                new GupyHomologationResponse.Metrics(
                        publishedTests,
                        gupyAttempts,
                        completedAttempts,
                        attemptsWithWebhook,
                        sentWebhooks,
                        dlqWebhooks,
                        resultQueries,
                        validPercentageResults,
                        lastAttemptAt,
                        lastAuthenticatedRequestAt
                ),
                externalEvidence,
                endpoints(praxisProperties.publicBaseUrl()),
                List.copyOf(checks)
        );
    }

    @Transactional
    public GupyHomologationResponse updateEvidence(GupyHomologationEvidenceRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        EmpresaIntegrationEntity integration = empresaIntegrationRepository
                .findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.GUPY)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Gere o token Gupy antes de registrar evidências de homologação."
                ));

        Map<String, Object> settings = readSettings(integration.getSettingsJson());
        Map<String, Object> evidence = evidenceMap(settings);
        Instant now = Instant.now();
        updateConfirmation(evidence, "callback", request.callbackConfirmed(), now, actorUserId);
        updateConfirmation(evidence, "resultPages", request.resultPagesConfirmed(), now, actorUserId);
        updateConfirmation(evidence, "gupyApproval", request.gupyApproved(), now, actorUserId);
        updateConfirmation(evidence, "clientApproval", request.clientApproved(), now, actorUserId);
        evidence.put("notes", normalizeNotes(request.notes()));
        evidence.put("updatedAt", now.toString());
        evidence.put("updatedBy", actorUserId);
        settings.put(EVIDENCE_SETTINGS_KEY, evidence);

        integration.setSettingsJson(writeJson(settings));
        integration.setUpdatedAt(now);
        empresaIntegrationRepository.save(integration);
        auditEventService.appendIntegrationEvent(
                empresaId,
                actorUserId,
                GUPY_PROVIDER_AUDIT,
                AuditEventType.INTEGRATION_ACTIVITY_RECORDED,
                "Evidências externas da homologação Gupy atualizadas.",
                writeJson(Map.of(
                        "callbackConfirmed", request.callbackConfirmed(),
                        "resultPagesConfirmed", request.resultPagesConfirmed(),
                        "gupyApproved", request.gupyApproved(),
                        "clientApproved", request.clientApproved(),
                        "updatedAt", now.toString()
                ))
        );
        return getStatus();
    }

    private List<GupyHomologationResponse.Endpoint> endpoints(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        return List.of(
                new GupyHomologationResponse.Endpoint("GET", normalized + "/test", "Listar avaliações publicadas"),
                new GupyHomologationResponse.Endpoint("POST", normalized + "/test/candidate", "Criar ou reutilizar tentativa"),
                new GupyHomologationResponse.Endpoint("GET", normalized + "/test/result/{resultId}", "Consultar resultado estruturado")
        );
    }

    private GupyHomologationResponse.Check check(
            String code,
            String title,
            String status,
            String detail,
            boolean external
    ) {
        return new GupyHomologationResponse.Check(code, title, status, detail, external);
    }

    private int readinessPercent(List<GupyHomologationResponse.Check> checks) {
        List<GupyHomologationResponse.Check> measurable = checks.stream()
                .filter(check -> !isApprovalCheck(check.code()))
                .toList();
        long completed = measurable.stream().filter(check -> STATUS_OK.equals(check.status())).count();
        return measurable.isEmpty() ? 0 : (int) Math.round((completed * 100.0) / measurable.size());
    }

    private boolean isApprovalCheck(String code) {
        return "GUPY_APPROVAL".equals(code) || "CLIENT_APPROVAL".equals(code);
    }

    private String deliveryDetail(long sentWebhooks, long dlqWebhooks) {
        if (dlqWebhooks > 0) {
            return dlqWebhooks + " entrega(s) em DLQ. Corrija a URL ou o contrato e reprocesse no Centro Operacional.";
        }
        if (sentWebhooks > 0) {
            return sentWebhooks + " resultado(s) entregue(s) com sucesso ao webhook informado pela Gupy.";
        }
        return "Aguardando a conclusão de uma tentativa com result_webhook_url real.";
    }

    private String confirmationDetail(String label, Instant confirmedAt, String confirmedBy) {
        String at = confirmedAt == null ? "data não informada" : confirmedAt.toString();
        String by = confirmedBy == null || confirmedBy.isBlank() ? "responsável não informado" : confirmedBy;
        return label + " em " + at + " por " + by + ".";
    }

    private GupyHomologationResponse.ExternalEvidence readEvidence(EmpresaIntegrationEntity integration) {
        Map<String, Object> settings = integration == null
                ? new LinkedHashMap<>()
                : readSettings(integration.getSettingsJson());
        Map<String, Object> evidence = evidenceMap(settings);
        return new GupyHomologationResponse.ExternalEvidence(
                booleanValue(evidence.get("callbackConfirmed")),
                instantValue(evidence.get("callbackConfirmedAt")),
                stringValue(evidence.get("callbackConfirmedBy")),
                booleanValue(evidence.get("resultPagesConfirmed")),
                instantValue(evidence.get("resultPagesConfirmedAt")),
                stringValue(evidence.get("resultPagesConfirmedBy")),
                booleanValue(evidence.get("gupyApprovalConfirmed")),
                instantValue(evidence.get("gupyApprovalConfirmedAt")),
                stringValue(evidence.get("gupyApprovalConfirmedBy")),
                booleanValue(evidence.get("clientApprovalConfirmed")),
                instantValue(evidence.get("clientApprovalConfirmedAt")),
                stringValue(evidence.get("clientApprovalConfirmedBy")),
                stringValue(evidence.get("notes"))
        );
    }

    private void updateConfirmation(
            Map<String, Object> evidence,
            String prefix,
            boolean confirmed,
            Instant now,
            String actorUserId
    ) {
        String confirmedKey = prefix + "Confirmed";
        String confirmedAtKey = prefix + "ConfirmedAt";
        String confirmedByKey = prefix + "ConfirmedBy";
        evidence.put(confirmedKey, confirmed);
        if (confirmed) {
            if (instantValue(evidence.get(confirmedAtKey)) == null) {
                evidence.put(confirmedAtKey, now.toString());
                evidence.put(confirmedByKey, actorUserId);
            }
        } else {
            evidence.remove(confirmedAtKey);
            evidence.remove(confirmedByKey);
        }
    }

    private Map<String, Object> readSettings(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(settingsJson, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Configuração persistida da integração Gupy é inválida.", exception);
        }
    }

    private Map<String, Object> evidenceMap(Map<String, Object> settings) {
        Object rawEvidence = settings.get(EVIDENCE_SETTINGS_KEY);
        Map<String, Object> evidence = new LinkedHashMap<>();
        if (rawEvidence instanceof Map<?, ?> values) {
            values.forEach((key, value) -> {
                if (key instanceof String stringKey) {
                    evidence.put(stringKey, value);
                }
            });
        }
        return evidence;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível persistir as evidências da homologação Gupy.", exception);
        }
    }

    private boolean booleanValue(Object value) {
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private String stringValue(Object value) {
        return value instanceof String stringValue && !stringValue.isBlank() ? stringValue : null;
    }

    private Instant instantValue(Object value) {
        String stringValue = stringValue(value);
        if (stringValue == null) {
            return null;
        }
        try {
            return Instant.parse(stringValue);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }

    private boolean isHttps(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return "https".equalsIgnoreCase(URI.create(value.trim()).getScheme());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }
}
