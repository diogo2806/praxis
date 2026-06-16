package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.config.PraxisProperties;
import org.springframework.beans.factory.annotation.Value;
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

    private final PraxisProperties properties;
    private final TenantRepository tenantRepository;
    private final String defaultTenantId;

    public GupyAuthService(
            PraxisProperties properties,
            TenantRepository tenantRepository,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.default-tenant-id:tenant-1}") String defaultTenantId
    ) {
        if (securityEnabled && (properties.integrationToken() == null
                || properties.integrationToken().isBlank()
                || "dev-company-token".equals(properties.integrationToken()))) {
            throw new IllegalStateException("praxis.integration-token deve ser configurado fora do valor de desenvolvimento.");
        }
        this.properties = properties;
        this.tenantRepository = tenantRepository;
        this.defaultTenantId = defaultTenantId;
    }

    /**
     * Resolve o tenant a partir do header Authorization. Primeiro tenta casar o hash do token com um
     * tenant configurado; se não houver, cai para o token global legado (associado ao tenant padrão).
     */
    public GupyTenantContext validateBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer obrigatorio.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        String tokenHash = sha256(token);

        return tenantRepository.findFirstByIntegrationTokenHash(tokenHash)
                .map(tenant -> new GupyTenantContext(tenant.getId(), tenant.getCompanyId()))
                .orElseGet(() -> validateLegacyToken(token));
    }

    public GupyTenantContext resolveByCompanyId(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId e obrigatorio.");
        }

        TenantEntity tenant = tenantRepository.findFirstByCompanyId(companyId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao configurada para integracao."));
        return new GupyTenantContext(tenant.getId(), tenant.getCompanyId());
    }

    private GupyTenantContext validateLegacyToken(String token) {
        if (properties.integrationToken() == null || !properties.integrationToken().equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer invalido.");
        }

        TenantEntity tenant = tenantRepository.findById(defaultTenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant padrao nao configurado."));
        return new GupyTenantContext(tenant.getId(), tenant.getCompanyId());
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
