package br.com.iforce.praxis.auth.persistence.entity;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.EmpresaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "empresas")
public class EmpresaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 120)
    private String id;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "trade_name", length = 180)
    private String tradeName;

    @Column(name = "legal_name", length = 180)
    private String legalName;

    @Column(name = "tax_id", length = 40)
    private String taxId;

    @Column(name = "corporate_email", length = 180)
    private String corporateEmail;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "website", length = 240)
    private String website;

    @Column(name = "company_id", nullable = false, length = 120)
    private String companyId;

    @Column(name = "integration_token_hash", length = 120)
    private String integrationTokenHash;

    /**
     * Habilita a vertical de saúde (uso educativo). A flag isolada não autoriza
     * publicação: também é necessária aprovação formal registrada e datada.
     */
    @Column(name = "health_vertical", nullable = false)
    private boolean healthVertical;

    @Column(name = "health_compliance_approved_at")
    private Instant healthComplianceApprovedAt;

    @Column(name = "health_compliance_approved_by", length = 120)
    private String healthComplianceApprovedBy;

    @Column(name = "privacy_controller_name", length = 180)
    private String privacyControllerName;

    @Column(name = "privacy_controller_tax_id", length = 40)
    private String privacyControllerTaxId;

    @Column(name = "privacy_service_email", length = 320)
    private String privacyServiceEmail;

    @Column(name = "privacy_service_url", length = 500)
    private String privacyServiceUrl;

    @Column(name = "privacy_dpo_contact", length = 320)
    private String privacyDpoContact;

    @Column(name = "privacy_legal_basis", length = 1000)
    private String privacyLegalBasis;

    @Column(name = "privacy_retention_days")
    private Integer privacyRetentionDays;

    @Column(name = "privacy_notice_version", length = 80)
    private String privacyNoticeVersion;

    @Column(name = "privacy_notice_hash", length = 64)
    private String privacyNoticeHash;

    /** Situação operacional do cliente controlada pelo painel ADMIN. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private EmpresaStatus status = EmpresaStatus.EM_TESTE;

    /** Rótulo comercial do cliente (AVULSO, PROFISSIONAL, ENTERPRISE). */
    @Enumerated(EnumType.STRING)
    @Column(name = "commercial_plan_type", nullable = false, length = 40)
    private CommercialPlanType commercialPlanType = CommercialPlanType.ENTERPRISE;

    /** Condição comercial livre, relevante sobretudo para contratos ENTERPRISE. */
    @Column(name = "commercial_condition", length = 2000)
    private String commercialCondition;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
