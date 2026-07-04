package br.com.iforce.praxis.term.persistence.entity;

import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;

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
 * Registro histórico de aceite de termo por usuário e empresa.
 *
 * <p>Na visão do processo, cada linha representa uma confirmação feita na tela:
 * quem confirmou, para qual empresa, qual termo, qual versão e em que momento.
 * O histórico é preservado para consulta e comprovação posterior.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "term_acceptances",
        indexes = @Index(name = "idx_term_acceptances_lookup", columnList = "empresa_id,user_id,term_type")
)
public class TermAcceptanceEntity implements EmpresaAwareEntity {

    /** Identificador técnico do registro de aceite. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** Empresa à qual o aceite pertence. */
    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    /** Usuário que confirmou o termo. */
    @Column(name = "user_id", nullable = false, length = 120)
    private String userId;

    /** Tipo de termo confirmado no processo. */
    @Column(name = "term_type", nullable = false, length = 80)
    private String termType;

    /** Versão do termo que estava vigente no momento da confirmação. */
    @Column(name = "term_version", nullable = false, length = 40)
    private String termVersion;

    /** Data e hora em que o aceite foi registrado. */
    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;
}
