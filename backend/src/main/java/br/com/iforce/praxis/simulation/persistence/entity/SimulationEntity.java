package br.com.iforce.praxis.simulation.persistence.entity;

import br.com.iforce.praxis.shared.jpa.TenantAwareEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "simulations")
public class SimulationEntity implements TenantAwareEntity {

    @Id
    @Column(name = "id", nullable = false, length = 120)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "critical_situation", length = 1200)
    private String criticalSituation;

    @Column(name = "result_use", length = 120)
    private String resultUse;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 160)
    private String deletedBy;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SimulationVersionEntity> versions = new LinkedHashSet<>();
}
