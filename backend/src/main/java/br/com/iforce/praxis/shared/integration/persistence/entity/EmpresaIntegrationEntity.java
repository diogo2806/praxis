package br.com.iforce.praxis.shared.integration.persistence.entity;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;

import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;

import br.com.iforce.praxis.shared.integration.model.IntegrationType;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.EnumType;

import jakarta.persistence.Enumerated;

import jakarta.persistence.FetchType;

import jakarta.persistence.GeneratedValue;

import jakarta.persistence.GenerationType;

import jakarta.persistence.Id;

import jakarta.persistence.JoinColumn;

import jakarta.persistence.ManyToOne;

import jakarta.persistence.Table;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;


import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "empresa_integrations")
public class EmpresaIntegrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, insertable = false, updatable = false)
    private String empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaEntity empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 60)
    private IntegrationProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private IntegrationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private IntegrationStatus status;

    @Column(name = "credentials_hash", length = 120)
    private String credentialsHash;

    @Column(name = "credentials_encrypted")
    private String credentialsEncrypted;

    @Column(name = "token_preview", length = 40)
    private String tokenPreview;

    @Column(name = "settings_json")
    private String settingsJson;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "configured_at")
    private Instant configuredAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "last_error_message", length = 600)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
