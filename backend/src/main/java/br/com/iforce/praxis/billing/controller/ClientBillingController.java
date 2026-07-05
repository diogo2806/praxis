package br.com.iforce.praxis.billing.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.billing.dto.BillingEventResponse;
import br.com.iforce.praxis.billing.dto.CheckoutResponse;
import br.com.iforce.praxis.billing.dto.ClientBillingResponse;
import br.com.iforce.praxis.billing.dto.SubscriptionPlanResponse;
import br.com.iforce.praxis.billing.service.ClientBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API de plano, pagamentos, créditos e assinatura da empresa autenticada. */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing · Cliente", description = "Visão de plano, uso, histórico e checkout para o cliente autenticado.")
public class ClientBillingController {

    private final CurrentEmpresaService currentEmpresaService;
    private final ClientBillingService clientBillingService;

    public ClientBillingController(CurrentEmpresaService currentEmpresaService,
                                   ClientBillingService clientBillingService) {
        this.currentEmpresaService = currentEmpresaService;
        this.clientBillingService = clientBillingService;
    }

    @GetMapping
    @Operation(summary = "Plano, uso, pagamentos e créditos da empresa autenticada")
    public ResponseEntity<ClientBillingResponse> getBilling() {
        return ResponseEntity.ok(clientBillingService.getBilling(currentEmpresaService.requiredEmpresaId()));
    }

    @GetMapping("/plans")
    @Operation(summary = "Lista planos e pacotes disponíveis para contratação self-service")
    public ResponseEntity<List<SubscriptionPlanResponse>> plans() {
        return ResponseEntity.ok(clientBillingService.getAvailablePlans());
    }

    @PostMapping("/credits/checkout")
    @Operation(summary = "Cria checkout de créditos para a empresa autenticada")
    public ResponseEntity<CheckoutResponse> creditCheckout(@RequestParam Long planId) {
        return ResponseEntity.ok(clientBillingService.createCreditCheckout(
                currentEmpresaService.requiredEmpresaId(), planId));
    }

    @PostMapping("/subscription/checkout")
    @Operation(summary = "Cria checkout de assinatura para a empresa autenticada")
    public ResponseEntity<CheckoutResponse> subscriptionCheckout(@RequestParam Long planId) {
        return ResponseEntity.ok(clientBillingService.createSubscriptionCheckout(
                currentEmpresaService.requiredEmpresaId(), planId));
    }

    @PostMapping("/subscription/sync")
    @Operation(summary = "Sincroniza a assinatura atual no Mercado Pago")
    public ResponseEntity<ClientBillingResponse> syncSubscription() {
        return ResponseEntity.ok(clientBillingService.syncCurrentSubscription(
                currentEmpresaService.requiredEmpresaId()));
    }

    @PostMapping("/subscription/cancel")
    @Operation(summary = "Cancela a cobrança recorrente da assinatura atual")
    public ResponseEntity<ClientBillingResponse> cancelSubscription() {
        return ResponseEntity.ok(clientBillingService.cancelCurrentSubscription(
                currentEmpresaService.requiredEmpresaId()));
    }

    @GetMapping("/events")
    @Operation(summary = "Histórico financeiro da empresa autenticada")
    public ResponseEntity<List<BillingEventResponse>> getEvents() {
        return ResponseEntity.ok(clientBillingService.getEvents(currentEmpresaService.requiredEmpresaId()));
    }
}
