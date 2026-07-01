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


public interface EmpresaRepository extends JpaRepository<EmpresaEntity, String> {

    Optional<EmpresaEntity> findFirstByCompanyId(String companyId);

    Optional<EmpresaEntity> findFirstByIntegrationTokenHash(String integrationTokenHash);

    long countByStatus(EmpresaStatus status);

    /**
     * Lista clientes da plataforma (exclui o empresa técnico PLATFORM) aplicando filtros
     * opcionais de busca livre, status e plano comercial. Parâmetros nulos são ignorados.
     *
     * @param search termo de busca já normalizado com curingas ({@code %termo%}), ou {@code null}
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

    /** Clientes criados mais recentemente (exclui o empresa técnico PLATFORM). */
    @Query("SELECT t FROM EmpresaEntity t WHERE t.id <> 'PLATFORM' ORDER BY t.createdAt DESC")
    List<EmpresaEntity> findRecentClients(Pageable pageable);

    /** Clientes em um conjunto de status (exclui o empresa técnico PLATFORM). */
    @Query("SELECT t FROM EmpresaEntity t WHERE t.id <> 'PLATFORM' AND t.status IN :statuses ORDER BY t.updatedAt DESC")
    List<EmpresaEntity> findByStatuses(@Param("statuses") List<EmpresaStatus> statuses);

    /** Total de clientes reais da plataforma (exclui o empresa técnico PLATFORM). */
    @Query("SELECT COUNT(t) FROM EmpresaEntity t WHERE t.id <> 'PLATFORM'")
    long countClients();

    /** Total de clientes reais por status (exclui o empresa técnico PLATFORM). */
    @Query("SELECT COUNT(t) FROM EmpresaEntity t WHERE t.id <> 'PLATFORM' AND t.status = :status")
    long countClientsByStatus(@Param("status") EmpresaStatus status);
}
