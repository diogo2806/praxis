package br.com.iforce.praxis.simulation.persistence.repository;

import br.com.iforce.praxis.simulation.persistence.entity.TalentReferenceSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TalentReferenceSnapshotRepository extends JpaRepository<TalentReferenceSnapshotEntity, Long> {

    Optional<TalentReferenceSnapshotEntity> findByEmpresaIdAndAttemptId(String empresaId, String attemptId);
}
