package br.com.iforce.praxis.partner.persistence.repository;

import br.com.iforce.praxis.partner.persistence.entity.PartnerCatalogAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerCatalogAccessRepository extends JpaRepository<PartnerCatalogAccessEntity, Long> {

    List<PartnerCatalogAccessEntity> findByEmpresaIdAndPartnerClientIdAndActiveTrueOrderByCreatedAtAsc(
            String empresaId,
            String partnerClientId
    );

    Optional<PartnerCatalogAccessEntity> findByEmpresaIdAndPartnerClientIdAndSimulationId(
            String empresaId,
            String partnerClientId,
            String simulationId
    );

    boolean existsByEmpresaIdAndPartnerClientIdAndSimulationIdAndActiveTrue(
            String empresaId,
            String partnerClientId,
            String simulationId
    );

    long countByEmpresaIdAndPartnerClientIdAndActiveTrue(String empresaId, String partnerClientId);
}
