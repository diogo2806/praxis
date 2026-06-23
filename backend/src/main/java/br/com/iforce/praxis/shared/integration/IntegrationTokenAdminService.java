package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.shared.integration.dto.IntegrationTokenResponse;
import br.com.iforce.praxis.shared.integration.dto.RotateIntegrationTokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IntegrationTokenAdminService {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("gupy", "recrutei");
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final CurrentTenantService currentTenantService;
    private final TenantRepository tenantRepository;
    private final IntegrationTokenRepository integrationTokenRepository;

    public IntegrationTokenAdminService(
            CurrentTenantService currentTenantService,
            TenantRepository tenantRepository,
            IntegrationTokenRepository integrationTokenRepository
    ) {
        this.currentTenantService = currentTenantService;
        this.tenantRepository = tenantRepository;
        this.integrationTokenRepository = integrationTokenRepository;
    }

    @Transactional(readOnly = true)
    public List<IntegrationTokenResponse> listTokens() {
        String tenantId = currentTenantService.requiredTenantId();
        Map<String, IntegrationTokenEntity> existing = integrationTokenRepository
                .findByTenantIdOrderByProviderAsc(tenantId)
                .stream()
                .collect(Collectors.toMap(IntegrationTokenEntity::getProvider, token -> token, (left, right) -> left));

        return SUPPORTED_PROVIDERS.stream()
                .sorted()
                .map(provider -> {
                    IntegrationTokenEntity token = existing.get(provider);
                    return new IntegrationTokenResponse(provider, token != null, token == null ? null : token.getCreatedAt());
                })
                .toList();
    }

    @Transactional
    public RotateIntegrationTokenResponse rotateToken(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        String tenantId = currentTenantService.requiredTenantId();
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant não encontrado."));

        String tokenValue = generateToken();
        integrationTokenRepository.deleteByTenantIdAndProvider(tenantId, normalizedProvider);
        integrationTokenRepository.flush();

        IntegrationTokenEntity entity = new IntegrationTokenEntity();
        entity.setTenant(tenant);
        entity.setProvider(normalizedProvider);
        entity.setTokenHash(sha256(tokenValue));
        entity.setCreatedAt(Instant.now());

        IntegrationTokenEntity saved = integrationTokenRepository.save(entity);
        return new RotateIntegrationTokenResponse(
                saved.getProvider(),
                true,
                saved.getCreatedAt(),
                tokenValue
        );
    }

    @Transactional
    public void revokeToken(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        String tenantId = currentTenantService.requiredTenantId();
        integrationTokenRepository.deleteByTenantIdAndProvider(tenantId, normalizedProvider);
    }

    private static String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase();
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provedor de integração não suportado.");
        }
        return normalized;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return "prx_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
