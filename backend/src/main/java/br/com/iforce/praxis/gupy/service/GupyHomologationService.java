package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.config.PraxisProperties;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolida as evidências técnicas que podem ser comprovadas pelo Práxis antes e durante a
 * homologação como provedor externo da Gupy. A aprovação final permanece externa.
 */
@Service
public class GupyHomologationService {

    private static final String GUPY_PROVIDER = "gupy";
    private static final String STATUS_OK = "OK";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_BLOCKER = "BLOCKER";

    private final CurrentEmpresaService currentEmpresaService;
    private final IntegrationTokenRepository integrationTokenRepository;
    private final EmpresaIntegrationRepository empresaIntegrationRepository;
    private final SimulationVersionRepository simulationVersionRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PraxisProperties praxisProperties;

    public GupyHomologationService(
            CurrentEmpresaService currentEmpresaService,
            IntegrationTokenRepository integrationTokenRepository,
            EmpresaIntegrationRepository empresaIntegrationRepository,
            SimulationVersionRepository simulationVersionRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            OutboxEventRepository outboxEventRepository,
            PraxisProperties praxisProperties
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.integrationTokenRepository = integrationTokenRepository;
        this.empresaIntegrationRepository = empresaIntegrationRepository;
        this.simulationVersionRepository = simulationVersionRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.praxisProperties = praxisProperties;
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
        long sentWebhooks = outboxEventRepository.countGupyResultDeliveriesByStatus(empresaId, "SENT");
        long dlqWebhooks = outboxEventRepository.countGupyResultDeliveriesByStatus(empresaId, "DLQ");
        Instant lastAttemptAt = candidateAttemptRepository.findLastGupyAttemptCreatedAt(empresaId).orElse(null);
        Instant lastAuthenticatedRequestAt = integration == null ? null : integration.getLastSyncAt();
        boolean publicHttps = isHttps(praxisProperties.publicBaseUrl());
        boolean authenticatedRequest = integration != null
                && integration.getStatus() == IntegrationStatus.CONECTADA
                && integration.getLastSyncAt() != null;

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
                "GUPY_APPROVAL",
                "Aprovação da homologação",
                STATUS_PENDING,
                "A confirmação final depende da Gupy e do cliente em uma vaga real; o Práxis não marca esta etapa automaticamente.",
                true
        ));

        int readinessPercent = readinessPercent(checks);
        boolean internalBlocked = checks.stream()
                .anyMatch(item -> !item.external() && STATUS_BLOCKER.equals(item.status()));
        String overallStatus;
        if (internalBlocked) {
            overallStatus = "BLOCKED";
        } else if (completedAttempts > 0 && sentWebhooks > 0 && dlqWebhooks == 0) {
            overallStatus = "EVIDENCE_READY";
        } else {
            overallStatus = "READY_FOR_EXTERNAL_VALIDATION";
        }

        return new GupyHomologationResponse(
                overallStatus,
                readinessPercent,
                true,
                normalizeBaseUrl(praxisProperties.publicBaseUrl()),
                Instant.now(),
                new GupyHomologationResponse.Metrics(
                        publishedTests,
                        gupyAttempts,
                        completedAttempts,
                        attemptsWithWebhook,
                        sentWebhooks,
                        dlqWebhooks,
                        lastAttemptAt,
                        lastAuthenticatedRequestAt
                ),
                endpoints(praxisProperties.publicBaseUrl()),
                List.copyOf(checks)
        );
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
                .filter(check -> !"GUPY_APPROVAL".equals(check.code()))
                .toList();
        long completed = measurable.stream().filter(check -> STATUS_OK.equals(check.status())).count();
        return measurable.isEmpty() ? 0 : (int) Math.round((completed * 100.0) / measurable.size());
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
