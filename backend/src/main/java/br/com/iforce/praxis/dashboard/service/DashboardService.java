package br.com.iforce.praxis.dashboard.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.billing.dto.EmpresaBillingOverviewResponse;

import br.com.iforce.praxis.billing.service.BillingService;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.AssessmentJourneyItem;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.AssessmentJourneysSummary;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.BillingUsage;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.IntegrationStatus;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.IntegrationStatusItem;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.LatestResult;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.RecommendedAction;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.RecommendedActionSeverity;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.RecommendedActionType;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStatus;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyEntity;

import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyAttemptRepository;

import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyRepository;

import br.com.iforce.praxis.shared.integration.IntegrationTokenEntity;

import br.com.iforce.praxis.shared.integration.IntegrationTokenRepository;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;

import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;

import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;

import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;

import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;

import org.springframework.data.domain.PageRequest;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.ArrayList;

import java.util.Comparator;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Locale;

import java.util.Map;

import java.util.Set;

import java.util.function.Function;

import java.util.stream.Collectors;


@Service
public class DashboardService {

    private static final List<ProviderDefinition> PROVIDERS = List.of(
            new ProviderDefinition("GUPY", "Gupy", "gupy", true),
            new ProviderDefinition("RECRUTEI", "Recrutei", "recrutei", true),
            new ProviderDefinition("CUSTOM_API", "API própria", "custom_api", true)
    );

    private final CurrentEmpresaService currentEmpresaService;
    private final EmpresaRepository empresaRepository;
    private final SimulationRepository simulationRepository;
    private final AssessmentJourneyRepository journeyRepository;
    private final AssessmentJourneyAttemptRepository journeyAttemptRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final IntegrationTokenRepository integrationTokenRepository;
    private final EmpresaIntegrationRepository empresaIntegrationRepository;
    private final BillingService billingService;

    public DashboardService(
            CurrentEmpresaService currentEmpresaService,
            EmpresaRepository empresaRepository,
            SimulationRepository simulationRepository,
            AssessmentJourneyRepository journeyRepository,
            AssessmentJourneyAttemptRepository journeyAttemptRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            IntegrationTokenRepository integrationTokenRepository,
            EmpresaIntegrationRepository empresaIntegrationRepository,
            BillingService billingService
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.empresaRepository = empresaRepository;
        this.simulationRepository = simulationRepository;
        this.journeyRepository = journeyRepository;
        this.journeyAttemptRepository = journeyAttemptRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.integrationTokenRepository = integrationTokenRepository;
        this.empresaIntegrationRepository = empresaIntegrationRepository;
        this.billingService = billingService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        EmpresaEntity empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));

        List<SimulationEntity> simulations = simulationRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId);
        List<AssessmentJourneyEntity> journeys = journeyRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId);
        Map<String, String> simulationNames = simulations.stream()
                .collect(Collectors.toMap(SimulationEntity::getId, SimulationEntity::getName, (left, right) -> left));

        long activeSimulations = simulations.stream().filter(this::hasPublishedVersion).count();
        long publishedJourneys = journeys.stream()
                .filter(journey -> journey.getStatus() == AssessmentJourneyStatus.PUBLISHED)
                .count();
        long draftJourneys = journeys.stream()
                .filter(journey -> journey.getStatus() == AssessmentJourneyStatus.DRAFT)
                .count();

        long candidatesInProgress = candidateAttemptRepository.countByEmpresaIdAndStatusIn(
                empresaId,
                List.of(AttemptStatus.NOT_STARTED, AttemptStatus.IN_PROGRESS, AttemptStatus.PAUSED)
        ) + journeyAttemptRepository.countByEmpresaIdAndStatusIn(
                empresaId,
                List.of(AssessmentJourneyAttemptStatus.CREATED, AssessmentJourneyAttemptStatus.IN_PROGRESS)
        );
        long completedAttemptsLast30Days = candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtAfter(
                empresaId,
                AttemptStatus.COMPLETED,
                Instant.now().minus(30, ChronoUnit.DAYS)
        );

        List<IntegrationStatusItem> integrations = getIntegrationStatuses(empresaId);
        EmpresaBillingOverviewResponse billingOverview = billingService.overview(empresaId);
        BillingUsage billing = new BillingUsage(
                empresa.getCommercialPlanType(),
                empresa.getStatus(),
                billingOverview.creditBalance(),
                completedAttemptsLast30Days,
                billingOverview.subscription() == null ? null : billingOverview.subscription().status(),
                billingOverview.subscription() == null ? null : billingOverview.subscription().currentPeriodEnd(),
                empresa.getCommercialCondition()
        );

        return new DashboardResponse(
                empresaId,
                displayName(empresa),
                activeSimulations,
                new AssessmentJourneysSummary(journeys.size(), publishedJourneys, draftJourneys),
                candidatesInProgress,
                completedAttemptsLast30Days,
                latestResults(empresaId, simulationNames),
                journeyItems(empresaId, journeys),
                integrations,
                billing,
                recommendedActions(simulations, journeys, integrations, billing, completedAttemptsLast30Days)
        );
    }

    @Transactional(readOnly = true)
    public List<IntegrationStatusItem> getIntegrationStatuses() {
        return getIntegrationStatuses(currentEmpresaService.requiredEmpresaId());
    }

    private List<IntegrationStatusItem> getIntegrationStatuses(String empresaId) {
        Map<String, IntegrationTokenEntity> tokens = integrationTokenRepository.findByEmpresaIdOrderByProviderAsc(empresaId)
                .stream()
                .collect(Collectors.toMap(
                        token -> token.getProvider().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<IntegrationProvider, EmpresaIntegrationEntity> empresaIntegrations = empresaIntegrationRepository
                .findByEmpresaIdOrderByProviderAsc(empresaId)
                .stream()
                .collect(Collectors.toMap(EmpresaIntegrationEntity::getProvider, Function.identity()));

        return PROVIDERS.stream()
                .map(provider -> {
                    EmpresaIntegrationEntity integration = empresaIntegrations.get(IntegrationProvider.valueOf(provider.code()));
                    IntegrationTokenEntity token = tokens.get(provider.tokenProvider());
                    IntegrationStatus status = integration != null
                            ? IntegrationStatus.valueOf(integration.getStatus().name())
                            : token != null ? IntegrationStatus.CONECTADA : IntegrationStatus.NAO_CONFIGURADA;
                    return new IntegrationStatusItem(
                            provider.code(),
                            provider.name(),
                            status,
                            integration == null ? null : integration.getLastSyncAt(),
                            status == IntegrationStatus.CONECTADA ? null : "CONFIGURAR"
                    );
                })
                .toList();
    }

    private List<LatestResult> latestResults(String empresaId, Map<String, String> simulationNames) {
        return candidateAttemptRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, 5))
                .stream()
                .map(attempt -> new LatestResult(
                        attempt.getId(),
                        attempt.getCandidateName(),
                        simulationNames.getOrDefault(attempt.getSimulationId(), "Avaliação"),
                        attempt.getStatus(),
                        attempt.getFinishedAt() != null ? attempt.getFinishedAt() : attempt.getCreatedAt(),
                        attempt.getScore(),
                        attempt.getStatus() == AttemptStatus.COMPLETED ? "Ver detalhes" : "Acompanhar",
                        attempt.getStatus() == AttemptStatus.COMPLETED
                                ? "/results"
                                : "/candidate-links/new"
                ))
                .toList();
    }

    private List<AssessmentJourneyItem> journeyItems(String empresaId, List<AssessmentJourneyEntity> journeys) {
        return journeys.stream()
                .limit(5)
                .map(journey -> {
                    long inProgress = journeyAttemptRepository.countByEmpresaIdAndJourneyIdAndStatusIn(
                            empresaId,
                            journey.getId(),
                            List.of(AssessmentJourneyAttemptStatus.CREATED, AssessmentJourneyAttemptStatus.IN_PROGRESS)
                    );
                    return new AssessmentJourneyItem(
                            journey.getId(),
                            journey.getName(),
                            journey.getStatus(),
                            inProgress,
                            journey.getStatus() == AssessmentJourneyStatus.DRAFT ? "Continuar edição" : "Ver jornada",
                            "/assessment-journeys"
                    );
                })
                .toList();
    }

    private List<RecommendedAction> recommendedActions(
            List<SimulationEntity> simulations,
            List<AssessmentJourneyEntity> journeys,
            List<IntegrationStatusItem> integrations,
            BillingUsage billing,
            long completedAttemptsLast30Days
    ) {
        List<RecommendedAction> actions = new ArrayList<>();
        if (simulations.isEmpty()) {
            actions.add(new RecommendedAction(
                    RecommendedActionType.CREATE_FIRST_SIMULATION,
                    "Crie sua primeira avaliação",
                    "Você ainda não possui avaliações cadastradas para gerar links de candidatos.",
                    RecommendedActionSeverity.info,
                    "Criar avaliação",
                    "/simulations/new"
            ));
        }
        if (journeys.isEmpty()) {
            actions.add(new RecommendedAction(
                    RecommendedActionType.CREATE_FIRST_JOURNEY,
                    "Monte uma jornada de avaliação",
                    "Agrupe avaliações publicadas para processos com múltiplas etapas.",
                    RecommendedActionSeverity.info,
                    "Criar jornada",
                    "/assessment-journeys/new"
            ));
        }

        long draftJourneys = journeys.stream().filter(j -> j.getStatus() == AssessmentJourneyStatus.DRAFT).count();
        if (draftJourneys > 0) {
            actions.add(new RecommendedAction(
                    RecommendedActionType.PUBLISH_DRAFT_JOURNEY,
                    "Você possui " + draftJourneys + " jornada" + (draftJourneys == 1 ? "" : "s") + " em rascunho",
                    "Publique as jornadas prontas para permitir convites de candidatos.",
                    RecommendedActionSeverity.warning,
                    "Publicar jornada",
                    "/assessment-journeys"
            ));
        }

        integrations.stream()
                .filter(integration -> integration.status() == IntegrationStatus.NAO_CONFIGURADA)
                .findFirst()
                .ifPresent(integration -> actions.add(new RecommendedAction(
                        RecommendedActionType.CONFIGURE_INTEGRATION,
                        "Configure integrações",
                        integration.name() + " ainda não está configurada para este cliente.",
                        RecommendedActionSeverity.warning,
                        "Configurar integração",
                        "/integrations"
                )));

        if (billing.plan() == CommercialPlanType.AVULSO && billing.creditBalance() <= 0) {
            actions.add(new RecommendedAction(
                    RecommendedActionType.BUY_CREDITS,
                    "Saldo de créditos esgotado",
                    "Compre créditos para liberar novas avaliações concluídas no plano avulso.",
                    RecommendedActionSeverity.danger,
                    "Comprar créditos",
                    "/billing"
            ));
        } else {
            actions.add(new RecommendedAction(
                    RecommendedActionType.VIEW_RESULTS,
                    "Sua operação está ativa",
                    completedAttemptsLast30Days + " avaliações concluídas nos últimos 30 dias.",
                    RecommendedActionSeverity.success,
                    "Ver resultados",
                    "/results"
            ));
        }

        return actions.stream()
                .sorted(Comparator.comparingInt(action -> severityOrder(action.severity())))
                .limit(4)
                .toList();
    }

    private boolean hasPublishedVersion(SimulationEntity simulation) {
        return simulation.getVersions().stream()
                .anyMatch(version -> version.getStatus() == SimulationVersionStatus.PUBLISHED);
    }

    private static String displayName(EmpresaEntity empresa) {
        if (empresa.getTradeName() != null && !empresa.getTradeName().isBlank()) {
            return empresa.getTradeName();
        }
        return empresa.getName();
    }

    private static int severityOrder(RecommendedActionSeverity severity) {
        return switch (severity) {
            case danger -> 0;
            case warning -> 1;
            case info -> 2;
            case success -> 3;
        };
    }

    private record ProviderDefinition(String code, String name, String tokenProvider, boolean configurable) {
    }
}
