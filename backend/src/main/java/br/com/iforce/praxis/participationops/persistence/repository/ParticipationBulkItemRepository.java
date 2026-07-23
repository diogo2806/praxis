package br.com.iforce.praxis.participationops.persistence.repository;

import br.com.iforce.praxis.participationops.persistence.entity.ParticipationBulkItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipationBulkItemRepository extends JpaRepository<ParticipationBulkItemEntity, Long> {

    List<ParticipationBulkItemEntity> findByJobIdOrderByIdAsc(String jobId);
}
