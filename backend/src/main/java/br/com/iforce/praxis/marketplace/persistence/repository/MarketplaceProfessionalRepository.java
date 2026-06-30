package br.com.iforce.praxis.marketplace.persistence.repository;

import br.com.iforce.praxis.marketplace.model.ProfessionalVerificationStatus;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceProfessionalRepository extends JpaRepository<MarketplaceProfessionalEntity, Long> {

    Optional<MarketplaceProfessionalEntity> findByUserId(Long userId);

    Optional<MarketplaceProfessionalEntity> findByDocument(String document);

    boolean existsByDocument(String document);

    /** Fila de moderação: profissionais por situação de verificação. */
    List<MarketplaceProfessionalEntity> findByVerificationStatusOrderByCreatedAtAsc(
            ProfessionalVerificationStatus verificationStatus);
}
