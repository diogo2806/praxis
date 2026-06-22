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
    SIMULATION_ARCHIVED("simulationArchived");

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
