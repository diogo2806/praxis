package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.billing.dto.BillingEventResponse;
import br.com.iforce.praxis.billing.dto.ClientBillingResponse;
import br.com.iforce.praxis.billing.persistence.repository.TenantBillingEventRepository;
import br.com.iforce.praxis.billing.persistence.repository.TenantSubscriptionRepository;
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

/** Visão de cobrança do próprio tenant (cliente autenticado). */
@Service
public class ClientBillingService {

    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantBillingEventRepository eventRepository;
    private final CandidateAttemptRepository attemptRepository;
    private final CreditService creditService;

    public ClientBillingService(TenantRepository tenantRepository,
                                TenantSubscriptionRepository subscriptionRepository,
                                TenantBillingEventRepository eventRepository,
                                CandidateAttemptRepository attemptRepository,
                                CreditService creditService) {
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
        this.creditService = creditService;
    }

    @Transactional(readOnly = true)
    public ClientBillingResponse getBilling(String tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));

        Instant now = Instant.now();
        long last7 = attemptRepository.countByTenantIdAndStatusAndFinishedAtAfter(
                tenantId, AttemptStatus.COMPLETED, now.minus(7, ChronoUnit.DAYS));
        long last30 = attemptRepository.countByTenantIdAndStatusAndFinishedAtAfter(
                tenantId, AttemptStatus.COMPLETED, now.minus(30, ChronoUnit.DAYS));
        long allTime = attemptRepository.countByTenantIdAndStatus(tenantId, AttemptStatus.COMPLETED);

        var subscription = subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId).orElse(null);

        ClientBillingResponse.SubscriptionInfo subscriptionInfo = subscription == null ? null
                : new ClientBillingResponse.SubscriptionInfo(
                subscription.getStatus(),
                subscription.getCurrentPeriodEnd(),
                subscription.getLastPaymentAt(),
                subscription.getGraceUntil());

        List<String> actions = resolveActions(tenant.getCommercialPlanType(), tenant.getStatus(),
                creditService.getBalance(tenantId));

        List<BillingEventResponse> events = eventRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 50)).stream()
                .map(e -> new BillingEventResponse(e.getId(), e.getEventType(), e.getMpResourceType(),
                        e.getMpResourceId(), e.getMpStatus(), e.getAmountCents(), e.getCurrency(), e.getCreatedAt()))
                .toList();

        return new ClientBillingResponse(
                tenantId,
                tenant.getCommercialPlanType(),
                tenant.getStatus(),
                financialStatusLabel(tenant.getStatus()),
                creditService.getBalance(tenantId),
                new ClientBillingResponse.UsageSummary(last7, last30, allTime),
                subscriptionInfo,
                actions,
                events);
    }

    @Transactional(readOnly = true)
    public List<BillingEventResponse> getEvents(String tenantId) {
        return eventRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 100)).stream()
                .map(e -> new BillingEventResponse(e.getId(), e.getEventType(), e.getMpResourceType(),
                        e.getMpResourceId(), e.getMpStatus(), e.getAmountCents(), e.getCurrency(), e.getCreatedAt()))
                .toList();
    }

    private static List<String> resolveActions(CommercialPlanType plan, TenantStatus status, int creditBalance) {
        List<String> actions = new ArrayList<>();
        if (plan == CommercialPlanType.AVULSO) {
            actions.add("BUY_CREDITS");
            actions.add("VIEW_HISTORY");
        } else if (plan == CommercialPlanType.PROFISSIONAL) {
            actions.add("VIEW_SUBSCRIPTION");
            if (status == TenantStatus.INADIMPLENTE || status == TenantStatus.PENDENTE_PAGAMENTO) {
                actions.add("UPDATE_PAYMENT");
            }
        } else if (plan == CommercialPlanType.ENTERPRISE) {
            actions.add("CONTACT_SUPPORT");
        }
        return actions;
    }

    private static String financialStatusLabel(TenantStatus status) {
        return switch (status) {
            case ATIVO, EM_TESTE -> "REGULAR";
            case PENDENTE_PAGAMENTO -> "PENDENTE_PAGAMENTO";
            case INADIMPLENTE -> "INADIMPLENTE";
            case SEM_CREDITO -> "SEM_CREDITO";
            case SUSPENSO, CANCELADO -> "CANCELADO";
        };
    }
}
