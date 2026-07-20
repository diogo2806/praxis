package br.com.iforce.praxis.integrity.persistence.repository;

import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateIntegrityEventRepository
        extends JpaRepository<CandidateIntegrityEventEntity, Long>, CandidateIntegrityEventAtomicRepository {

    boolean existsBySessionIdAndSequenceNumber(String sessionId, Long sequenceNumber);
}
