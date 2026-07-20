package br.com.iforce.praxis.integrity.persistence.repository;

import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityEventEntity;

public interface CandidateIntegrityEventAtomicRepository {

    boolean insertIfAbsent(CandidateIntegrityEventEntity event);
}
