package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.billing.config.MercadoPagoProperties;
import br.com.iforce.praxis.billing.dto.BillingEventResponse;
import br.com.iforce.praxis.billing.dto.CheckoutResponse;
import br.com.iforce.praxis.billing.dto.TenantBillingOverviewResponse;
import br.com.iforce.praxis.billing.model.BillingEventType;
import br.com.iforce.praxis.billing.model.SubscriptionStatus;
import br.com.iforce.praxis.billing.persistence.entity.SubscriptionPlanEntity;
import br.com.iforce.praxis.billing.persistence.entity.TenantBillingEventEntity;
import br.com.iforce.praxis.billing.persistence.entity.TenantSubscriptionEntity;
import br.com.iforce.praxis.billing.persistence.repository.SubscriptionPlanRepository;
import br.com.iforce.praxis.billing.persistence.repository.TenantBillingEventRepository;
import br.com.iforce.praxis.billing.persistence.repository.TenantSubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra a cobrança via Mercado Pago.
 *
 * <p>Princípio: o Mercado Pago é a fonte da verdade financeira. Nenhum pagamento é marcado como
 * aprovado por ação manual — toda confirmação vem de uma consulta à API do MP (disparada por
 * webhook ou por sincronização manual). Todo efeito financeiro gera evento append-only e é
 * idempotente por recurso do MP.</p>
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final String KIND_CREDIT = "credit";
    private static final String KIND_SUB = "sub";

    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoProperties properties;
    private final SubscriptionPlanRepository planRepository;
    private final TenantBillingEventRepository eventRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final CreditService creditService;
    private final TenantRepository tenantRepository;

    public BillingService(
            MercadoPagoClient mercadoPagoClient,
            MercadoPagoProperties properties,
            SubscriptionPlanRepository planRepository,
            TenantBillingEventRepository eventRepository,
            TenantSubscriptionRepository subscriptionRepository,
            CreditService creditService,
            TenantRepository tenantRepository
    ) {
        this.mercadoPagoClient = mercadoPagoClient;
        this.properties = properties;
        this.planRepository = planRepository;
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.creditService = creditService;
        this.tenantRepository = tenantRepository;
    }

    // ------------------------------------------------------------------
    // Criação de cobranças (ADMIN)
    // ------------------------------------------------------------------

    /** AVULSO: cria o checkout de compra de um pacote de créditos. */
    @Transactional
    public CheckoutResponse createCreditCheckout(String tenantId, Long planId) {
        requireTenant(tenantId);
        SubscriptionPlanEntity plan = requirePlan(planId);
        if (plan.getPlanType() != CommercialPlanType.AVULSO || plan.getCreditAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano não é um pacote de créditos AVULSO.");
        }
        if (!plan.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano inativo.");
        }
        String externalReference = reference(KIND_CREDIT, tenantId, planId);
        JsonNode response = mercadoPagoClient.createCreditPreference(plan, externalReference,
                java.util.Map.of("tenant_id", tenantId, "plan_id", planId, "kind", KIND_CREDIT));

        String preferenceId = text(response, "id");
        String initPoint = text(response, "init_point");
        recordEvent(tenantId, BillingEventType.CREDIT_CHECKOUT_CREATED, "preference", preferenceId,
                externalReference, "created", plan.getPriceCents(), plan.getCurrency(), null, response);

        return new CheckoutResponse(KIND_CREDIT, preferenceId, initPoint, externalReference);
    }

    /** PROFISSIONAL: cria a assinatura recorrente no Mercado Pago. */
    @Transactional
    public CheckoutResponse createSubscription(String tenantId, Long planId) {
        TenantEntity tenant = requireTenant(tenantId);
        SubscriptionPlanEntity plan = requirePlan(planId);
        if (plan.getPlanType() != CommercialPlanType.PROFISSIONAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano não é uma assinatura PROFISSIONAL.");
        }
        if (!plan.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano inativo.");
        }
        // Bloqueia criar nova assinatura se já existe uma PENDING, AUTHORIZED ou DELINQUENT.
        subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)
                .filter(sub -> sub.getStatus() == SubscriptionStatus.PENDING
                        || sub.getStatus() == SubscriptionStatus.AUTHORIZED
                        || sub.getStatus() == SubscriptionStatus.DELINQUENT)
                .ifPresent(sub -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Cliente já possui assinatura ativa ou pendente."
                    );
                });
        String externalReference = reference(KIND_SUB, tenantId, planId);
        JsonNode response = mercadoPagoClient.createPreapproval(plan, tenant.getCorporateEmail(), externalReference);

        String preapprovalId = text(response, "id");
        String initPoint = text(response, "init_point");

        TenantSubscriptionEntity subscription = new TenantSubscriptionEntity();
        subscription.setTenantId(tenantId);
        subscription.setPlanId(planId);
        subscription.setMpPreapprovalId(preapprovalId);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setInitPoint(initPoint);
        subscription.setCreatedAt(Instant.now());
        subscription.setUpdatedAt(Instant.now());
        subscriptionRepository.save(subscription);

        recordEvent(tenantId, BillingEventType.SUBSCRIPTION_CREATED, "preapproval", preapprovalId,
                externalReference, "pending", plan.getPriceCents(), plan.getCurrency(), null, response);

        if (!tenant.getStatus().blocksAccess()) {
            tenant.setStatus(TenantStatus.PENDENTE_PAGAMENTO);
            tenant.setUpdatedAt(Instant.now());
        }
        return new CheckoutResponse(KIND_SUB, preapprovalId, initPoint, externalReference);
    }

    // ------------------------------------------------------------------
    // Processamento de notificações (consulta o MP antes de aplicar)
    // ------------------------------------------------------------------

    @Transactional
    public void processPaymentNotification(String paymentId, String requestId) {
        JsonNode payment = mercadoPagoClient.getPayment(paymentId);
        String status = text(payment, "status");
        String externalReference = text(payment, "external_reference");
        Long amountCents = cents(payment);
        String[] ref = parseReference(externalReference);
        String kind = ref[0];
        String tenantId = ref[1];
        Long planId = ref[2] == null ? null : Long.valueOf(ref[2]);

        if (tenantId == null) {
            log.warn("Pagamento {} sem external_reference atribuível; ignorado.", paymentId);
            return;
        }

        if ("approved".equals(status)) {
            if (KIND_CREDIT.equals(kind)) {
                if (eventRepository.existsByMpResourceIdAndEventType(paymentId, BillingEventType.CREDIT_PURCHASE_APPROVED)) {
                    return;
                }
                SubscriptionPlanEntity plan = requirePlan(planId);
                TenantBillingEventEntity event = recordEvent(tenantId, BillingEventType.CREDIT_PURCHASE_APPROVED,
                        "payment", paymentId, externalReference, status, amountCents, "BRL", requestId, payment);
                creditService.addCredits(tenantId, plan.getCreditAmount(), event.getId(),
                        "Compra de créditos confirmada (pagamento " + paymentId + ")");
            } else if (KIND_SUB.equals(kind)) {
                if (eventRepository.existsByMpResourceIdAndEventType(paymentId, BillingEventType.SUBSCRIPTION_PAYMENT_APPROVED)) {
                    return;
                }
                recordEvent(tenantId, BillingEventType.SUBSCRIPTION_PAYMENT_APPROVED, "payment", paymentId,
                        externalReference, status, amountCents, "BRL", requestId, payment);
                markSubscriptionPaid(tenantId);
            }
        } else if ("rejected".equals(status) || "cancelled".equals(status)) {
            if (KIND_SUB.equals(kind)) {
                recordEvent(tenantId, BillingEventType.SUBSCRIPTION_PAYMENT_REJECTED, "payment", paymentId,
                        externalReference, status, amountCents, "BRL", requestId, payment);
                startDelinquency(tenantId);
            } else {
                recordEvent(tenantId, BillingEventType.PAYMENT_PENDING, "payment", paymentId,
                        externalReference, status, amountCents, "BRL", requestId, payment);
            }
        } else if ("refunded".equals(status)) {
            recordEvent(tenantId, BillingEventType.PAYMENT_REFUNDED, "payment", paymentId,
                    externalReference, status, amountCents, "BRL", requestId, payment);
        } else if ("charged_back".equals(status)) {
            recordEvent(tenantId, BillingEventType.PAYMENT_CHARGEBACK, "payment", paymentId,
                    externalReference, status, amountCents, "BRL", requestId, payment);
        } else {
            recordEvent(tenantId, BillingEventType.PAYMENT_PENDING, "payment", paymentId,
                    externalReference, status, amountCents, "BRL", requestId, payment);
        }
    }

    @Transactional
    public void processPreapprovalNotification(String preapprovalId, String requestId) {
        JsonNode preapproval = mercadoPagoClient.getPreapproval(preapprovalId);
        String status = text(preapproval, "status");
        TenantSubscriptionEntity subscription = subscriptionRepository.findByMpPreapprovalId(preapprovalId)
                .orElse(null);
        if (subscription == null) {
            String[] ref = parseReference(text(preapproval, "external_reference"));
            if (ref[1] == null) {
                log.warn("Preapproval {} desconhecido e sem referência; ignorado.", preapprovalId);
                return;
            }
            subscription = subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(ref[1]).orElse(null);
            if (subscription == null) {
                return;
            }
        }
        String tenantId = subscription.getTenantId();

        if ("authorized".equals(status)) {
            subscription.setStatus(SubscriptionStatus.AUTHORIZED);
            subscription.setGraceUntil(null);
            subscription.setUpdatedAt(Instant.now());
            recordEvent(tenantId, BillingEventType.SUBSCRIPTION_AUTHORIZED, "preapproval", preapprovalId,
                    null, status, null, null, requestId, preapproval);
            activateTenant(tenantId);
        } else if ("paused".equals(status)) {
            subscription.setStatus(SubscriptionStatus.PAUSED);
            subscription.setUpdatedAt(Instant.now());
            recordEvent(tenantId, BillingEventType.PAYMENT_PENDING, "preapproval", preapprovalId,
                    null, status, null, null, requestId, preapproval);
        } else if ("cancelled".equals(status)) {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setUpdatedAt(Instant.now());
            recordEvent(tenantId, BillingEventType.SUBSCRIPTION_CANCELLED, "preapproval", preapprovalId,
                    null, status, null, null, requestId, preapproval);
        }
    }

    /** Sincronização manual disparada pelo ADMIN: também consulta o MP antes de aplicar. */
    @Transactional
    public void manualSync(String resourceType, String resourceId, String requestId) {
        if ("payment".equalsIgnoreCase(resourceType)) {
            processPaymentNotification(resourceId, requestId);
        } else if ("preapproval".equalsIgnoreCase(resourceType) || "subscription".equalsIgnoreCase(resourceType)) {
            processPreapprovalNotification(resourceId, requestId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de recurso inválido para sincronização.");
        }
    }

    // ------------------------------------------------------------------
    // Visão consolidada
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public TenantBillingOverviewResponse overview(String tenantId) {
        TenantEntity tenant = requireTenant(tenantId);
        TenantSubscriptionEntity subscription = subscriptionRepository
                .findFirstByTenantIdOrderByCreatedAtDesc(tenantId).orElse(null);
        List<BillingEventResponse> events = eventRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 100)).stream()
                .map(e -> new BillingEventResponse(e.getId(), e.getEventType(), e.getMpResourceType(),
                        e.getMpResourceId(), e.getMpStatus(), e.getAmountCents(), e.getCurrency(), e.getCreatedAt()))
                .toList();

        TenantBillingOverviewResponse.SubscriptionInfo subscriptionInfo = subscription == null ? null
                : new TenantBillingOverviewResponse.SubscriptionInfo(
                subscription.getId(), subscription.getStatus(), subscription.getMpPreapprovalId(),
                subscription.getInitPoint(), subscription.getCurrentPeriodEnd(),
                subscription.getLastPaymentAt(), subscription.getGraceUntil());

        return new TenantBillingOverviewResponse(
                tenantId, tenant.getCommercialPlanType(), tenant.getStatus(),
                creditService.getBalance(tenantId), subscriptionInfo, events);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private void markSubscriptionPaid(String tenantId) {
        subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId).ifPresent(subscription -> {
            subscription.setStatus(SubscriptionStatus.AUTHORIZED);
            subscription.setLastPaymentAt(Instant.now());
            subscription.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
            subscription.setGraceUntil(null);
            subscription.setUpdatedAt(Instant.now());
        });
        activateTenant(tenantId);
    }

    private void startDelinquency(String tenantId) {
        Instant graceUntil = Instant.now().plus(properties.gracePeriodDays(), ChronoUnit.DAYS);
        subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId).ifPresent(subscription -> {
            subscription.setStatus(SubscriptionStatus.DELINQUENT);
            subscription.setGraceUntil(graceUntil);
            subscription.setUpdatedAt(Instant.now());
        });
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            if (!tenant.getStatus().blocksAccess()) {
                tenant.setStatus(TenantStatus.INADIMPLENTE);
                tenant.setUpdatedAt(Instant.now());
            }
        });
    }

    private void activateTenant(String tenantId) {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            if (tenant.getStatus() == TenantStatus.PENDENTE_PAGAMENTO
                    || tenant.getStatus() == TenantStatus.INADIMPLENTE
                    || tenant.getStatus() == TenantStatus.SEM_CREDITO) {
                tenant.setStatus(TenantStatus.ATIVO);
                tenant.setUpdatedAt(Instant.now());
            }
        });
    }

    private TenantBillingEventEntity recordEvent(String tenantId, BillingEventType type, String resourceType,
                                                 String resourceId, String externalReference, String mpStatus,
                                                 Long amountCents, String currency, String requestId, JsonNode raw) {
        TenantBillingEventEntity event = new TenantBillingEventEntity();
        event.setTenantId(tenantId);
        event.setEventType(type);
        event.setMpResourceType(resourceType);
        event.setMpResourceId(resourceId);
        event.setExternalReference(externalReference);
        event.setMpStatus(mpStatus);
        event.setAmountCents(amountCents);
        event.setCurrency(currency);
        event.setRequestId(requestId);
        event.setRawPayload(truncate(raw == null ? null : raw.toString()));
        event.setCreatedAt(Instant.now());
        return eventRepository.save(event);
    }

    private TenantEntity requireTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
    }

    private SubscriptionPlanEntity requirePlan(Long planId) {
        if (planId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano não informado.");
        }
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado."));
    }

    private static String reference(String kind, String tenantId, Long planId) {
        return kind + ":" + tenantId + ":" + planId + ":" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Retorna {kind, tenantId, planId} a partir de "kind:tenantId:planId:nonce". */
    private static String[] parseReference(String externalReference) {
        if (externalReference == null) {
            return new String[]{null, null, null};
        }
        String[] parts = externalReference.split(":");
        if (parts.length < 3) {
            return new String[]{null, null, null};
        }
        return new String[]{parts[0], parts[1], parts[2]};
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Long cents(JsonNode payment) {
        if (payment == null) {
            return null;
        }
        JsonNode amount = payment.get("transaction_amount");
        if (amount == null || amount.isNull()) {
            return null;
        }
        return Math.round(amount.asDouble() * 100);
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 8000 ? value.substring(0, 8000) : value;
    }
}
