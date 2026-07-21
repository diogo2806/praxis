package br.com.iforce.praxis.shared.privacy.persistence.repository;

import br.com.iforce.praxis.shared.privacy.persistence.entity.CandidateNoticeAcceptanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateNoticeAcceptanceRepository extends JpaRepository<CandidateNoticeAcceptanceEntity, Long> {

    Optional<CandidateNoticeAcceptanceEntity> findByAttemptIdAndNoticeVersionAndTermsVersion(
            String attemptId,
            String noticeVersion,
            String termsVersion
    );
}
