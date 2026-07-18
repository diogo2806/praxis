package br.com.iforce.praxis.partner.persistence.repository;

import br.com.iforce.praxis.partner.persistence.entity.PartnerClientEntity;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerClientRepository extends JpaRepository<PartnerClientEntity, String> {

    List<PartnerClientEntity> findByEmpresaIdOrderByNameAsc(String empresaId);

    Optional<PartnerClientEntity> findByIdAndEmpresaId(String id, String empresaId);

    boolean existsByEmpresaIdAndProviderAndExternalCompanyId(
            String empresaId,
            IntegrationProvider provider,
            String externalCompanyId
    );
}
