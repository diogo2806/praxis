package br.com.iforce.praxis.participationops.persistence.repository;

import br.com.iforce.praxis.participationops.persistence.entity.ParticipationBulkJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationBulkJobRepository extends JpaRepository<ParticipationBulkJobEntity, String> {

    Optional<ParticipationBulkJobEntity> findByEmpresaIdAndId(String empresaId, String id);

    Optional<ParticipationBulkJobEntity> findByEmpresaIdAndIdempotencyKey(
            String empresaId,
            String idempotencyKey
    );

    List<ParticipationBulkJobEntity> findTop50ByEmpresaIdOrderByCreatedAtDesc(String empresaId);
}
