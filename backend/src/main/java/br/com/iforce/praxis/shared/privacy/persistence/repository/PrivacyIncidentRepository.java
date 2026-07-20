package br.com.iforce.praxis.shared.privacy.persistence.repository;

import br.com.iforce.praxis.shared.privacy.persistence.entity.PrivacyIncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrivacyIncidentRepository extends JpaRepository<PrivacyIncidentEntity, String> {

    List<PrivacyIncidentEntity> findByEmpresaIdOrderByDiscoveredAtDesc(String empresaId);

    Optional<PrivacyIncidentEntity> findByIdAndEmpresaId(String id, String empresaId);
}
