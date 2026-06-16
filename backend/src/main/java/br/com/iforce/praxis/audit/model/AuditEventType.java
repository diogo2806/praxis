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
    SIMULATION_VERSION_SUBMITTED_FOR_REVIEW("simulationVersionSubmittedForReview"),
    SIMULATION_VERSION_APPROVED("simulationVersionApproved"),
    SIMULATION_VERSION_REJECTED("simulationVersionRejected"),
    SIMULATION_VERSION_CLONED("simulationVersionCloned"),
    SIMULATION_VERSION_PUBLISHED("simulationVersionPublished");

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
