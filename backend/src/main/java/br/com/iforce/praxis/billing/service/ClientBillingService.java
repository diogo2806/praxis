package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.EmpresaStatus;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.billing.dto.BillingEventResponse;
import br.com.iforce.praxis.billing.dto.CheckoutResponse;
import br.com.iforce.praxis.billing.dto.ClientBillingResponse;
import br.com.iforce.praxis.billing.dto.CreditMovementResponse;
import br.com.iforce.praxis.billing.dto.SubscriptionPlanResponse;
import br.com.iforce.praxis.billing.persistence.entity.EmpresaSubscriptionEntity;
import br.com.iforce.praxis.billing.persistence.repository.EmpresaBillingEventRepository;
import br.com.iforce.praxis.billing.persistence.repository.EmpresaCreditLedgerRepository;
import br.com.iforce.praxis.billing.persistence.repository.EmpresaSubscriptionRepository;
import br.com.iforce.praxis.billing.persistence.repository.SubscriptionPlanRepository;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Visão e ações de cobrança disponíveis para a própria empresa autenticada.
 * A confirmação de pagamento continua sendo feita somente pelo Mercado Pago.
 */
@Service
public class ClientBillingService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaSubscriptionRepository subscriptionRepository;
    private final EmpresaBillingEventRepository eventRepository;
    private final EmpresaCreditLedgerRepository creditLedgerRepository;
    private final CandidateAttemptRepository attemptRepository;
    private final CreditService creditService;
    private final SubscriptionPlanRepository planRepository;
    private final BillingService billingService;
    private final MercadoPagoClient mercadoPagoClient;

    public ClientBillingService(EmpresaRepository empresaRepository,
                                EmpresaSubscriptionRepository subscriptionRepository,
                                EmpresaBillingEventRepository eventRepository,
                                EmpresaCreditLedgerRepository creditLedgerRepository,
                                CandidateAttemptRepository attemptRepository,
                                CreditService creditService,
                                SubscriptionPlanRepository planRepository,
                                BillingService billingService,
                                MercadoPagoClient mercadoPagoClient) {
        this.empresaRepository = empresaRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.creditLedgerRepository = creditLedgerRepository;
        this.attemptRepository = attemptRepository;
        this.creditService = creditService;
        this.planRepository = planRepository;
        this.billingService = billingService;
        this.mercadoPagoClient = mercadoPagoClient;
    }

    @Transactional(readOnly = true)
    public ClientBillingResponse getBilling(String empresaId) {
        EmpresaEntity empresa = requireEmpresa(empresaId);
        Instant now = Instant.now();
        long last7 = attemptRepository.countByEmpresaIdAndStatusAndFinishedAtAfter(
                empresaId, AttemptStatus.COMPLETED, now.minus(7, ChronoUnit.DAYS));
        long last30 = attemptRepository.countByEmpresaIdAndStatusAndFinishedAtAfter(
                empresaId, AttemptStatus.COMPLETED, now.minus(30, ChronoUnit.DAYS));
        long allTime = attemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.COMPLETED);

        EmpresaSubscriptionEntity subscription = subscriptionRepository
                .findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId)
                .orElse(null);
        ClientBillingResponse.SubscriptionInfo subscriptionInfo = subscription == null ? null
                : new ClientBillingResponse.SubscriptionInfo(
                subscription.getStatus(),
                subscription.getInitPoint(),
                subscription.getCurrentPeriodEnd(),
                subscription.getLastPaymentAt(),
                subscription.getGraceUntil());

        List<BillingEventResponse> events = eventRepository
                .findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, 50)).stream()
                .map(event -> new BillingEventResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getMpResourceType(),
                        event.getMpResourceId(),
                        event.getMpStatus(),
                        event.getAmountCents(),
                        event.getCurrency(),
                        event.getCreatedAt()))
                .toList();

        List<CreditMovementResponse> creditMovements = creditLedgerRepository
                .findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, 50)).stream()
                .map(movement -> new CreditMovementResponse(
                        movement.getId(),
                        movement.getDelta(),
                        movement.getReason().name(),
                        movement.getBalanceAfter(),
                        movement.getNote(),
                        movement.getCreatedAt()))
                .toList();

        int creditBalance = creditService.getBalance(empresaId);
        return new ClientBillingResponse(
                empresaId,
                empresa.getCommercialPlanType(),
                empresa.getStatus(),
                financialStatusLabel(empresa.getStatus()),
                creditBalance,
                new ClientBillingResponse.UsageSummary(last7, last30, allTime),
                subscriptionInfo,
                resolveActions(empresa.getCommercialPlanType(), empresa.getStatus(), creditBalance),
                events,
                creditMovements);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAvailablePlans() {
        return planRepository.findByActiveTrueOrderByPriceCentsAsc().stream()
                .map(plan -> new SubscriptionPlanResponse(
                        plan.getId(),
                        plan.getCode(),
                        plan.getName(),
                        plan.getPlanType(),
                        plan.getPriceCents(),
                        plan.getCurrency(),
                        plan.getCreditAmount()))
                .toList();
    }

    /**
     * Permite que uma empresa sem plano escolha um pacote avulso e inicie a contratação sozinha.
     */
    @Transactional
    public CheckoutResponse createCreditCheckout(String empresaId, Long planId) {
        EmpresaEntity empresa = requireEmpresa(empresaId);
        prepareInitialPlan(empresa, CommercialPlanType.AVULSO);
        return billingService.createCreditCheckout(empresaId, planId);
    }

    /**
     * Permite que uma empresa sem plano escolha uma assinatura mensal e inicie a contratação sozinha.
     */
    @Transactional
    public CheckoutResponse createSubscriptionCheckout(String empresaId, Long planId) {
        EmpresaEntity empresa = requireEmpresa(empresaId);
        prepareInitialPlan(empresa, CommercialPlanType.PROFISSIONAL);
        return billingService.createSubscription(empresaId, planId);
    }

    @Transactional
    public ClientBillingResponse syncCurrentSubscription(String empresaId) {
        EmpresaSubscriptionEntity subscription = subscriptionRepository
                .findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhuma assinatura encontrada para sincronizar."));
        if (subscription.getMpPreapprovalId() == null || subscription.getMpPreapprovalId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assinatura ainda não possui identificador do Mercado Pago para sincronizar.");
        }
        billingService.manualSync("preapproval", subscription.getMpPreapprovalId(), null);
        return getBilling(empresaId);
    }

    /**
     * Cancela a cobrança recorrente no Mercado Pago e sincroniza o resultado antes de devolvê-lo ao cliente.
     */
    @Transactional
    public ClientBillingResponse cancelCurrentSubscription(String empresaId) {
        EmpresaSubscriptionEntity subscription = subscriptionRepository
                .findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhuma assinatura encontrada para cancelar."));
        if (subscription.getStatus().name().equals("CANCELLED")) {
            return getBilling(empresaId);
        }
        if (subscription.getMpPreapprovalId() == null || subscription.getMpPreapprovalId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assinatura ainda não possui identificador do Mercado Pago para cancelar.");
        }
        mercadoPagoClient.cancelPreapproval(subscription.getMpPreapprovalId());
        billingService.manualSync("preapproval", subscription.getMpPreapprovalId(), null);
        return getBilling(empresaId);
    }

    @Transactional(readOnly = true)
    public List<BillingEventResponse> getEvents(String empresaId) {
        return eventRepository
                .findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, 100)).stream()
                .map(event -> new BillingEventResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getMpResourceType(),
                        event.getMpResourceId(),
                        event.getMpStatus(),
                        event.getAmountCents(),
                        event.getCurrency(),
                        event.getCreatedAt()))
                .toList();
    }

    private void prepareInitialPlan(EmpresaEntity empresa, CommercialPlanType requestedPlan) {
        CommercialPlanType currentPlan = empresa.getCommercialPlanType();
        if (currentPlan != null && currentPlan != requestedPlan) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sua empresa já utiliza outro modelo de cobrança. Entre em contato com o suporte para migrar de plano.");
        }
        if (currentPlan == null) {
            empresa.setCommercialPlanType(requestedPlan);
            if (!empresa.getStatus().blocksAccess()) {
                empresa.setStatus(EmpresaStatus.PENDENTE_PAGAMENTO);
            }
            empresa.setUpdatedAt(Instant.now());
        }
    }

    private EmpresaEntity requireEmpresa(String empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
    }

    private static List<String> resolveActions(CommercialPlanType plan, EmpresaStatus status, int creditBalance) {
        List<String> actions = new ArrayList<>();
        if (plan == null) {
            actions.add("CHOOSE_PLAN");
        } else if (plan == CommercialPlanType.AVULSO) {
            actions.add("BUY_CREDITS");
            actions.add("MANAGE_AUTO_RECHARGE");
            actions.add("VIEW_HISTORY");
        } else if (plan == CommercialPlanType.PROFISSIONAL) {
            actions.add("VIEW_SUBSCRIPTION");
            actions.add("SYNC_SUBSCRIPTION");
            actions.add("CANCEL_SUBSCRIPTION");
            actions.add("VIEW_HISTORY");
            if (status == EmpresaStatus.INADIMPLENTE || status == EmpresaStatus.PENDENTE_PAGAMENTO) {
                actions.add("UPDATE_PAYMENT");
            }
        } else if (plan == CommercialPlanType.ENTERPRISE) {
            actions.add("CONTACT_SUPPORT");
        }
        return actions;
    }

    private static String financialStatusLabel(EmpresaStatus status) {
        return switch (status) {
            case ATIVO, EM_TESTE -> "REGULAR";
            case PENDENTE_PAGAMENTO -> "PENDENTE_PAGAMENTO";
            case INADIMPLENTE -> "INADIMPLENTE";
            case SEM_CREDITO -> "SEM_CREDITO";
            case SUSPENSO, CANCELADO -> "CANCELADO";
        };
    }
}