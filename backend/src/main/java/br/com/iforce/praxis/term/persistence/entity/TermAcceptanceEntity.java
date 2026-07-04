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
 * Registro persistido de um aceite de termo por um usuário de uma empresa (REQ-L5).
 *
 * <p>Na visão do processo, cada linha representa uma confirmação feita em um momento
 * específico. O histórico é insert-only: novos aceites geram novas linhas, e o status
 * atual é calculado pelo registro mais recente para a combinação de empresa, usuário e
 * tipo de termo.</p>
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

    /** Identificador técnico do registro de aceite no banco de dados. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** Empresa para a qual o aceite foi realizado. */
    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    /** Usuário que confirmou o aceite do termo. */
    @Column(name = "user_id", nullable = false, length = 120)
    private String userId;

    /** Tipo do termo aceito, por exemplo responsabilidade ou uso em saúde. */
    @Column(name = "term_type", nullable = false, length = 80)
    private String termType;

    /** Versão do termo aceita pelo usuário naquele momento. */
    @Column(name = "term_version", nullable = false, length = 40)
    private String termVersion;

    /** Momento em que a confirmação foi registrada para fins de auditoria. */
    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;
}
