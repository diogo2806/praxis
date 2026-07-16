package br.com.iforce.praxis.auth.persistence.repository;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.EmpresaStatus;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Ponto de consulta dos clientes/empresas cadastrados na plataforma.
 */
public interface EmpresaRepository extends JpaRepository<EmpresaEntity, String> {

    Optional<EmpresaEntity> findFirstByCompanyId(String companyId);

    Optional<EmpresaEntity> findFirstByIntegrationTokenHash(String integrationTokenHash);

    long countByStatus(EmpresaStatus status);

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
     * Versão paginada da busca administrativa. Mantém os mesmos filtros do contrato legado,
     * mas impede carregar toda a base de clientes em uma única requisição.
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
            """)
    Page<EmpresaEntity> searchPage(
            @Param("search") String search,
            @Param("status") EmpresaStatus status,
            @Param("plan") CommercialPlanType plan,
            Pageable pageable
    );

    @Query("SELECT t FROM EmpresaEntity t WHERE t.id <> 'PLATFORM' ORDER BY t.createdAt DESC")
    List<EmpresaEntity> findRecentClients(Pageable pageable);

    @Query("SELECT t FROM EmpresaEntity t WHERE t.id <> 'PLATFORM' AND t.status IN :statuses ORDER BY t.updatedAt DESC")
    List<EmpresaEntity> findByStatuses(@Param("statuses") List<EmpresaStatus> statuses);

    @Query("SELECT COUNT(t) FROM EmpresaEntity t WHERE t.id <> 'PLATFORM'")
    long countClients();

    @Query("SELECT COUNT(t) FROM EmpresaEntity t WHERE t.id <> 'PLATFORM' AND t.status = :status")
    long countClientsByStatus(@Param("status") EmpresaStatus status);
}
