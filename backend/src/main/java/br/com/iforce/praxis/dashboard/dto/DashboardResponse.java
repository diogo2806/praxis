package br.com.iforce.praxis.dashboard.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStatus;


import java.time.Instant;

import java.util.List;


/**
 * O retrato completo da operação da empresa, do jeito que a tela inicial recebe.
 *
 * <p>Concentra tudo o que o painel exibe em uma só resposta: os indicadores do topo,
 * os últimos resultados, as jornadas, a situação das integrações, o resumo de
 * plano/consumo e as próximas ações recomendadas. É a "fotografia" que o RH vê ao
 * entrar no sistema.</p>
 *
 * @param empresaId identificador da empresa (cliente)
 * @param empresaName nome da empresa exibido na tela
 * @param activeSimulations quantas avaliações já têm versão publicada (prontas para uso)
 * @param assessmentJourneys resumo das jornadas (total, publicadas e em rascunho)
 * @param candidatesInProgress quantos candidatos estão avaliando agora (avaliações e jornadas)
 * @param completedAttemptsLast30Days quantas avaliações foram concluídas nos últimos 30 dias
 * @param latestResults os candidatos avaliados mais recentemente
 * @param journeys as jornadas da empresa, com o andamento de cada uma
 * @param integrations a situação de cada canal de integração
 * @param billing o resumo de plano, saldo e consumo
 * @param recommendedActions as próximas ações sugeridas, já priorizadas
 */
public record DashboardResponse(
        String empresaId,
        String empresaName,
        long activeSimulations,
        AssessmentJourneysSummary assessmentJourneys,
        long candidatesInProgress,
        long completedAttemptsLast30Days,
        List<LatestResult> latestResults,
        List<AssessmentJourneyItem> journeys,
        List<IntegrationStatusItem> integrations,
        BillingUsage billing,
        List<RecommendedAction> recommendedActions
) {

    /**
     * Resumo das jornadas de avaliação da empresa.
     *
     * @param total quantas jornadas existem no total
     * @param published quantas estão publicadas (prontas para receber candidatos)
     * @param draft quantas ainda estão em rascunho
     */
    public record AssessmentJourneysSummary(long total, long published, long draft) {
    }

    /**
     * Um candidato no bloco "últimos resultados" do painel.
     *
     * @param attemptId identificador da avaliação do candidato
     * @param candidateName nome do candidato
     * @param simulationOrJourneyName nome da avaliação ou jornada realizada
     * @param status situação da avaliação (ex.: concluída, em andamento)
     * @param date data de referência (conclusão, ou criação se ainda não concluiu)
     * @param result nota obtida, quando já houver
     * @param actionLabel texto do atalho (ex.: "Ver detalhes" ou "Acompanhar")
     * @param actionRoute para onde o atalho leva na plataforma
     */
    public record LatestResult(
            String attemptId,
            String candidateName,
            String simulationOrJourneyName,
            AttemptStatus status,
            Instant date,
            Integer result,
            String actionLabel,
            String actionRoute
    ) {
    }

    /**
     * Uma jornada no bloco de jornadas do painel.
     *
     * @param id identificador da jornada
     * @param name nome da jornada
     * @param status situação da jornada (rascunho, publicada ou arquivada)
     * @param candidatesInProgress quantos candidatos estão em andamento nesta jornada
     * @param actionLabel texto do atalho (ex.: "Continuar edição" ou "Ver jornada")
     * @param actionRoute para onde o atalho leva na plataforma
     */
    public record AssessmentJourneyItem(
            String id,
            String name,
            AssessmentJourneyStatus status,
            long candidatesInProgress,
            String actionLabel,
            String actionRoute
    ) {
    }

    /**
     * A situação de um canal de integração no painel.
     *
     * @param provider código do canal (ex.: GUPY, RECRUTEI, CUSTOM_API)
     * @param name nome amigável do canal
     * @param status estado atual do canal (ver {@link IntegrationStatus})
     * @param lastSyncAt data da última sincronização, quando houver
     * @param action convite de ação quando falta algo (ex.: "CONFIGURAR"); vazio se já conectado
     */
    public record IntegrationStatusItem(
            String provider,
            String name,
            IntegrationStatus status,
            Instant lastSyncAt,
            String action
    ) {
    }

    /**
     * Estados possíveis de um canal de integração, na leitura do RH.
     *
     * <ul>
     *   <li>{@code CONECTADA} — canal ligado e funcionando.</li>
     *   <li>{@code PENDENTE} — configuração iniciada, aguardando conclusão.</li>
     *   <li>{@code ERRO} — canal com problema que precisa de atenção.</li>
     *   <li>{@code DESATIVADA} — canal desligado propositalmente.</li>
     *   <li>{@code NAO_CONFIGURADA} — canal ainda não configurado.</li>
     * </ul>
     */
    public enum IntegrationStatus {
        CONECTADA,
        PENDENTE,
        ERRO,
        DESATIVADA,
        NAO_CONFIGURADA
    }

    /**
     * Resumo de plano e consumo exibido no painel.
     *
     * @param plan plano contratado (ex.: avulso, profissional, enterprise)
     * @param status situação da empresa (ex.: ativa, suspensa)
     * @param creditBalance saldo de créditos disponível (relevante no plano avulso)
     * @param usedInPeriod quanto foi consumido no período (avaliações concluídas nos últimos 30 dias)
     * @param subscriptionStatus situação da assinatura, quando houver plano recorrente
     * @param nextRenewalAt data da próxima renovação da assinatura, quando houver
     * @param commercialCondition condição comercial acordada, quando houver
     */
    public record BillingUsage(
            CommercialPlanType plan,
            EmpresaStatus status,
            int creditBalance,
            long usedInPeriod,
            SubscriptionStatus subscriptionStatus,
            Instant nextRenewalAt,
            String commercialCondition
    ) {
    }

    /**
     * Uma sugestão de próximo passo exibida no painel.
     *
     * @param type o tipo da ação sugerida (ver {@link RecommendedActionType})
     * @param title título curto da sugestão
     * @param description explicação do porquê da sugestão
     * @param severity urgência da sugestão (ver {@link RecommendedActionSeverity})
     * @param buttonLabel texto do botão de ação
     * @param route para onde o botão leva na plataforma
     */
    public record RecommendedAction(
            RecommendedActionType type,
            String title,
            String description,
            RecommendedActionSeverity severity,
            String buttonLabel,
            String route
    ) {
    }

    /**
     * Tipos de próximas ações que o painel pode sugerir ao RH.
     *
     * <ul>
     *   <li>{@code CREATE_FIRST_SIMULATION} — criar a primeira avaliação.</li>
     *   <li>{@code CREATE_FIRST_JOURNEY} — montar a primeira jornada.</li>
     *   <li>{@code CONFIGURE_INTEGRATION} — configurar um canal de integração pendente.</li>
     *   <li>{@code PUBLISH_DRAFT_JOURNEY} — publicar uma jornada em rascunho.</li>
     *   <li>{@code BUY_CREDITS} — comprar créditos quando o saldo zera.</li>
     *   <li>{@code CHECK_DLQ} — revisar entregas de resultados que caíram em DLQ.</li>
     *   <li>{@code VIEW_RESULTS} — acompanhar os resultados quando está tudo em ordem.</li>
     * </ul>
     */
    public enum RecommendedActionType {
        CREATE_FIRST_SIMULATION,
        CREATE_FIRST_JOURNEY,
        CONFIGURE_INTEGRATION,
        PUBLISH_DRAFT_JOURNEY,
        BUY_CREDITS,
        CHECK_DLQ,
        VIEW_RESULTS
    }

    /**
     * Grau de urgência de uma sugestão, usado para ordenar e destacar visualmente.
     *
     * <p>Da mais urgente para a menos urgente: {@code danger}, {@code warning},
     * {@code info}, {@code success}.</p>
     */
    public enum RecommendedActionSeverity {
        info,
        warning,
        success,
        danger
    }
}
