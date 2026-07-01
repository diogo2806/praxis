package br.com.iforce.praxis.shared.integration.controller;

import br.com.iforce.praxis.shared.integration.dto.ConfigureGenericWebhookRequest;

import br.com.iforce.praxis.shared.integration.dto.GenericWebhookConfigResponse;

import br.com.iforce.praxis.shared.integration.dto.WebhookSecretResponse;

import br.com.iforce.praxis.shared.integration.dto.WebhookTestResponse;

import br.com.iforce.praxis.shared.integration.service.GenericWebhookDeliveryService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


/**
 * Configuração e teste do webhook personalizado assinado (CUSTOM_API). Fica sob
 * {@code /api/v1/integrations/**}, já exigindo o papel do tenant na
 * {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/integrations/custom-api/webhook")
@Tag(name = "Webhook personalizado", description = "Webhook genérico assinado por HMAC (CUSTOM_API).")
public class GenericWebhookController {

    private final GenericWebhookDeliveryService genericWebhookDeliveryService;

    public GenericWebhookController(GenericWebhookDeliveryService genericWebhookDeliveryService) {
        this.genericWebhookDeliveryService = genericWebhookDeliveryService;
    }

    @GetMapping
    @Operation(summary = "Lê a configuração do webhook personalizado")
    public ResponseEntity<GenericWebhookConfigResponse> getConfig() {
        return ResponseEntity.ok(genericWebhookDeliveryService.getConfig());
    }

    @PostMapping
    @Operation(summary = "Configura o webhook personalizado")
    public ResponseEntity<GenericWebhookConfigResponse> configure(
            @Valid @RequestBody ConfigureGenericWebhookRequest request
    ) {
        return ResponseEntity.ok(genericWebhookDeliveryService.configure(request));
    }

    @PostMapping("/secret/rotate")
    @Operation(summary = "Rotaciona o segredo HMAC do webhook")
    public ResponseEntity<WebhookSecretResponse> rotateSecret() {
        return ResponseEntity.ok(genericWebhookDeliveryService.rotateSecret());
    }

    @PostMapping("/test")
    @Operation(summary = "Envia um evento de teste ao webhook")
    public ResponseEntity<WebhookTestResponse> test() {
        return ResponseEntity.ok(genericWebhookDeliveryService.sendTestEvent());
    }
}
