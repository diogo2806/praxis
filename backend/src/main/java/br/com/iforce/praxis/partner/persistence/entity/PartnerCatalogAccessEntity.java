package br.com.iforce.praxis.partner.persistence.entity;

import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "partner_catalog_access",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_partner_catalog_client_simulation",
                columnNames = {"partner_client_id", "simulation_id"}
        )
)
public class PartnerCatalogAccessEntity implements EmpresaAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "partner_client_id", nullable = false, length = 120)
    private String partnerClientId;

    @Column(name = "simulation_id", nullable = false, length = 120)
    private String simulationId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
