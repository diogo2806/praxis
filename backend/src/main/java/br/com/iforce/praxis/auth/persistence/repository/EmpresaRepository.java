package br.com.iforce.praxis.auth.persistence.repository;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;


/**
 * Ponto de consulta dos clientes/empresas cadastrados na plataforma.
 *
 * <p>Os metodos deste repositorio sustentam processos administrativos,
 * integracoes, paineis de acompanhamento e controles de status comercial. A
 * documentacao descreve a finalidade de negocio de cada consulta para facilitar
 * a leitura por pessoas que acompanham o processo, mesmo sem atuar no codigo.</p>
 */
public interface EmpresaRepository extends JpaRepository<EmpresaEntity, String> {

    /**
     * Encontra uma empresa pelo identificador usado fora da plataforma.
     *
     * <p>Usado em processos de integracao ou sincronizacao, quando a plataforma
     * precisa relacionar uma empresa interna com o cadastro conhecido por outro
     * sistema.</p>
     */
    Optional<EmpresaEntity> findFirstByCompanyId(String companyId);

    /**
     * Encontra a empresa associada a um token de integracao.
     *
     * <p>Apoia a validacao de chamadas externas, permitindo identificar qual
     * cliente esta tentando se comunicar com a plataforma sem expor o token
     * original.</p>
     */
    Optional<EmpresaEntity> findFirstByIntegrationTokenHash(String integrationTokenHash);

    /**
     * Conta quantas empresas estao em um determinado status.
     *
     * <p>Usado em indicadores administrativos e acompanhamentos operacionais,
     * como saber quantos clientes estao ativos, bloqueados, pendentes ou em
     * outra situacao do ciclo comercial.</p>
     */
    long countByStatus(EmpresaStatus status);

    /**
     * Lista clientes reais da plataforma aplicando filtros opcionais de busca,
     * status e plano comercial.
     *
     * <p>Usado na tela administrativa para encontrar empresas por nome, nome
     * fantasia, documento, e-mail corporativo, situacao ou plano. O cliente
     * tecnico da plataforma e removido do resultado para que a listagem mostre
     * apenas clientes acompanhados pelo time.</p>
     *
     * @param search termo de busca ja normalizado com curingas ({@code %termo%}), ou {@code null}
     * @param status status da empresa que deve aparecer no resultado, ou {@code null}
     * @param plan plano comercial que deve aparecer no resultado, ou {@code null}
     */
    @Query("""
            SELECT t FROM EmpresaEntity t
            WHERE t.id <> 'PLATFORM'
              AND (:search IS NULL
                   OR LOWER(t.name) LIKE :search
                   OR LOWER(COALESCE(t.tradeName, '')) LIKE :search
                   OR LOWER(COALESCE(t.taxId, '')) LIKE :search
                   OR LOWER(COALESCE(t.corporateEmail, '')) LIKE :search)
              AND (:status IS NULL OR t.status = :status)
              AND (:plan IS NULL OR t.commercialPlanType = :plan)
            ORDER BY t.createdAt DESC
            """)
    List<EmpresaEntity> search(
            @Param("search") String search,
            @Param("status") EmpresaStatus status,
            @Param("plan") CommercialPlanType plan
    );

    /**
     * Lista os clientes reais criados mais recentemente.
     *
     * <p>Usado para acompanhar novas entradas na plataforma e dar visibilidade
     * rapida aos clientes que acabaram de iniciar o relacionamento.</p>
     */
    @Query("SELECT t FROM EmpresaEntity t WHERE t.id <> 'PLATFORM' ORDER BY t.createdAt DESC")
    List<EmpresaEntity> findRecentClients(Pageable pageable);

    /**
     * Lista clientes reais que estejam em qualquer um dos status informados.
     *
     * <p>Permite que o processo administrativo acompanhe grupos de clientes em
     * situacoes especificas, como pendencias, ativacoes ou bloqueios, sem
     * misturar o cadastro tecnico da propria plataforma.</p>
     */
    @Query("SELECT t FROM EmpresaEntity t WHERE t.id <> 'PLATFORM' AND t.status IN :statuses ORDER BY t.updatedAt DESC")
    List<EmpresaEntity> findByStatuses(@Param("statuses") List<EmpresaStatus> statuses);

    /**
     * Conta o total de clientes reais da plataforma.
     *
     * <p>Usado em paineis e indicadores para mostrar o tamanho da base de
     * clientes, desconsiderando o registro tecnico usado internamente pelo
     * sistema.</p>
     */
    @Query("SELECT COUNT(t) FROM EmpresaEntity t WHERE t.id <> 'PLATFORM'")
    long countClients();

    /**
     * Conta o total de clientes reais em um status especifico.
     *
     * <p>Apoia indicadores de gestao, mostrando quantos clientes estao em cada
     * etapa operacional ou comercial sem contabilizar o registro tecnico da
     * plataforma.</p>
     */
    @Query("SELECT COUNT(t) FROM EmpresaEntity t WHERE t.id <> 'PLATFORM' AND t.status = :status")
    long countClientsByStatus(@Param("status") EmpresaStatus status);
}
