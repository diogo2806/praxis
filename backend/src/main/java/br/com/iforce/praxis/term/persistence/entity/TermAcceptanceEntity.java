package br.com.iforce.praxis.term.persistence.entity;

import br.com.iforce.praxis.shared.jpa.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Aceite de um termo por um usuário do tenant (REQ-L5). Insert-only: cada aceite é uma linha;
 * o estado atual é o registro mais recente para (tenant, usuário, tipo de termo).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "term_acceptances",
        indexes = @Index(name = "idx_term_acceptances_lookup", columnList = "tenant_id,user_id,term_type")
)
public class TermAcceptanceEntity implements TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Column(name = "user_id", nullable = false, length = 120)
    private String userId;

    @Column(name = "term_type", nullable = false, length = 80)
    private String termType;

    @Column(name = "term_version", nullable = false, length = 40)
    private String termVersion;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;
}
