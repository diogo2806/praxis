package br.com.iforce.praxis.term.persistence.repository;

import br.com.iforce.praxis.term.persistence.entity.TermAcceptanceEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import java.util.Optional;


/**
 * Acesso ao histórico de aceites de termos.
 *
 * <p>Na visão do processo, este repositório permite gravar cada confirmação feita pelo
 * usuário e localizar rapidamente o último aceite de um termo para decidir se a etapa
 * já está concluída ou se a nova versão precisa ser apresentada novamente.</p>
 */
@Repository
public interface TermAcceptanceRepository extends JpaRepository<TermAcceptanceEntity, Long> {

    /**
     * Busca o aceite mais recente de um tipo de termo para uma empresa e um usuário.
     *
     * <p>Como o histórico é preservado, o registro mais novo é a referência usada pelo
     * processo para saber se a versão vigente já foi aceita.</p>
     *
     * @param empresaId empresa dona do processo em andamento
     * @param userId usuário que precisa ter o aceite conferido
     * @param termType tipo do termo consultado
     * @return aceite mais recente encontrado, quando existir
     */
    Optional<TermAcceptanceEntity> findFirstByEmpresaIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
            String empresaId,
            String userId,
            String termType
    );
}
