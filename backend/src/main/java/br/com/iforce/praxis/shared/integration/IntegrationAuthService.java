package br.com.iforce.praxis.shared.integration;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class IntegrationAuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final IntegrationTokenRepository integrationTokenRepository;

    public IntegrationAuthService(IntegrationTokenRepository integrationTokenRepository) {
        this.integrationTokenRepository = integrationTokenRepository;
    }

    public IntegrationTenantContext validateBearerToken(String authorizationHeader, String provider) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer obrigatório.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        String tokenHash = sha256(token);

        return integrationTokenRepository.findFirstByProviderAndTokenHash(provider, tokenHash)
                .map(entity -> new IntegrationTenantContext(
                        entity.getTenant().getId(),
                        entity.getTenant().getCompanyId()
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer inválido."));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
