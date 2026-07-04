package br.com.iforce.praxis.term.persistence.repository;

import br.com.iforce.praxis.term.persistence.entity.TermAcceptanceEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import java.util.Optional;


/**
 * Acesso ao histórico de aceites de termos.
 *
 * <p>Na visão do processo, permite encontrar o último registro feito por uma
 * pessoa dentro de uma empresa para saber se a etapa de confirmação já foi
 * concluída.</p>
 */
@Repository
public interface TermAcceptanceRepository extends JpaRepository<TermAcceptanceEntity, Long> {

    /**
     * Busca o aceite mais recente de um usuário para um tipo de termo.
     *
     * @param empresaId empresa responsável pelo processo
     * @param userId usuário que realizou ou pode realizar o aceite
     * @param termType tipo de termo consultado
     * @return último aceite encontrado, quando existir
     */
    Optional<TermAcceptanceEntity> findFirstByEmpresaIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
            String empresaId,
            String userId,
            String termType
    );
}
