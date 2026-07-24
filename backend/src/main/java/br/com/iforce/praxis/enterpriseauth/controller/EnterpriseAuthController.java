package br.com.iforce.praxis.enterpriseauth.controller;

import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.AuditEventResponse;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.CallbackResponse;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.DiscoveryResponse;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.IdentityProviderResponse;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.ProviderTestResponse;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.SaveIdentityProviderRequest;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.StartLoginRequest;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.StartLoginResponse;
import br.com.iforce.praxis.enterpriseauth.service.EnterpriseIdentityProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enterprise-auth")
@Tag(name = "Enterprise authentication", description = "SSO OIDC corporativo, MFA no IdP e auditoria.")
public class EnterpriseAuthController {

    private final EnterpriseIdentityProviderService identityProviderService;

    public EnterpriseAuthController(EnterpriseIdentityProviderService identityProviderService) {
        this.identityProviderService = identityProviderService;
    }

    @PostMapping("/providers")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER')")
    @Operation(summary = "Cria configuração corporativa sem persistir segredo em texto puro")
    public ResponseEntity<IdentityProviderResponse> createProvider(
            @Valid @RequestBody SaveIdentityProviderRequest request
    ) {
        return ResponseEntity.ok(identityProviderService.create(request));
    }

    @PutMapping("/providers/{providerId}")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER')")
    public ResponseEntity<IdentityProviderResponse> updateProvider(
            @PathVariable String providerId,
            @Valid @RequestBody SaveIdentityProviderRequest request
    ) {
        return ResponseEntity.ok(identityProviderService.update(providerId, request));
    }

    @GetMapping("/providers")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER')")
    public ResponseEntity<List<IdentityProviderResponse>> listProviders() {
        return ResponseEntity.ok(identityProviderService.list());
    }

    @PostMapping("/providers/{providerId}/test")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER')")
    @Operation(summary = "Valida discovery OIDC e presença da variável de segredo")
    public ResponseEntity<ProviderTestResponse> testProvider(@PathVariable String providerId) {
        return ResponseEntity.ok(identityProviderService.test(providerId));
    }

    @GetMapping("/audit-events")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER')")
    public ResponseEntity<List<AuditEventResponse>> auditEvents(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(identityProviderService.auditEvents(limit));
    }

    @GetMapping("/discovery")
    @Operation(summary = "Descobre política de acesso corporativo pelo domínio do e-mail")
    public ResponseEntity<DiscoveryResponse> discovery(@RequestParam String email) {
        return ResponseEntity.ok(identityProviderService.discoverByEmail(email));
    }

    @PostMapping("/providers/{providerId}/start")
    @Operation(summary = "Inicia autorização OIDC com state, nonce e PKCE")
    public ResponseEntity<StartLoginResponse> start(
            @PathVariable String providerId,
            @Valid @RequestBody StartLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(identityProviderService.start(
                providerId,
                request,
                clientIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @GetMapping("/callback")
    @Operation(summary = "Valida callback, assinatura OIDC, domínio e evidência de MFA")
    public ResponseEntity<CallbackResponse> callback(
            @RequestParam String state,
            @RequestParam String code,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(identityProviderService.callback(
                state,
                code,
                clientIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        ));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
