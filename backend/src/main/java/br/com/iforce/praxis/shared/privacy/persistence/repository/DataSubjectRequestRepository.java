package br.com.iforce.praxis.shared.privacy.persistence.repository;

import br.com.iforce.praxis.shared.privacy.model.ComplianceRequestStatus;
import br.com.iforce.praxis.shared.privacy.persistence.entity.DataSubjectRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DataSubjectRequestRepository extends JpaRepository<DataSubjectRequestEntity, String> {

    List<DataSubjectRequestEntity> findByEmpresaIdOrderByRequestedAtDesc(String empresaId);

    Optional<DataSubjectRequestEntity> findByIdAndEmpresaId(String id, String empresaId);

    List<DataSubjectRequestEntity> findByAttemptId(String attemptId);

    boolean existsByAttemptIdAndRequestTypeAndStatusIn(
            String attemptId,
            br.com.iforce.praxis.candidate.dto.DataSubjectRequestType requestType,
            Collection<ComplianceRequestStatus> statuses
    );
}
