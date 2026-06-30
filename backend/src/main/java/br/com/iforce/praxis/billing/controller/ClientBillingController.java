package br.com.iforce.praxis.billing.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.billing.dto.ClientBillingResponse;

import br.com.iforce.praxis.billing.service.ClientBillingService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


/** API de cobrança visível ao próprio cliente (empresa autenticado). */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing · Cliente", description = "Visão de plano e uso para o cliente autenticado.")
public class ClientBillingController {

    private final CurrentEmpresaService currentEmpresaService;
    private final ClientBillingService clientBillingService;

    public ClientBillingController(CurrentEmpresaService currentEmpresaService,
                                   ClientBillingService clientBillingService) {
        this.currentEmpresaService = currentEmpresaService;
        this.clientBillingService = clientBillingService;
    }

    @GetMapping
    @Operation(summary = "Plano, uso e cobrança do empresa autenticado")
    public ResponseEntity<ClientBillingResponse> getBilling() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(clientBillingService.getBilling(empresaId));
    }

    @GetMapping("/events")
    @Operation(summary = "Histórico de eventos financeiros do empresa autenticado")
    public ResponseEntity<java.util.List<br.com.iforce.praxis.billing.dto.BillingEventResponse>> getEvents() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(clientBillingService.getEvents(empresaId));
    }
}
