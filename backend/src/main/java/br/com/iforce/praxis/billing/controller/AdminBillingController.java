package br.com.iforce.praxis.billing.controller;

import br.com.iforce.praxis.billing.dto.CheckoutResponse;

import br.com.iforce.praxis.billing.dto.ManualSyncRequest;

import br.com.iforce.praxis.billing.dto.SubscriptionPlanResponse;

import br.com.iforce.praxis.billing.dto.EmpresaBillingOverviewResponse;

import br.com.iforce.praxis.billing.persistence.repository.SubscriptionPlanRepository;

import br.com.iforce.praxis.billing.service.BillingService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;


import java.util.List;


/**
 * API administrativa de cobrança (Parte B). Exige papel {@code ADMIN}.
 *
 * <p>O ADMIN cria cobranças e sincroniza com o Mercado Pago, mas nunca marca pagamento como
 * aprovado manualmente: a confirmação vem sempre de consulta à API do MP.</p>
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin · Cobrança", description = "Créditos AVULSO, assinatura PROFISSIONAL e sincronização Mercado Pago.")
public class AdminBillingController {

    private final BillingService billingService;
    private final SubscriptionPlanRepository planRepository;

    public AdminBillingController(BillingService billingService, SubscriptionPlanRepository planRepository) {
        this.billingService = billingService;
        this.planRepository = planRepository;
    }

    /**
     * Lista os planos e pacotes de cobrança que estão ativos, para o ADMIN escolher ao criar uma
     * cobrança. Vem ordenado do mais barato para o mais caro.
     *
     * @return os planos ativos disponíveis (pacotes de crédito e assinaturas)
     */
    @GetMapping("/billing/plans")
    @Operation(summary = "Lista planos de cobrança ativos")
    public ResponseEntity<List<SubscriptionPlanResponse>> plans() {
        List<SubscriptionPlanResponse> plans = planRepository.findByActiveTrueOrderByPriceCentsAsc().stream()
                .map(plan -> new SubscriptionPlanResponse(plan.getId(), plan.getCode(), plan.getName(),
                        plan.getPlanType(), plan.getPriceCents(), plan.getCurrency(), plan.getCreditAmount()))
                .toList();
        return ResponseEntity.ok(plans);
    }

    /**
     * Abre a visão consolidada de cobrança de um cliente específico, para o ADMIN acompanhar plano,
     * situação, saldo, assinatura e histórico daquele cliente.
     *
     * @param empresaId identificador do cliente a consultar
     * @return a visão consolidada de cobrança do cliente
     */
    @GetMapping("/empresas/{empresaId}/billing")
    @Operation(summary = "Visão de cobrança do cliente")
    public ResponseEntity<EmpresaBillingOverviewResponse> overview(@PathVariable String empresaId) {
        return ResponseEntity.ok(billingService.overview(empresaId));
    }

    /**
     * Cria a compra de um pacote de créditos (plano AVULSO) para um cliente e devolve o link de
     * pagamento do Mercado Pago. Os créditos só entram após o pagamento ser confirmado.
     *
     * @param empresaId identificador do cliente que vai comprar os créditos
     * @param planId identificador do pacote de créditos escolhido
     * @return o checkout criado, com o link de pagamento
     */
    @PostMapping("/empresas/{empresaId}/billing/credits/checkout")
    @Operation(summary = "Cria checkout de compra de créditos (AVULSO)")
    public ResponseEntity<CheckoutResponse> creditCheckout(
            @PathVariable String empresaId,
            @RequestParam Long planId
    ) {
        return ResponseEntity.ok(billingService.createCreditCheckout(empresaId, planId));
    }

    /**
     * Cria uma assinatura mensal recorrente (plano PROFISSIONAL) para um cliente e devolve o link
     * para ele autorizar a cobrança no Mercado Pago.
     *
     * @param empresaId identificador do cliente que vai assinar
     * @param planId identificador do plano de assinatura escolhido
     * @return o checkout criado, com o link para autorizar a assinatura
     */
    @PostMapping("/empresas/{empresaId}/billing/subscription")
    @Operation(summary = "Cria assinatura recorrente (PROFISSIONAL)")
    public ResponseEntity<CheckoutResponse> subscription(
            @PathVariable String empresaId,
            @RequestParam Long planId
    ) {
        return ResponseEntity.ok(billingService.createSubscription(empresaId, planId));
    }

    /**
     * Reconfere manualmente um recurso do Mercado Pago (um pagamento ou uma assinatura) quando uma
     * notificação automática pode ter se perdido, e devolve a visão de cobrança já atualizada. Assim
     * como as notificações, esta ação consulta o Mercado Pago antes de aplicar qualquer efeito.
     *
     * @param empresaId identificador do cliente cuja cobrança será reconferida
     * @param request tipo e identificador do recurso do Mercado Pago a reconferir
     * @return a visão de cobrança do cliente após a sincronização
     */
    @PostMapping("/empresas/{empresaId}/billing/sync")
    @Operation(summary = "Sincroniza um recurso do Mercado Pago manualmente")
    public ResponseEntity<EmpresaBillingOverviewResponse> sync(
            @PathVariable String empresaId,
            @Valid @RequestBody ManualSyncRequest request
    ) {
        billingService.manualSync(request.resourceType(), request.resourceId(), null);
        return ResponseEntity.ok(billingService.overview(empresaId));
    }
}
