package br.com.iforce.praxis.billing.controller;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.billing.dto.ClientBillingResponse;
import br.com.iforce.praxis.billing.service.ClientBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de cobrança visível ao próprio cliente (tenant autenticado). */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing · Cliente", description = "Visão de plano e uso para o cliente autenticado.")
public class ClientBillingController {

    private final CurrentTenantService currentTenantService;
    private final ClientBillingService clientBillingService;

    public ClientBillingController(CurrentTenantService currentTenantService,
                                   ClientBillingService clientBillingService) {
        this.currentTenantService = currentTenantService;
        this.clientBillingService = clientBillingService;
    }

    @GetMapping
    @Operation(summary = "Plano, uso e cobrança do tenant autenticado")
    public ResponseEntity<ClientBillingResponse> getBilling() {
        String tenantId = currentTenantService.requiredTenantId();
        return ResponseEntity.ok(clientBillingService.getBilling(tenantId));
    }

    @GetMapping("/events")
    @Operation(summary = "Histórico de eventos financeiros do tenant autenticado")
    public ResponseEntity<java.util.List<br.com.iforce.praxis.billing.dto.BillingEventResponse>> getEvents() {
        String tenantId = currentTenantService.requiredTenantId();
        return ResponseEntity.ok(clientBillingService.getEvents(tenantId));
    }
}
