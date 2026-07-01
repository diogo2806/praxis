package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.model.CreditLedgerReason;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaCreditBalanceEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaCreditLedgerEntity;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaCreditBalanceRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaCreditLedgerRepository;

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
    private EmpresaCreditBalanceRepository balanceRepository;
    @Mock
    private EmpresaCreditLedgerRepository ledgerRepository;
    @Mock
    private EmpresaRepository empresaRepository;

    private CreditService service;

    @BeforeEach
    void setUp() {
        service = new CreditService(balanceRepository, ledgerRepository, empresaRepository);
        lenient().when(ledgerRepository.save(any(EmpresaCreditLedgerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(balanceRepository.save(any(EmpresaCreditBalanceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private EmpresaEntity empresa(CommercialPlanType plan, EmpresaStatus status) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("t1");
        empresa.setCommercialPlanType(plan);
        empresa.setStatus(status);
        return empresa;
    }

    private EmpresaCreditBalanceEntity balance(int value) {
        EmpresaCreditBalanceEntity entity = new EmpresaCreditBalanceEntity();
        entity.setEmpresaId("t1");
        entity.setBalance(value);
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    @Test
    void addCreditsUpdatesBalanceAndWritesLedgerAndReactivates() {
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(0)));
        EmpresaEntity empresa = empresa(CommercialPlanType.AVULSO, EmpresaStatus.SEM_CREDITO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));

        int newBalance = service.addCredits("t1", 100, 7L, "compra");

        assertThat(newBalance).isEqualTo(100);
        ArgumentCaptor<EmpresaCreditLedgerEntity> ledger = ArgumentCaptor.forClass(EmpresaCreditLedgerEntity.class);
        verify(ledgerRepository).save(ledger.capture());
        assertThat(ledger.getValue().getDelta()).isEqualTo(100);
        assertThat(ledger.getValue().getReason()).isEqualTo(CreditLedgerReason.PURCHASE);
        assertThat(ledger.getValue().getBalanceAfter()).isEqualTo(100);
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.ATIVO);
    }

    @Test
    void consumeDecrementsAndMarksOutOfCreditWhenZero() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO, EmpresaStatus.ATIVO)));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(false);
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(1)));

        service.consumeOnCompletion("t1", "att1");

        ArgumentCaptor<EmpresaCreditLedgerEntity> ledger = ArgumentCaptor.forClass(EmpresaCreditLedgerEntity.class);
        verify(ledgerRepository).save(ledger.capture());
        assertThat(ledger.getValue().getDelta()).isEqualTo(-1);
        assertThat(ledger.getValue().getAttemptId()).isEqualTo("att1");
        assertThat(ledger.getValue().getBalanceAfter()).isZero();
    }

    @Test
    void consumeIsIdempotentPerAttempt() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO, EmpresaStatus.ATIVO)));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(true);

        service.consumeOnCompletion("t1", "att1");

        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void consumeIsNoOpForNonAvulso() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.PROFISSIONAL, EmpresaStatus.ATIVO)));

        service.consumeOnCompletion("t1", "att1");

        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void assertCanStartBlocksAvulsoWithoutCredit() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO, EmpresaStatus.SEM_CREDITO)));
        when(balanceRepository.findById("t1")).thenReturn(Optional.of(balance(0)));

        assertThatThrownBy(() -> service.assertCanStartNewAttempt("t1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void assertCanStartAllowsAvulsoWithCredit() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO, EmpresaStatus.ATIVO)));
        when(balanceRepository.findById("t1")).thenReturn(Optional.of(balance(5)));

        service.assertCanStartNewAttempt("t1");
    }

    @Test
    void assertCanStartAllowsNonAvulsoAlways() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.PROFISSIONAL, EmpresaStatus.ATIVO)));

        service.assertCanStartNewAttempt("t1");
        verify(balanceRepository, never()).findById(anyString());
    }
}
