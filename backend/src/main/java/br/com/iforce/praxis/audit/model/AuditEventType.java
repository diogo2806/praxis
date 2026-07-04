package br.com.iforce.praxis.audit.model;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


public enum AuditEventType implements DescribedEnum {

    ATTEMPT_CREATED("attemptCreated"),
    ATTEMPT_STARTED("attemptStarted"),
    ATTEMPT_ABANDONED("attemptAbandoned"),
    ATTEMPT_EXPIRED("attemptExpired"),
    ANSWER_SUBMITTED("answerSubmitted"),
    ATTEMPT_COMPLETED("attemptCompleted"),
    ATTEMPT_ANONYMIZED("attemptAnonymized"),
    SIMULATION_VERSION_DRAFT_CREATED("simulationVersionDraftCreated"),
    SIMULATION_VERSION_BLUEPRINT_UPDATED("simulationVersionBlueprintUpdated"),
    SIMULATION_NODE_ADDED("simulationNodeAdded"),
    SIMULATION_NODE_UPDATED("simulationNodeUpdated"),
    SIMULATION_NODE_DELETED("simulationNodeDeleted"),
    SIMULATION_OPTION_ADDED("simulationOptionAdded"),
    SIMULATION_OPTION_UPDATED("simulationOptionUpdated"),
    SIMULATION_OPTION_DELETED("simulationOptionDeleted"),
    SIMULATION_VERSION_SUBMITTED_FOR_REVIEW("simulationVersionSubmittedForReview"),
    SIMULATION_VERSION_APPROVED("simulationVersionApproved"),
    SIMULATION_VERSION_CLONED("simulationVersionCloned"),
    SIMULATION_VERSION_PUBLISHED("simulationVersionPublished"),
    SIMULATION_GUPY_INTEGRATION_ACTIVATED("simulationGupyIntegrationActivated"),
    SIMULATION_ARCHIVED("simulationArchived"),
    HUMAN_DECISION("humanDecision"),
    REVIEW_REQUESTED("reviewRequested"),
    DATA_SUBJECT_REQUEST("dataSubjectRequest"),
    HEALTH_CONSENT_RECORDED("healthConsentRecorded"),
    ADMIN_EMPRESA_CREATED("adminEmpresaCreated"),
    ADMIN_EMPRESA_UPDATED("adminEmpresaUpdated"),
    ADMIN_EMPRESA_SUSPENDED("adminEmpresaSuspended"),
    ADMIN_EMPRESA_REACTIVATED("adminEmpresaReactivated"),
    ADMIN_EMPRESA_CANCELED("adminEmpresaCanceled"),
    ADMIN_EMPRESA_CREDITS_GRANTED("adminEmpresaCreditsGranted"),
    ADMIN_COMMERCIAL_PLAN_CHANGED("adminCommercialPlanChanged"),
    ADMIN_COMMERCIAL_CONDITION_CHANGED("adminCommercialConditionChanged"),
    ADMIN_USER_INVITED("adminUserInvited"),
    ADMIN_USER_INVITE_RESENT("adminUserInviteResent"),
    ADMIN_USER_BLOCKED("adminUserBlocked"),
    ADMIN_USER_UNBLOCKED("adminUserUnblocked"),
    ADMIN_USAGE_VIEWED("adminUsageViewed"),
    PASSWORD_RESET_REQUESTED("passwordResetRequested"),
    PASSWORD_RESET_COMPLETED("passwordResetCompleted"),
    ASSESSMENT_JOURNEY_CREATED("assessmentJourneyCreated"),
    ASSESSMENT_JOURNEY_UPDATED("assessmentJourneyUpdated"),
    ASSESSMENT_JOURNEY_PUBLISHED("assessmentJourneyPublished"),
    ASSESSMENT_JOURNEY_ARCHIVED("assessmentJourneyArchived"),
    ASSESSMENT_JOURNEY_ATTEMPT_CREATED("assessmentJourneyAttemptCreated"),
    ASSESSMENT_JOURNEY_ATTEMPT_STARTED("assessmentJourneyAttemptStarted"),
    ASSESSMENT_JOURNEY_STEP_STARTED("assessmentJourneyStepStarted"),
    ASSESSMENT_JOURNEY_STEP_COMPLETED("assessmentJourneyStepCompleted"),
    ASSESSMENT_JOURNEY_ATTEMPT_COMPLETED("assessmentJourneyAttemptCompleted"),
    ASSESSMENT_JOURNEY_ATTEMPT_ABANDONED("assessmentJourneyAttemptAbandoned"),
    ASSESSMENT_JOURNEY_ATTEMPT_EXPIRED("assessmentJourneyAttemptExpired"),
    INTEGRATION_CONFIGURED("integrationConfigured"),
    INTEGRATION_UPDATED("integrationUpdated"),
    INTEGRATION_DISABLED("integrationDisabled"),
    INTEGRATION_REACTIVATED("integrationReactivated"),
    INTEGRATION_SYNC_STARTED("integrationSyncStarted"),
    INTEGRATION_SYNC_COMPLETED("integrationSyncCompleted"),
    INTEGRATION_SYNC_FAILED("integrationSyncFailed"),
    INTEGRATION_TOKEN_CREATED("integrationTokenCreated"),
    INTEGRATION_TOKEN_ROTATED("integrationTokenRotated"),
    INTEGRATION_TOKEN_REVOKED("integrationTokenRevoked"),
    TEAM_USER_INVITED("teamUserInvited"),
    TEAM_USER_INVITE_RESENT("teamUserInviteResent"),
    TEAM_USER_BLOCKED("teamUserBlocked"),
    TEAM_USER_UNBLOCKED("teamUserUnblocked");

    private final String descricao;

    AuditEventType(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static AuditEventType fromString(String valor) {
        return DescribedEnums.fromValue(AuditEventType.class, valor);
    }
}
