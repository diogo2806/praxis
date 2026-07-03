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

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.util.List;


/**
 * Carteira de créditos do plano pré-pago (AVULSO) — controla quanto o cliente ainda pode gastar.
 *
 * <p>Na visão do processo, pense num cartão pré-pago: o cliente compra créditos e cada avaliação
 * concluída "consome" um deles. Este serviço é quem soma os créditos comprados, debita os
 * consumidos e impede que o cliente comece novas avaliações quando o saldo zera.</p>
 *
 * <p><b>Regra de ouro:</b> o saldo NUNCA muda sem deixar rastro. Toda alteração vem acompanhada de
 * um lançamento no "extrato" (o ledger {@code empresa_credit_ledger}), que só acrescenta linhas e
 * nunca apaga — exatamente como um extrato bancário. As compras somam; cada avaliação concluída
 * debita 1 crédito, de forma idempotente por tentativa (uma mesma avaliação nunca cobra duas
 * vezes). Ao zerar, o cliente fica {@code SEM_CREDITO} — situação diferente de {@code INADIMPLENTE},
 * que é a do assinante que atrasou a mensalidade.</p>
 */
@Service
public class CreditService {

    private final EmpresaCreditBalanceRepository balanceRepository;
    private final EmpresaCreditLedgerRepository ledgerRepository;
    private final EmpresaRepository empresaRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;

    public CreditService(
            EmpresaCreditBalanceRepository balanceRepository,
            EmpresaCreditLedgerRepository ledgerRepository,
            EmpresaRepository empresaRepository,
            CandidateAttemptRepository candidateAttemptRepository
    ) {
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.empresaRepository = empresaRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
    }

    /**
     * Informa quantos créditos o cliente tem disponíveis agora.
     *
     * <p>É a consulta simples de "quanto ainda posso gastar". Um cliente que nunca comprou créditos
     * é tratado como saldo zero. É apenas leitura.</p>
     *
     * @param empresaId identificador do cliente
     * @return o saldo atual de créditos (zero se nunca houve movimentação)
     */
    @Transactional(readOnly = true)
    public int getBalance(String empresaId) {
        return balanceRepository.findById(empresaId)
                .map(EmpresaCreditBalanceEntity::getBalance)
                .orElse(0);
    }

    /**
     * Adiciona créditos ao cliente depois de uma compra confirmada.
     *
     * <p>Fluxo do processo: quando o Mercado Pago confirma o pagamento de um pacote de créditos, os
     * créditos entram na carteira do cliente. O saldo é somado, o extrato ganha uma linha de compra
     * e — se o cliente estava travado por falta de crédito ou aguardando pagamento — o acesso é
     * reativado automaticamente. O saldo é travado durante a operação para que duas confirmações ao
     * mesmo tempo não se atrapalhem.</p>
     *
     * @param empresaId identificador do cliente que receberá os créditos
     * @param amount quantidade de créditos a adicionar (precisa ser positiva)
     * @param billingEventId identificador do evento financeiro que originou a compra (para o extrato)
     * @param note observação livre registrada na linha do extrato
     * @return o novo saldo do cliente após a soma
     * @throws IllegalArgumentException se a quantidade informada não for positiva
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
     * Debita 1 crédito quando uma avaliação é concluída.
     *
     * <p>Fluxo do processo: assim que um candidato termina uma avaliação, o cliente que a aplicou
     * "paga" por ela com 1 crédito. A cobrança vale apenas para clientes do plano pré-pago (AVULSO)
     * e é idempotente por tentativa — se o mesmo término for processado duas vezes, o crédito é
     * debitado uma vez só. Se o cliente já estiver sem saldo, nada é debitado (o saldo nunca fica
     * negativo) e ele é marcado como {@code SEM_CREDITO}. Ao zerar com este débito, também passa a
     * {@code SEM_CREDITO}, o que o impede de iniciar novas avaliações até comprar mais.</p>
     *
     * @param empresaId identificador do cliente dono da avaliação
     * @param attemptId identificador da tentativa concluída (garante que ela seja cobrada uma só vez)
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
     * Verifica, antes de começar, se o cliente pode iniciar mais uma avaliação.
     *
     * <p>Fluxo do processo: é o "porteiro" do consumo. Antes de uma nova avaliação começar, o
     * sistema confere se o cliente pré-pago (AVULSO) ainda tem saldo; se estiver zerado, a criação
     * é barrada com um aviso de que faltam créditos. Para clientes de outros planos, não há bloqueio
     * aqui.</p>
     *
     * <p><b>Reserva por tentativa em andamento:</b> cada tentativa ainda não concluída (não
     * iniciada, em andamento ou pausada) reserva 1 crédito. O débito continua ocorrendo só na
     * conclusão ({@link #consumeOnCompletion}), mas não se permite ter mais tentativas em
     * andamento do que o saldo disponível — assim nunca se conclui mais avaliações do que os
     * créditos comprados (antes, sob criação concorrente, conclusões excedentes saíam "de
     * graça"). O saldo é travado durante a checagem, o que serializa criações concorrentes do
     * mesmo cliente: como a criação da tentativa roda na mesma transação, a trava só é liberada
     * após a nova tentativa ser persistida, então a próxima criação já a enxerga na contagem.
     * Tentativas abandonadas/expiradas deixam de reservar e liberam a vaga.</p>
     *
     * @param empresaId identificador do cliente que quer iniciar uma avaliação
     * @throws ResponseStatusException (402 · pagamento requerido) se não houver crédito livre para reservar
     */
    @Transactional
    public void assertCanStartNewAttempt(String empresaId) {
        EmpresaEntity empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null || empresa.getCommercialPlanType() != CommercialPlanType.AVULSO) {
            return;
        }
        int balance = lockOrCreateBalance(empresaId).getBalance();
        long activeReserved = candidateAttemptRepository.countByEmpresaIdAndStatusIn(
                empresaId,
                List.of(AttemptStatus.NOT_STARTED, AttemptStatus.IN_PROGRESS, AttemptStatus.PAUSED));
        if (balance <= activeReserved) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Créditos insuficientes: já há " + activeReserved
                            + " avaliação(ões) em andamento para o saldo disponível (" + balance + ").");
        }
    }

    /**
     * Abre a carteira do cliente para alteração com segurança: trava a linha do saldo (para evitar
     * corridas entre operações simultâneas) e, se o cliente ainda não tinha carteira, cria uma
     * zerada. Uso interno.
     */
    private EmpresaCreditBalanceEntity lockOrCreateBalance(String empresaId) {
        return balanceRepository.findByEmpresaIdForUpdate(empresaId).orElseGet(() -> {
            EmpresaCreditBalanceEntity created = new EmpresaCreditBalanceEntity();
            created.setEmpresaId(empresaId);
            created.setBalance(0);
            created.setUpdatedAt(Instant.now());
            return balanceRepository.save(created);
        });
    }

    /**
     * Escreve uma linha no extrato de créditos: quanto entrou ou saiu, por qual motivo, qual saldo
     * ficou depois e a que compra ou avaliação a linha se refere. É o registro que sustenta a regra
     * de que nenhum crédito muda sem rastro. Uso interno.
     */
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

    /**
     * Reativa o cliente quando ele volta a ter saldo: quem estava travado por falta de crédito ou
     * aguardando pagamento volta a ficar ativo. Não mexe em quem foi suspenso ou cancelado por
     * decisão administrativa. Uso interno.
     */
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

    /**
     * Marca o cliente como "sem crédito" quando o saldo zera, o que passa a impedir novas
     * avaliações. Só afeta quem estava ativo ou em teste — não rebaixa quem já tinha uma situação
     * mais grave. Uso interno.
     */
    private void markOutOfCredit(EmpresaEntity empresa) {
        if (empresa.getStatus() == EmpresaStatus.ATIVO || empresa.getStatus() == EmpresaStatus.EM_TESTE) {
            empresa.setStatus(EmpresaStatus.SEM_CREDITO);
            empresa.setUpdatedAt(Instant.now());
        }
    }
}
