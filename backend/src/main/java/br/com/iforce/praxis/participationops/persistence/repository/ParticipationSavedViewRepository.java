package br.com.iforce.praxis.participationops.persistence.repository;

import br.com.iforce.praxis.participationops.persistence.entity.ParticipationSavedViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationSavedViewRepository extends JpaRepository<ParticipationSavedViewEntity, String> {

    List<ParticipationSavedViewEntity> findByEmpresaIdAndOwnerUserIdOrEmpresaIdAndSharedTrueOrderByNameAsc(
            String ownerEmpresaId,
            String ownerUserId,
            String sharedEmpresaId
    );

    Optional<ParticipationSavedViewEntity> findByEmpresaIdAndId(String empresaId, String id);

    boolean existsByEmpresaIdAndOwnerUserIdAndNameIgnoreCase(
            String empresaId,
            String ownerUserId,
            String name
    );
}
