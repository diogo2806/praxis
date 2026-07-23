package br.com.iforce.praxis.participationops.persistence.repository;

import br.com.iforce.praxis.participationops.persistence.entity.ParticipationTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationTagRepository extends JpaRepository<ParticipationTagEntity, String> {

    List<ParticipationTagEntity> findByEmpresaIdOrderByNameAsc(String empresaId);

    Optional<ParticipationTagEntity> findByEmpresaIdAndId(String empresaId, String id);

    boolean existsByEmpresaIdAndNameIgnoreCase(String empresaId, String name);
}
