package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class GupyAuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TenantRepository tenantRepository;

    public GupyAuthService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public GupyTenantContext validateBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer obrigatorio.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        String tokenHash = sha256(token);

        return tenantRepository.findFirstByIntegrationTokenHash(tokenHash)
                .map(tenant -> new GupyTenantContext(tenant.getId(), tenant.getCompanyId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer invalido."));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel.", exception);
        }
    }

    public record GupyTenantContext(String tenantId, String companyId) {
    }
}
