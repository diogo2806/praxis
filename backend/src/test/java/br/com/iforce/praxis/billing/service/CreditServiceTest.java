package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.event.CreditConsumedEvent;

import br.com.iforce.praxis.billing.model.CreditLedgerReason;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaCreditBalanceEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaCreditLedgerEntity;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaCreditBalanceRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaCreditLedgerRepository;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.eq;

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
    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreditService service;

    @BeforeEach
    void setUp() {
        service = new CreditService(balanceRepository, ledgerRepository, empresaRepository,
                candidateAttemptRepository, eventPublisher);
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
        EmpresaEntity empresa = empresa(CommercialPlanType.AVULSO, EmpresaStatus.ATIVO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(false);
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(1)));

        service.consumeOnCompletion("t1", "att1");

        ArgumentCaptor<EmpresaCreditLedgerEntity> ledger = ArgumentCaptor.forClass(EmpresaCreditLedgerEntity.class);
        verify(ledgerRepository).save(ledger.capture());
        assertThat(ledger.getValue().getDelta()).isEqualTo(-1);
        assertThat(ledger.getValue().getAttemptId()).isEqualTo("att1");
        assertThat(ledger.getValue().getBalanceAfter()).isZero();
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.SEM_CREDITO);
    }

    @Test
    void consumeMarksOutOfCreditEvenWhenBalanceWasAlreadyZero() {
        EmpresaEntity empresa = empresa(CommercialPlanType.AVULSO, EmpresaStatus.ATIVO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(false);
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(0)));

        service.consumeOnCompletion("t1", "att1");

        // Sem saldo para debitar: não escreve ledger e não deixa saldo negativo, mas ainda marca SEM_CREDITO.
        verify(ledgerRepository, never()).save(any());
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.SEM_CREDITO);
    }

    @Test
    void consumeDoesNotDowngradeSuspensoEmpresa() {
        EmpresaEntity empresa = empresa(CommercialPlanType.AVULSO, EmpresaStatus.SUSPENSO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(false);
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(1)));

        service.consumeOnCompletion("t1", "att1");

        // O débito acontece, mas o status SUSPENSO NÃO deve ser rebaixado para SEM_CREDITO.
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.SUSPENSO);
    }

    @Test
    void consumeIsIdempotentPerAttempt() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO, EmpresaStatus.ATIVO)));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(true);

        service.consumeOnCompletion("t1", "att1");

        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void consumeIsNoOpForEnterprise() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.ENTERPRISE, EmpresaStatus.ATIVO)));

        service.consumeOnCompletion("t1", "att1");

        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void consumeDecrementsProfissionalPool() {
        EmpresaEntity empresa = empresa(CommercialPlanType.PROFISSIONAL, EmpresaStatus.ATIVO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(false);
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(360)));

        service.consumeOnCompletion("t1", "att1");

        ArgumentCaptor<EmpresaCreditLedgerEntity> ledger = ArgumentCaptor.forClass(EmpresaCreditLedgerEntity.class);
        verify(ledgerRepository).save(ledger.capture());
        assertThat(ledger.getValue().getDelta()).isEqualTo(-1);
        assertThat(ledger.getValue().getBalanceAfter()).isEqualTo(359);
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.ATIVO);
    }

    @Test
    void assertCanStartBlocksAvulsoWithoutCredit() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO, EmpresaStatus.SEM_CREDITO)));
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(0)));
        when(candidateAttemptRepository.countByEmpresaIdAndStatusIn(eq("t1"), any())).thenReturn(0L);

        assertThatThrownBy(() -> service.assertCanStartNewAttempt("t1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void assertCanStartAllowsAvulsoWithFreeCredit() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO, EmpresaStatus.ATIVO)));
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(5)));
        // 2 tentativas em andamento reservam 2 dos 5 créditos: ainda há vaga.
        when(candidateAttemptRepository.countByEmpresaIdAndStatusIn(eq("t1"), any())).thenReturn(2L);

        service.assertCanStartNewAttempt("t1");
    }

    @Test
    void assertCanStartBlocksWhenActiveAttemptsAlreadyReserveAllCredit() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO, EmpresaStatus.ATIVO)));
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(2)));
        // 2 tentativas em andamento já reservam os 2 créditos: não cria uma terceira.
        when(candidateAttemptRepository.countByEmpresaIdAndStatusIn(eq("t1"), any())).thenReturn(2L);

        assertThatThrownBy(() -> service.assertCanStartNewAttempt("t1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void assertCanStartAllowsEnterpriseAlways() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.ENTERPRISE, EmpresaStatus.ATIVO)));

        service.assertCanStartNewAttempt("t1");
        verify(balanceRepository, never()).findByEmpresaIdForUpdate(anyString());
    }

    @Test
    void assertCanStartBlocksProfissionalWithExhaustedPool() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.PROFISSIONAL, EmpresaStatus.ATIVO)));
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(0)));
        when(candidateAttemptRepository.countByEmpresaIdAndStatusIn(eq("t1"), any())).thenReturn(0L);

        assertThatThrownBy(() -> service.assertCanStartNewAttempt("t1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void grantAdjustmentCreditsWritesAdjustmentLedgerAndReactivatesSemCredito() {
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(0)));
        EmpresaEntity empresa = empresa(CommercialPlanType.AVULSO, EmpresaStatus.SEM_CREDITO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));

        int newBalance = service.grantAdjustmentCredits("t1", 10, "cortesia");

        assertThat(newBalance).isEqualTo(10);
        ArgumentCaptor<EmpresaCreditLedgerEntity> ledger = ArgumentCaptor.forClass(EmpresaCreditLedgerEntity.class);
        verify(ledgerRepository).save(ledger.capture());
        assertThat(ledger.getValue().getDelta()).isEqualTo(10);
        assertThat(ledger.getValue().getReason()).isEqualTo(CreditLedgerReason.ADJUSTMENT);
        assertThat(ledger.getValue().getBillingEventId()).isNull();
        assertThat(ledger.getValue().getNote()).isEqualTo("cortesia");
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.ATIVO);
    }

    @Test
    void grantAdjustmentCreditsReactivatesPendentePagamento() {
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(0)));
        EmpresaEntity empresa = empresa(CommercialPlanType.AVULSO, EmpresaStatus.PENDENTE_PAGAMENTO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));

        service.grantAdjustmentCredits("t1", 3, "liberação");

        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.ATIVO);
    }

    @Test
    void grantAdjustmentCreditsDoesNotOverrideSuspensoOrCancelado() {
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(0)));
        EmpresaEntity empresa = empresa(CommercialPlanType.AVULSO, EmpresaStatus.SUSPENSO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));

        service.grantAdjustmentCredits("t1", 5, "cortesia");

        // Empresa suspensa administrativamente não volta a ATIVO só porque ganhou crédito.
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.SUSPENSO);
    }

    @Test
    void grantAdjustmentCreditsRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.grantAdjustmentCredits("t1", 0, "erro"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.grantAdjustmentCredits("t1", -1, "erro"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void consumePublishesCreditConsumedEventWithRemainingBalance() {
        EmpresaEntity empresa = empresa(CommercialPlanType.AVULSO, EmpresaStatus.ATIVO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(false);
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(4)));

        service.consumeOnCompletion("t1", "att1");

        ArgumentCaptor<CreditConsumedEvent> event = ArgumentCaptor.forClass(CreditConsumedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().empresaId()).isEqualTo("t1");
        assertThat(event.getValue().balance()).isEqualTo(3);
    }

    @Test
    void consumeDoesNotPublishWhenAlreadyOutOfCredit() {
        EmpresaEntity empresa = empresa(CommercialPlanType.AVULSO, EmpresaStatus.ATIVO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));
        when(ledgerRepository.existsByAttemptIdAndReason("att1", CreditLedgerReason.CONSUMPTION)).thenReturn(false);
        when(balanceRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(balance(0)));

        service.consumeOnCompletion("t1", "att1");

        // Sem débito não há mudança de saldo para anunciar à recarga automática.
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void consumeDoesNotPublishForEnterprise() {
        when(empresaRepository.findById("t1"))
                .thenReturn(Optional.of(empresa(CommercialPlanType.ENTERPRISE, EmpresaStatus.ATIVO)));

        service.consumeOnCompletion("t1", "att1");

        verify(eventPublisher, never()).publishEvent(any());
    }
}
