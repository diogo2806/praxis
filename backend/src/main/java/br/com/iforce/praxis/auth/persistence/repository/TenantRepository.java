package br.com.iforce.praxis.auth.persistence.repository;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<TenantEntity, String> {

    Optional<TenantEntity> findFirstByCompanyId(String companyId);

    Optional<TenantEntity> findFirstByIntegrationTokenHash(String integrationTokenHash);

    long countByStatus(TenantStatus status);

    /**
     * Lista clientes da plataforma (exclui o tenant técnico PLATFORM) aplicando filtros
     * opcionais de busca livre, status e plano comercial. Parâmetros nulos são ignorados.
     *
     * @param search termo de busca já normalizado com curingas ({@code %termo%}), ou {@code null}
     */
    @Query("""
            SELECT t FROM TenantEntity t
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
    List<TenantEntity> search(
            @Param("search") String search,
            @Param("status") TenantStatus status,
            @Param("plan") CommercialPlanType plan
    );

    /** Clientes criados mais recentemente (exclui o tenant técnico PLATFORM). */
    @Query("SELECT t FROM TenantEntity t WHERE t.id <> 'PLATFORM' ORDER BY t.createdAt DESC")
    List<TenantEntity> findRecentClients(Pageable pageable);

    /** Clientes em um conjunto de status (exclui o tenant técnico PLATFORM). */
    @Query("SELECT t FROM TenantEntity t WHERE t.id <> 'PLATFORM' AND t.status IN :statuses ORDER BY t.updatedAt DESC")
    List<TenantEntity> findByStatuses(@Param("statuses") List<TenantStatus> statuses);

    /** Total de clientes reais da plataforma (exclui o tenant técnico PLATFORM). */
    @Query("SELECT COUNT(t) FROM TenantEntity t WHERE t.id <> 'PLATFORM'")
    long countClients();

    /** Total de clientes reais por status (exclui o tenant técnico PLATFORM). */
    @Query("SELECT COUNT(t) FROM TenantEntity t WHERE t.id <> 'PLATFORM' AND t.status = :status")
    long countClientsByStatus(@Param("status") TenantStatus status);
}
