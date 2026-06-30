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
     * Habilita a vertical de saúde (uso educativo). Quando verdadeiro, a publicação exige aceite do
     * termo de uso em saúde pelo recrutador e o fluxo do candidato coleta o consentimento do
     * paciente para tratamento de dado sensível (LGPD, arts. 11 e 14).
     */
    @Column(name = "health_vertical", nullable = false)
    private boolean healthVertical;

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
