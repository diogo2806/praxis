package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.EmpresaStatus;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.billing.dto.CheckoutResponse;
import br.com.iforce.praxis.billing.dto.PlanChangeRequestResponse;
import br.com.iforce.praxis.billing.dto.PlanManagementResponse;
import br.com.iforce.praxis.billing.model.PlanChangeRequestType;
import br.com.iforce.praxis.billing.model.SubscriptionStatus;
import br.com.iforce.praxis.billing.persistence.entity.EmpresaPlanChangeRequestEntity;
import br.com.iforce.praxis.billing.persistence.entity.EmpresaSubscriptionEntity;
import br.com.iforce.praxis.billing.persistence.entity.SubscriptionPlanEntity;
import br.com.iforce.praxis.billing.persistence.repository.CommercialRequestRepository;
import br.com.iforce.praxis.billing.persistence.repository.EmpresaSubscriptionRepository;
import br.com.iforce.praxis.billing.persistence.repository.SubscriptionPlanRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ClientPlanManagementService {
    private static final String PENDING = "PENDING";

    private final EmpresaRepository empresaRepository;
    private final EmpresaSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final CommercialRequestRepository requestRepository;
    private final BillingService billingService;
    private final MercadoPagoClient mercadoPagoClient;
    private final CreditService creditService;

    public ClientPlanManagementService(EmpresaRepository empresaRepository,
                                       EmpresaSubscriptionRepository subscriptionRepository,
                                       SubscriptionPlanRepository planRepository,
                                       CommercialRequestRepository requestRepository,
                                       BillingService billingService,
                                       MercadoPagoClient mercadoPagoClient,
                                       CreditService creditService) {
        this.empresaRepository = empresaRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.requestRepository = requestRepository;
        this.billingService = billingService;
        this.mercadoPagoClient = mercadoPagoClient;
        this.creditService = creditService;
    }

    @Transactional(readOnly = true)
    public PlanManagementResponse getManagement(String empresaId) {
        EmpresaEntity empresa = empresa(empresaId);
        List<PlanChangeRequestResponse> requests = requestRepository
                .findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, 20)).stream()
                .map(this::response)
                .toList();
        return new PlanManagementResponse(empresa.getCommercialPlanType(), requests);
    }

    @Transactional
    public CheckoutResponse changePlan(String empresaId, Long targetPlanId) {
        EmpresaEntity empresa = empresa(empresaId);
        if (empresa.getCommercialPlanType() == CommercialPlanType.ENTERPRISE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O contrato Enterprise deve ser alterado por solicitação comercial.");
        }
        SubscriptionPlanEntity target = activePlan(targetPlanId);
        if (target.getPlanType() == CommercialPlanType.AVULSO) {
            if (empresa.getCommercialPlanType() == CommercialPlanType.PROFISSIONAL) {
                cancelRecurringSubscription(empresaId);
            }
            empresa.setCommercialPlanType(CommercialPlanType.AVULSO);
            if (!empresa.getStatus().blocksAccess() && creditService.getBalance(empresaId) == 0) {
                empresa.setStatus(EmpresaStatus.SEM_CREDITO);
            }
            empresa.setUpdatedAt(Instant.now());
            return billingService.createCreditCheckout(empresaId, targetPlanId);
        }
        if (target.getPlanType() != CommercialPlanType.PROFISSIONAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano de destino inválido.");
        }
        EmpresaSubscriptionEntity current = subscriptionRepository
                .findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).orElse(null);
        if (empresa.getCommercialPlanType() == CommercialPlanType.PROFISSIONAL && live(current)) {
            if (targetPlanId.equals(current.getPlanId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este já é o plano mensal atual.");
            }
            cancelRecurringSubscription(empresaId);
        }
        empresa.setCommercialPlanType(CommercialPlanType.PROFISSIONAL);
        if (!empresa.getStatus().blocksAccess()) {
            empresa.setStatus(EmpresaStatus.PENDENTE_PAGAMENTO);
        }
        empresa.setUpdatedAt(Instant.now());
        return billingService.createSubscription(empresaId, targetPlanId);
    }

    @Transactional
    public PlanChangeRequestResponse requestEnterpriseChange(String empresaId,
                                                              PlanChangeRequestType type,
                                                              CommercialPlanType requestedPlan,
                                                              String note) {
        EmpresaEntity empresa = empresa(empresaId);
        if (empresa.getCommercialPlanType() != CommercialPlanType.ENTERPRISE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta solicitação é exclusiva para contratos Enterprise.");
        }
        if (requestRepository.existsByEmpresaIdAndStatus(empresaId, PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma solicitação comercial em análise.");
        }
        if (type == PlanChangeRequestType.CHANGE_PLAN
                && (requestedPlan == null || requestedPlan == CommercialPlanType.ENTERPRISE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione o plano desejado.");
        }
        if (type == PlanChangeRequestType.CANCEL_CONTRACT && requestedPlan != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O cancelamento não possui plano de destino.");
        }
        Instant now = Instant.now();
        EmpresaPlanChangeRequestEntity request = new EmpresaPlanChangeRequestEntity();
        request.setEmpresaId(empresaId);
        request.setRequestType(type);
        request.setCurrentPlan(empresa.getCommercialPlanType());
        request.setRequestedPlan(requestedPlan);
        request.setStatus(PENDING);
        request.setNote(blank(note) ? null : note.trim());
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        return response(requestRepository.save(request));
    }

    private void cancelRecurringSubscription(String empresaId) {
        EmpresaSubscriptionEntity subscription = subscriptionRepository
                .findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhuma assinatura encontrada para cancelar."));
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            return;
        }
        if (blank(subscription.getMpPreapprovalId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assinatura sem identificador para cancelamento.");
        }
        mercadoPagoClient.cancelPreapproval(subscription.getMpPreapprovalId());
        billingService.manualSync("preapproval", subscription.getMpPreapprovalId(), null);
    }

    private SubscriptionPlanEntity activePlan(Long planId) {
        SubscriptionPlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado."));
        if (!plan.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano indisponível.");
        }
        return plan;
    }

    private EmpresaEntity empresa(String empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
    }

    private static boolean live(EmpresaSubscriptionEntity subscription) {
        return subscription != null && (subscription.getStatus() == SubscriptionStatus.PENDING
                || subscription.getStatus() == SubscriptionStatus.AUTHORIZED
                || subscription.getStatus() == SubscriptionStatus.DELINQUENT);
    }

    private PlanChangeRequestResponse response(EmpresaPlanChangeRequestEntity request) {
        return new PlanChangeRequestResponse(request.getId(), request.getRequestType(), request.getCurrentPlan(),
                request.getRequestedPlan(), request.getStatus(), request.getNote(), request.getCreatedAt(), request.getUpdatedAt());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
