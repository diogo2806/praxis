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


/**
 * Monta o resumo que alimenta a tela inicial (painel) da empresa.
 *
 * <p>Na visão do processo, este componente é o "raio-x" da operação de
 * recrutamento do cliente. Ele reúne, em uma única resposta, tudo que o RH
 * precisa ver ao abrir o sistema:</p>
 *
 * <ul>
 *   <li><b>Avaliações ativas</b>: quantas provas já estão publicadas e prontas
 *       para receber candidatos.</li>
 *   <li><b>Jornadas de avaliação</b>: processos com várias etapas, com quantas
 *       estão publicadas e quantas ainda em rascunho.</li>
 *   <li><b>Candidatos em andamento</b>: quantas pessoas estão respondendo neste
 *       momento (contando provas avulsas e jornadas).</li>
 *   <li><b>Últimos resultados</b>: as participações mais recentes, com nome do
 *       candidato, prova, situação e pontuação.</li>
 *   <li><b>Integrações</b>: se os parceiros externos (Gupy, Recrutei, API
 *       própria) estão conectados.</li>
 *   <li><b>Uso do plano</b>: plano contratado, saldo de créditos e consumo dos
 *       últimos 30 dias.</li>
 *   <li><b>Ações recomendadas</b>: sugestões do que fazer a seguir (criar a
 *       primeira avaliação, publicar rascunho, comprar créditos, etc.).</li>
 * </ul>
 *
 * <p>Todos os números são sempre calculados apenas para a empresa logada, de
 * modo que uma empresa nunca enxerga dados de outra.</p>
 */
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

    /**
     * Reúne e devolve todos os indicadores da tela inicial da empresa logada.
     *
     * <p>Fluxo do processo: descobre qual é a empresa do usuário, confirma que
     * ela existe e então percorre a operação para montar o retrato do momento —
     * conta quantas avaliações estão publicadas, quantas jornadas existem
     * (publicadas e em rascunho), quantos candidatos estão respondendo agora e
     * quantas avaliações foram concluídas nos últimos 30 dias. Em seguida junta
     * os últimos resultados, as jornadas em destaque, a situação das
     * integrações, o uso do plano de cobrança e as ações recomendadas, e
     * entrega tudo pronto para a tela exibir.</p>
     *
     * @return o resumo completo do painel da empresa logada
     * @throws ResponseStatusException se a empresa logada não for encontrada
     */
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
                List.of(AttemptStatus.NOT_STARTED, AttemptStatus.IN_PROGRESS)
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

    /**
     * Lista a situação de cada integração da empresa logada.
     *
     * <p>Fluxo do processo: identifica a empresa do usuário e devolve o estado
     * de conexão com cada parceiro externo de recrutamento. É a mesma
     * informação que aparece no painel, disponibilizada de forma isolada para a
     * tela de integrações.</p>
     *
     * @return a situação de cada integração da empresa logada
     */
    @Transactional(readOnly = true)
    public List<IntegrationStatusItem> getIntegrationStatuses() {
        return getIntegrationStatuses(currentEmpresaService.requiredEmpresaId());
    }

    /**
     * Calcula, para uma empresa específica, a situação de cada parceiro de
     * integração suportado. Uso interno.
     *
     * <p>Para cada parceiro previsto (Gupy, Recrutei e API própria), cruza o que
     * está configurado no cadastro de integrações com os tokens de acesso
     * existentes e decide se a conexão está conectada, pendente, com erro,
     * desativada ou ainda não configurada — incluindo a data da última
     * sincronização e a ação sugerida quando falta configurar.</p>
     */
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

    /**
     * Seleciona as participações mais recentes da empresa para a lista de
     * "últimos resultados" do painel. Uso interno.
     *
     * <p>Pega as cinco participações criadas mais recentemente e, para cada uma,
     * mostra o nome do candidato, o nome da avaliação, a situação atual, a data,
     * a pontuação e um atalho: "Ver detalhes" quando já foi concluída ou
     * "Acompanhar" quando ainda está em andamento.</p>
     */
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

    /**
     * Monta a lista resumida de jornadas em destaque no painel. Uso interno.
     *
     * <p>Para até cinco jornadas, informa o nome, a situação (rascunho ou
     * publicada), quantos candidatos estão em andamento nela e o atalho
     * apropriado: "Continuar edição" quando ainda é rascunho ou "Ver jornada"
     * quando já está publicada.</p>
     */
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

    /**
     * Gera as "ações recomendadas" — as sugestões de próximo passo do painel.
     * Uso interno.
     *
     * <p>Fluxo do processo: olha o estado da operação e propõe o que fazer a
     * seguir. Se não há avaliações, sugere criar a primeira; se não há jornadas,
     * sugere montar uma; se existem jornadas em rascunho, lembra de publicá-las;
     * se alguma integração está sem configurar, sugere configurá-la; e, no plano
     * avulso sem saldo, alerta para comprar créditos — caso contrário, confirma
     * que a operação está ativa e leva aos resultados. As sugestões são
     * ordenadas por urgência (primeiro as mais críticas) e limitadas a quatro,
     * para não sobrecarregar a tela.</p>
     */
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

    /**
     * Indica se uma avaliação já tem ao menos uma versão publicada, ou seja, se
     * está pronta para receber candidatos. É esse critério que a define como
     * "avaliação ativa". Uso interno.
     */
    private boolean hasPublishedVersion(SimulationEntity simulation) {
        return simulation.getVersions().stream()
                .anyMatch(version -> version.getStatus() == SimulationVersionStatus.PUBLISHED);
    }

    /**
     * Escolhe o nome da empresa a exibir: dá preferência ao nome fantasia
     * (comercial) e, na falta dele, usa a razão social. Uso interno.
     */
    private static String displayName(EmpresaEntity empresa) {
        if (empresa.getTradeName() != null && !empresa.getTradeName().isBlank()) {
            return empresa.getTradeName();
        }
        return empresa.getName();
    }

    /**
     * Traduz o grau de urgência de uma ação recomendada em uma ordem de
     * prioridade, para que as sugestões mais críticas apareçam primeiro
     * (perigo, depois aviso, informação e, por fim, sucesso). Uso interno.
     */
    private static int severityOrder(RecommendedActionSeverity severity) {
        return switch (severity) {
            case danger -> 0;
            case warning -> 1;
            case info -> 2;
            case success -> 3;
        };
    }

    /**
     * Descreve um parceiro de integração suportado pelo painel (por exemplo,
     * Gupy ou Recrutei): o código interno, o nome exibido ao usuário, o
     * identificador usado para localizar o token de acesso e se ele pode ser
     * configurado pela empresa. Uso interno.
     */
    private record ProviderDefinition(String code, String name, String tokenProvider, boolean configurable) {
    }
}
