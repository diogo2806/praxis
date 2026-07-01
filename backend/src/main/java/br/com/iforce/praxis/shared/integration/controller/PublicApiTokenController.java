package br.com.iforce.praxis.shared.integration.controller;

import br.com.iforce.praxis.shared.integration.dto.PublicApiTokenResponse;

import br.com.iforce.praxis.shared.integration.service.PublicApiTokenService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


/**
 * Geração e rotação do token de API pública do cliente (CUSTOM_API). Fica sob
 * {@code /api/v1/integrations/**}, já exigindo o papel do tenant na
 * {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/integrations/custom-api/api-token")
@Tag(name = "API pública", description = "Token de API pública para o cliente consumir a Práxis.")
public class PublicApiTokenController {

    private final PublicApiTokenService publicApiTokenService;

    public PublicApiTokenController(PublicApiTokenService publicApiTokenService) {
        this.publicApiTokenService = publicApiTokenService;
    }

    @PostMapping
    @Operation(summary = "Gera ou rotaciona o token de API pública")
    public ResponseEntity<PublicApiTokenResponse> generate() {
        return ResponseEntity.ok(publicApiTokenService.generateToken());
    }

    @DeleteMapping
    @Operation(summary = "Revoga o token de API pública")
    public ResponseEntity<Void> revoke() {
        publicApiTokenService.revokeToken();
        return ResponseEntity.noContent().build();
    }
}
