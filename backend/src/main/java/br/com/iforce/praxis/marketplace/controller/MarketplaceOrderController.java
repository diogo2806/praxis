package br.com.iforce.praxis.marketplace.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.marketplace.dto.CheckoutRequest;
import br.com.iforce.praxis.marketplace.dto.CheckoutResponse;
import br.com.iforce.praxis.marketplace.dto.MarketplaceOrderResponse;
import br.com.iforce.praxis.marketplace.service.MarketplaceOrderService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/marketplace/orders")
public class MarketplaceOrderController {

    private final MarketplaceOrderService orderService;
    private final CurrentEmpresaService currentEmpresaService;

    public MarketplaceOrderController(
            MarketplaceOrderService orderService,
            CurrentEmpresaService currentEmpresaService
    ) {
        this.orderService = orderService;
        this.currentEmpresaService = currentEmpresaService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(201).body(orderService.checkout(
                currentEmpresaService.requiredEmpresaId(),
                request
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarketplaceOrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(currentEmpresaService.requiredEmpresaId(), id));
    }

    @GetMapping
    public ResponseEntity<List<MarketplaceOrderResponse>> list() {
        return ResponseEntity.ok(orderService.listOrders(currentEmpresaService.requiredEmpresaId()));
    }
}
