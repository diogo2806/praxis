package br.com.iforce.praxis.enterpriseauth.controller;

import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.CallbackResponse;
import br.com.iforce.praxis.enterpriseauth.service.EnterpriseIdentityProviderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/enterprise-auth")
public class EnterpriseAuthRedirectController {

    private final EnterpriseIdentityProviderService identityProviderService;

    public EnterpriseAuthRedirectController(
            EnterpriseIdentityProviderService identityProviderService
    ) {
        this.identityProviderService = identityProviderService;
    }

    @GetMapping("/callback/redirect")
    public ResponseEntity<Void> callbackRedirect(
            @RequestParam String state,
            @RequestParam String code,
            HttpServletRequest servletRequest
    ) {
        CallbackResponse callback = identityProviderService.callback(
                state,
                code,
                clientIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        );
        String separator = callback.returnUri().contains("#") ? "&" : "#";
        String target = callback.returnUri()
                + separator
                + "enterprise_token=" + encode(callback.token())
                + "&email=" + encode(callback.email())
                + "&name=" + encode(callback.displayName())
                + "&empresa_id=" + encode(callback.empresaId())
                + "&roles=" + encode(String.join(",", callback.roles()))
                + "&mfa=" + callback.mfaVerified();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(target).toASCIIString())
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",", 2)[0].trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
