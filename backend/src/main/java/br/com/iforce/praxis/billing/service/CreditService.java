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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Saldo e ledger de créditos do plano AVULSO.
 *
 * <p>Invariante central: o saldo NUNCA é alterado sem um lançamento correspondente no ledger
 * ({@code tenant_credit_ledger}). Compras somam créditos; cada avaliação concluída consome 1
 * crédito, de forma idempotente por tentativa. Sem crédito, o cliente não inicia nova avaliação
 * e fica em {@code SEM_CREDITO} (distinto de {@code INADIMPLENTE}).</p>
 */
@Service
public class CreditService {

    private final TenantCreditBalanceRepository balanceRepository;
    private final TenantCreditLedgerRepository ledgerRepository;
    private final TenantRepository tenantRepository;

    public CreditService(
            TenantCreditBalanceRepository balanceRepository,
            TenantCreditLedgerRepository ledgerRepository,
            TenantRepository tenantRepository
    ) {
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public int getBalance(String tenantId) {
        return balanceRepository.findById(tenantId)
                .map(TenantCreditBalanceEntity::getBalance)
                .orElse(0);
    }

    /**
     * Adiciona créditos após uma compra confirmada. Reativa o cliente se ele estava sem crédito
     * ou pendente de pagamento. Gera lançamento no ledger.
     */
    @Transactional
    public int addCredits(String tenantId, int amount, Long billingEventId, String note) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Quantidade de créditos deve ser positiva.");
        }
        TenantCreditBalanceEntity balance = lockOrCreateBalance(tenantId);
        balance.setBalance(balance.getBalance() + amount);
        balance.setUpdatedAt(Instant.now());

        appendLedger(tenantId, amount, CreditLedgerReason.PURCHASE, balance.getBalance(), null,
                billingEventId, note);

        reactivateIfRecovered(tenantId, balance.getBalance());
        return balance.getBalance();
    }

    /**
     * Consome 1 crédito quando uma avaliação é concluída, de forma idempotente por tentativa.
     * Aplica-se apenas a clientes AVULSO. Ao zerar, o cliente passa a {@code SEM_CREDITO}.
     */
    @Transactional
    public void consumeOnCompletion(String tenantId, String attemptId) {
        TenantEntity tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || tenant.getCommercialPlanType() != CommercialPlanType.AVULSO) {
            return;
        }
        if (ledgerRepository.existsByAttemptIdAndReason(attemptId, CreditLedgerReason.CONSUMPTION)) {
            return;
        }
        TenantCreditBalanceEntity balance = lockOrCreateBalance(tenantId);
        if (balance.getBalance() <= 0) {
            // Sem saldo para debitar: garante a marcação SEM_CREDITO e não cria saldo negativo.
            markOutOfCredit(tenant);
            return;
        }
        balance.setBalance(balance.getBalance() - 1);
        balance.setUpdatedAt(Instant.now());
        appendLedger(tenantId, -1, CreditLedgerReason.CONSUMPTION, balance.getBalance(), attemptId,
                null, "Consumo por avaliação concluída");

        if (balance.getBalance() <= 0) {
            markOutOfCredit(tenant);
        }
    }

    /** Bloqueia o início de nova avaliação quando um cliente AVULSO está sem crédito. */
    @Transactional(readOnly = true)
    public void assertCanStartNewAttempt(String tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || tenant.getCommercialPlanType() != CommercialPlanType.AVULSO) {
            return;
        }
        if (getBalance(tenantId) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED, "Cliente sem créditos disponíveis para iniciar nova avaliação.");
        }
    }

    private TenantCreditBalanceEntity lockOrCreateBalance(String tenantId) {
        return balanceRepository.findByTenantIdForUpdate(tenantId).orElseGet(() -> {
            TenantCreditBalanceEntity created = new TenantCreditBalanceEntity();
            created.setTenantId(tenantId);
            created.setBalance(0);
            created.setUpdatedAt(Instant.now());
            return balanceRepository.save(created);
        });
    }

    private void appendLedger(String tenantId, int delta, CreditLedgerReason reason, int balanceAfter,
                              String attemptId, Long billingEventId, String note) {
        TenantCreditLedgerEntity entry = new TenantCreditLedgerEntity();
        entry.setTenantId(tenantId);
        entry.setDelta(delta);
        entry.setReason(reason);
        entry.setBalanceAfter(balanceAfter);
        entry.setAttemptId(attemptId);
        entry.setBillingEventId(billingEventId);
        entry.setNote(note);
        entry.setCreatedAt(Instant.now());
        ledgerRepository.save(entry);
    }

    private void reactivateIfRecovered(String tenantId, int balance) {
        if (balance <= 0) {
            return;
        }
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            if (tenant.getStatus() == TenantStatus.SEM_CREDITO
                    || tenant.getStatus() == TenantStatus.PENDENTE_PAGAMENTO) {
                tenant.setStatus(TenantStatus.ATIVO);
                tenant.setUpdatedAt(Instant.now());
            }
        });
    }

    private void markOutOfCredit(TenantEntity tenant) {
        if (tenant.getStatus() == TenantStatus.ATIVO || tenant.getStatus() == TenantStatus.EM_TESTE) {
            tenant.setStatus(TenantStatus.SEM_CREDITO);
            tenant.setUpdatedAt(Instant.now());
        }
    }
}
