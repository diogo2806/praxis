package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.model.CreditLedgerReason;
import br.com.iforce.praxis.billing.persistence.entity.TenantCreditLedgerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantCreditLedgerRepository extends JpaRepository<TenantCreditLedgerEntity, Long> {

    List<TenantCreditLedgerEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /** Garante idempotência do consumo: uma tentativa concluída consome no máximo 1 crédito. */
    boolean existsByAttemptIdAndReason(String attemptId, CreditLedgerReason reason);
}
