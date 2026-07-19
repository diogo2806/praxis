package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.shared.integration.dto.IntegrationTokenResponse;
import br.com.iforce.praxis.shared.integration.dto.RotateIntegrationTokenResponse;
import br.com.iforce.praxis.shared.security.SecureTokens;
import br.com.iforce.praxis.shared.security.Sha256;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persistência interna dos tokens usados para autenticar integrações externas.
 *
 * <p>As mutações exigem uma transação já aberta pelo caso de uso da Central de
 * Integrações. Isso impede que a credencial real seja alterada sem que o estado
 * operacional correspondente em {@code empresa_integrations} participe da mesma
 * unidade atômica.</p>
 */
@Service
public class IntegrationTokenAdminService {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("custom_api", "gupy", "recrutei");
    private static final int TOKEN_BYTES = 32;
    private static final String TOKEN_PREFIX = "prx_";

    private final CurrentEmpresaService currentEmpresaService;
    private final EmpresaRepository empresaRepository;
    private final IntegrationTokenRepository integrationTokenRepository;

    public IntegrationTokenAdminService(
            CurrentEmpresaService currentEmpresaService,
            EmpresaRepository empresaRepository,
            IntegrationTokenRepository integrationTokenRepository
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.empresaRepository = empresaRepository;
        this.integrationTokenRepository = integrationTokenRepository;
    }

    @Transactional(readOnly = true)
    public List<IntegrationTokenResponse> listTokens() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        Map<String, IntegrationTokenEntity> existing = integrationTokenRepository
                .findByEmpresaIdOrderByProviderAsc(empresaId)
                .stream()
                .collect(Collectors.toMap(
                        IntegrationTokenEntity::getProvider,
                        token -> token,
                        (left, right) -> left
                ));

        return SUPPORTED_PROVIDERS.stream()
                .sorted()
                .map(provider -> {
                    IntegrationTokenEntity token = existing.get(provider);
                    return new IntegrationTokenResponse(
                            provider,
                            token != null,
                            token == null ? null : token.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public RotateIntegrationTokenResponse rotateToken(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        String empresaId = currentEmpresaService.requiredEmpresaId();
        EmpresaEntity empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Não encontramos os dados da sua empresa."
                ));

        String tokenValue = SecureTokens.prefixed(TOKEN_PREFIX, TOKEN_BYTES);
        integrationTokenRepository.deleteByEmpresaIdAndProvider(empresaId, normalizedProvider);
        integrationTokenRepository.flush();

        IntegrationTokenEntity entity = new IntegrationTokenEntity();
        entity.setEmpresa(empresa);
        entity.setProvider(normalizedProvider);
        entity.setTokenHash(Sha256.base64Url(tokenValue));
        entity.setCreatedAt(Instant.now());

        IntegrationTokenEntity saved = integrationTokenRepository.save(entity);
        return new RotateIntegrationTokenResponse(
                saved.getProvider(),
                true,
                saved.getCreatedAt(),
                tokenValue
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeToken(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        String empresaId = currentEmpresaService.requiredEmpresaId();
        integrationTokenRepository.deleteByEmpresaIdAndProvider(empresaId, normalizedProvider);
    }

    private static String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase();
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provedor de integração não suportado.");
        }
        return normalized;
    }
}
