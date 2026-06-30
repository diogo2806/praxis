package br.com.iforce.praxis.shared.integration.persistence.repository;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;

import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

import java.util.Optional;


public interface EmpresaIntegrationRepository extends JpaRepository<EmpresaIntegrationEntity, Long> {

    List<EmpresaIntegrationEntity> findByEmpresaIdOrderByProviderAsc(String empresaId);

    Optional<EmpresaIntegrationEntity> findFirstByEmpresaIdAndProvider(
            String empresaId,
            IntegrationProvider provider
    );
}
