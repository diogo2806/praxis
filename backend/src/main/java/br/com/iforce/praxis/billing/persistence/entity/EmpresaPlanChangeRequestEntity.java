package br.com.iforce.praxis.billing.persistence.entity;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.billing.model.PlanChangeRequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "empresa_plan_change_requests")
public class EmpresaPlanChangeRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 40)
    private PlanChangeRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_plan", nullable = false, length = 40)
    private CommercialPlanType currentPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_plan", length = 40)
    private CommercialPlanType requestedPlan;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
