package br.com.iforce.praxis.participationops.persistence.repository;

import br.com.iforce.praxis.participationops.persistence.entity.ParticipationTagAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationTagAssignmentRepository extends JpaRepository<ParticipationTagAssignmentEntity, Long> {

    List<ParticipationTagAssignmentEntity> findByEmpresaIdAndParticipationTypeAndParticipationId(
            String empresaId,
            String participationType,
            String participationId
    );

    List<ParticipationTagAssignmentEntity> findByEmpresaIdAndParticipationTypeAndParticipationIdIn(
            String empresaId,
            String participationType,
            List<String> participationIds
    );

    Optional<ParticipationTagAssignmentEntity> findByEmpresaIdAndParticipationTypeAndParticipationIdAndTagId(
            String empresaId,
            String participationType,
            String participationId,
            String tagId
    );

    void deleteByEmpresaIdAndTagId(String empresaId, String tagId);
}
