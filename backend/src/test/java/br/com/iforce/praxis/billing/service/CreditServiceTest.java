package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.billing.model.CreditLedgerReason;
import br.com.iforce.praxis.billing.persistence.entity.TenantCreditBalanceEntity;
import br.com.iforce.praxis.billing.persistence.entity.TenantCreditLedgerEntity;
import br.com.iforce.praxis.billing.persistence.repository.TenantCreditBalanceRepository;
import br.com.iforce.praxis.billing.persistence.repository.TenantCreditLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    private TenantCreditBalanceRepository balanceRepository;
    @Mock
    private TenantCreditLedgerRepository ledgerRepository;
    @Mock
    private TenantRepository tenantRepository;

    private CreditService service;

    @BeforeEach
    void setUp() {
        service = new CreditService(balanceRepository, ledgerRepository, tenantRepository);
        lenient().when(ledgerRepository.save(any(TenantCreditLedgerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(balanceRepository.save(any(TenantCreditBalanceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TenantEntity tenant(CommercialPlanType plan, TenantStatus status) {
        TenantEntity tenant = new TenantEntity();
        tenant.setId("t1");
        tenant.setCommercialPlanType(plan);
        tenant.setStatus(status);
        return tenant;
    }

    private TenantCreditBalanceEntity balance(int value) {
        TenantCreditBalanceEntity entity = new TenantCreditBalanceEntity();
        entity.setTenantId("t1");
        entity.setBalance(value);
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    @Test
    void addCreditsUpdatesBalanceAndWritesLedgerAndReactivates() {
        when(balanceRepository.findByTenantIdForUpdate("t1")).thenReturn(Optional.of(balance(0)));
        TenantEntity tenant = tenant(CommercialPlanType.AVULSO, TenantStatus.SEM_CREDITO);
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant));

        int newBalance = service.addCredits("t1", 100, 7L, "compra");

        assertThat(newBalance).isEqualTo(100);
        ArgumentCaptor<TenantCreditLedgerEntity> ledger = ArgumentCaptor.forClass(TenantCreditLedgerEntity.class);
        verify(ledgerRepository).save(ledger.capture());
        assertThat(ledger.getValue().getDelta()).isEqualTo(100);
        assertThat(ledger.getValue().getReason()).isEqualTo(CreditLedgerReason.PURCHASE);
        assertThat(ledger.getValue().getBalanceAfter()).isEqualTo(100);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ATIVO);
    }

    @Test
    void consumeDecrementsAndMarksOutOfCreditWhenZero() {
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant(CommercialPlanType.AVULSO, TenantStatus.ATIVO)));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(false);
        when(balanceRepository.findByTenantIdForUpdate("t1")).thenReturn(Optional.of(balance(1)));

        service.consumeOnCompletion("t1", "att1");

        ArgumentCaptor<TenantCreditLedgerEntity> ledger = ArgumentCaptor.forClass(TenantCreditLedgerEntity.class);
        verify(ledgerRepository).save(ledger.capture());
        assertThat(ledger.getValue().getDelta()).isEqualTo(-1);
        assertThat(ledger.getValue().getAttemptId()).isEqualTo("att1");
        assertThat(ledger.getValue().getBalanceAfter()).isZero();
    }

    @Test
    void consumeIsIdempotentPerAttempt() {
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant(CommercialPlanType.AVULSO, TenantStatus.ATIVO)));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(true);

        service.consumeOnCompletion("t1", "att1");

        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void consumeIsNoOpForNonAvulso() {
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant(CommercialPlanType.PROFISSIONAL, TenantStatus.ATIVO)));

        service.consumeOnCompletion("t1", "att1");

        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void assertCanStartBlocksAvulsoWithoutCredit() {
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant(CommercialPlanType.AVULSO, TenantStatus.SEM_CREDITO)));
        when(balanceRepository.findById("t1")).thenReturn(Optional.of(balance(0)));

        assertThatThrownBy(() -> service.assertCanStartNewAttempt("t1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void assertCanStartAllowsAvulsoWithCredit() {
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant(CommercialPlanType.AVULSO, TenantStatus.ATIVO)));
        when(balanceRepository.findById("t1")).thenReturn(Optional.of(balance(5)));

        service.assertCanStartNewAttempt("t1");
    }

    @Test
    void assertCanStartAllowsNonAvulsoAlways() {
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant(CommercialPlanType.PROFISSIONAL, TenantStatus.ATIVO)));

        service.assertCanStartNewAttempt("t1");
        verify(balanceRepository, never()).findById(anyString());
    }
}
