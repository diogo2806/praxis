package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.persistence.entity.TenantCreditBalanceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TenantCreditBalanceRepository extends JpaRepository<TenantCreditBalanceEntity, String> {

    /** Trava a linha de saldo para alterações concorrentes seguras (compra/consumo). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM TenantCreditBalanceEntity b WHERE b.tenantId = :tenantId")
    Optional<TenantCreditBalanceEntity> findByTenantIdForUpdate(@Param("tenantId") String tenantId);
}
