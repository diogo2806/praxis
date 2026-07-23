package br.com.iforce.praxis.featureflag.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "feature_flags")
public class FeatureFlagEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "flag_key", nullable = false, unique = true, length = 120)
    private String key;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "owner", nullable = false, length = 120)
    private String owner;

    @Column(name = "default_enabled", nullable = false)
    private boolean defaultEnabled;

    @Column(name = "global_override")
    private Boolean globalOverride;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "kill_switch", nullable = false)
    private boolean killSwitch;

    @Column(name = "frontend_exposed", nullable = false)
    private boolean frontendExposed;

    @Column(name = "temporary", nullable = false)
    private boolean temporary;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "removal_plan", length = 2000)
    private String removalPlan;

    @Column(name = "environment_targets", length = 2000)
    private String environmentTargets;

    @Column(name = "company_targets", length = 8000)
    private String companyTargets;

    @Column(name = "plan_targets", length = 2000)
    private String planTargets;

    @Column(name = "user_targets", length = 8000)
    private String userTargets;

    @Column(name = "role_targets", length = 4000)
    private String roleTargets;

    @Column(name = "rollout_percentage", nullable = false)
    private int rolloutPercentage;

    @Column(name = "affects_scoring", nullable = false)
    private boolean affectsScoring;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 120)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
