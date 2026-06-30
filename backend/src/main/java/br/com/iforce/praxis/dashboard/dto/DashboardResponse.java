package br.com.iforce.praxis.dashboard.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStatus;


import java.time.Instant;

import java.util.List;


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

    public record AssessmentJourneysSummary(long total, long published, long draft) {
    }

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

    public record AssessmentJourneyItem(
            String id,
            String name,
            AssessmentJourneyStatus status,
            long candidatesInProgress,
            String actionLabel,
            String actionRoute
    ) {
    }

    public record IntegrationStatusItem(
            String provider,
            String name,
            IntegrationStatus status,
            Instant lastSyncAt,
            String action
    ) {
    }

    public enum IntegrationStatus {
        CONECTADA,
        PENDENTE,
        ERRO,
        DESATIVADA,
        NAO_CONFIGURADA
    }

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

    public record RecommendedAction(
            RecommendedActionType type,
            String title,
            String description,
            RecommendedActionSeverity severity,
            String buttonLabel,
            String route
    ) {
    }

    public enum RecommendedActionType {
        CREATE_FIRST_SIMULATION,
        CREATE_FIRST_JOURNEY,
        CONFIGURE_INTEGRATION,
        PUBLISH_DRAFT_JOURNEY,
        BUY_CREDITS,
        VIEW_RESULTS
    }

    public enum RecommendedActionSeverity {
        info,
        warning,
        success,
        danger
    }
}
