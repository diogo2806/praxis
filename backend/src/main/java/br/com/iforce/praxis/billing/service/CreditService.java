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

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;


/**
 * Saldo e ledger de créditos do plano AVULSO.
 *
 * <p>Invariante central: o saldo NUNCA é alterado sem um lançamento correspondente no ledger
 * ({@code empresa_credit_ledger}). Compras somam créditos; cada avaliação concluída consome 1
 * crédito, de forma idempotente por tentativa. Sem crédito, o cliente não inicia nova avaliação
 * e fica em {@code SEM_CREDITO} (distinto de {@code INADIMPLENTE}).</p>
 */
@Service
public class CreditService {

    private final EmpresaCreditBalanceRepository balanceRepository;
    private final EmpresaCreditLedgerRepository ledgerRepository;
    private final EmpresaRepository empresaRepository;

    public CreditService(
            EmpresaCreditBalanceRepository balanceRepository,
            EmpresaCreditLedgerRepository ledgerRepository,
            EmpresaRepository empresaRepository
    ) {
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public int getBalance(String empresaId) {
        return balanceRepository.findById(empresaId)
                .map(EmpresaCreditBalanceEntity::getBalance)
                .orElse(0);
    }

    /**
     * Adiciona créditos após uma compra confirmada. Reativa o cliente se ele estava sem crédito
     * ou pendente de pagamento. Gera lançamento no ledger.
     */
    @Transactional
    public int addCredits(String empresaId, int amount, Long billingEventId, String note) {
        return applyCredits(empresaId, amount, CreditLedgerReason.PURCHASE, billingEventId, note);
    }

    /**
     * Concede créditos de cortesia/ajuste manual (motivo {@link CreditLedgerReason#ADJUSTMENT}),
     * usado pela operação ADMIN para liberar empresas para teste. Igual a uma compra do ponto de
     * vista do saldo (soma + lançamento no ledger + reativação se estava sem crédito), mas sem
     * vínculo a evento de cobrança do Mercado Pago. O chamador é responsável por auditar a ação.
     */
    @Transactional
    public int grantAdjustmentCredits(String empresaId, int amount, String note) {
        return applyCredits(empresaId, amount, CreditLedgerReason.ADJUSTMENT, null, note);
    }

    private int applyCredits(String empresaId, int amount, CreditLedgerReason reason,
                             Long billingEventId, String note) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Quantidade de créditos deve ser positiva.");
        }
        EmpresaCreditBalanceEntity balance = lockOrCreateBalance(empresaId);
        balance.setBalance(balance.getBalance() + amount);
        balance.setUpdatedAt(Instant.now());

        appendLedger(empresaId, amount, reason, balance.getBalance(), null, billingEventId, note);

        reactivateIfRecovered(empresaId, balance.getBalance());
        return balance.getBalance();
    }

    /**
     * Consome 1 crédito quando uma avaliação é concluída, de forma idempotente por tentativa.
     * Aplica-se apenas a clientes AVULSO. Ao zerar, o cliente passa a {@code SEM_CREDITO}.
     */
    @Transactional
    public void consumeOnCompletion(String empresaId, String attemptId) {
        EmpresaEntity empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null || empresa.getCommercialPlanType() != CommercialPlanType.AVULSO) {
            return;
        }
        // Trava o saldo ANTES de checar idempotencia. O lock serializa duas conclusoes
        // concorrentes da MESMA tentativa: a segunda so prossegue apos o commit da primeira e,
        // ai sim, enxerga o lancamento CONSUMPTION ja gravado. Checar antes do lock (como era)
        // deixava ambas lerem "nao consumido" e debitarem o credito em dobro.
        EmpresaCreditBalanceEntity balance = lockOrCreateBalance(empresaId);
        if (ledgerRepository.existsByAttemptIdAndReason(attemptId, CreditLedgerReason.CONSUMPTION)) {
            return;
        }
        if (balance.getBalance() <= 0) {
            // Sem saldo para debitar: garante a marcação SEM_CREDITO e não cria saldo negativo.
            markOutOfCredit(empresa);
            return;
        }
        balance.setBalance(balance.getBalance() - 1);
        balance.setUpdatedAt(Instant.now());
        appendLedger(empresaId, -1, CreditLedgerReason.CONSUMPTION, balance.getBalance(), attemptId,
                null, "Consumo por avaliação concluída");

        if (balance.getBalance() <= 0) {
            markOutOfCredit(empresa);
        }
    }

    /**
     * Bloqueia o início de nova avaliação quando um cliente AVULSO está sem crédito.
     *
     * <p><b>Limitação conhecida (corrida):</b> esta verificação apenas LÊ o saldo; o débito só
     * ocorre em {@link #consumeOnCompletion}. Sob criação concorrente de tentativas para o mesmo
     * cliente, várias podem passar aqui com saldo 1 e depois só a primeira conclusão debita — as
     * demais completam "de graça". Eliminar isso exige RESERVAR o crédito de forma atômica no
     * início (débito com lock + liberação em abandono/expiração), o que muda a semântica de
     * cobrança (hoje: cobra na conclusão) e é uma decisão de produto. Não alterar sem testes de
     * integração cobrindo reserva, liberação e reembolso.</p>
     */
    @Transactional(readOnly = true)
    public void assertCanStartNewAttempt(String empresaId) {
        EmpresaEntity empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null || empresa.getCommercialPlanType() != CommercialPlanType.AVULSO) {
            return;
        }
        if (getBalance(empresaId) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED, "Cliente sem créditos disponíveis para iniciar nova avaliação.");
        }
    }

    private EmpresaCreditBalanceEntity lockOrCreateBalance(String empresaId) {
        return balanceRepository.findByEmpresaIdForUpdate(empresaId).orElseGet(() -> {
            EmpresaCreditBalanceEntity created = new EmpresaCreditBalanceEntity();
            created.setEmpresaId(empresaId);
            created.setBalance(0);
            created.setUpdatedAt(Instant.now());
            return balanceRepository.save(created);
        });
    }

    private void appendLedger(String empresaId, int delta, CreditLedgerReason reason, int balanceAfter,
                              String attemptId, Long billingEventId, String note) {
        EmpresaCreditLedgerEntity entry = new EmpresaCreditLedgerEntity();
        entry.setEmpresaId(empresaId);
        entry.setDelta(delta);
        entry.setReason(reason);
        entry.setBalanceAfter(balanceAfter);
        entry.setAttemptId(attemptId);
        entry.setBillingEventId(billingEventId);
        entry.setNote(note);
        entry.setCreatedAt(Instant.now());
        ledgerRepository.save(entry);
    }

    private void reactivateIfRecovered(String empresaId, int balance) {
        if (balance <= 0) {
            return;
        }
        empresaRepository.findById(empresaId).ifPresent(empresa -> {
            if (empresa.getStatus() == EmpresaStatus.SEM_CREDITO
                    || empresa.getStatus() == EmpresaStatus.PENDENTE_PAGAMENTO) {
                empresa.setStatus(EmpresaStatus.ATIVO);
                empresa.setUpdatedAt(Instant.now());
            }
        });
    }

    private void markOutOfCredit(EmpresaEntity empresa) {
        if (empresa.getStatus() == EmpresaStatus.ATIVO || empresa.getStatus() == EmpresaStatus.EM_TESTE) {
            empresa.setStatus(EmpresaStatus.SEM_CREDITO);
            empresa.setUpdatedAt(Instant.now());
        }
    }
}
