package br.com.iforce.praxis.shared.integration.service;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.shared.integration.dto.PublicApiTokenResponse;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.model.IntegrationType;
import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;
import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;
import br.com.iforce.praxis.shared.security.SecureTokens;
import br.com.iforce.praxis.shared.security.Sha256;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Cuida do token que permite ao cliente consumir a API pública da Práxis.
 * A chave completa aparece somente na geração ou rotação; o banco mantém apenas
 * seu hash e uma prévia mascarada.
 */
@Service
public class PublicApiTokenService {

    private static final String TOKEN_PREFIX = "prx_live_";
    private static final int TOKEN_BYTES = 24;

    private final EmpresaIntegrationRepository empresaIntegrationRepository;
    private final EmpresaRepository empresaRepository;
    private final CurrentEmpresaService currentEmpresaService;

    public PublicApiTokenService(
            EmpresaIntegrationRepository empresaIntegrationRepository,
            EmpresaRepository empresaRepository,
            CurrentEmpresaService currentEmpresaService
    ) {
        this.empresaIntegrationRepository = empresaIntegrationRepository;
        this.empresaRepository = empresaRepository;
        this.currentEmpresaService = currentEmpresaService;
    }

    @Transactional
    public PublicApiTokenResponse generateToken() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        EmpresaIntegrationEntity entity = findOrCreate(empresaId);

        String token = SecureTokens.prefixed(TOKEN_PREFIX, TOKEN_BYTES);
        String tokenPreview = preview(token);
        entity.setCredentialsHash(Sha256.base64Url(token));
        entity.setTokenPreview(tokenPreview);
        entity.setUpdatedAt(Instant.now());
        empresaIntegrationRepository.save(entity);

        return new PublicApiTokenResponse(token, tokenPreview);
    }

    @Transactional
    public void revokeToken() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .ifPresent(entity -> {
                    entity.setCredentialsHash(null);
                    entity.setTokenPreview(null);
                    entity.setUpdatedAt(Instant.now());
                    empresaIntegrationRepository.save(entity);
                });
    }

    private EmpresaIntegrationEntity findOrCreate(String empresaId) {
        return empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .orElseGet(() -> {
                    EmpresaEntity empresa = empresaRepository.findById(empresaId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
                    Instant now = Instant.now();
                    EmpresaIntegrationEntity created = new EmpresaIntegrationEntity();
                    created.setEmpresa(empresa);
                    created.setProvider(IntegrationProvider.CUSTOM_API);
                    created.setType(IntegrationType.API);
                    created.setStatus(IntegrationStatus.CONECTADA);
                    created.setConfiguredAt(now);
                    created.setCreatedAt(now);
                    created.setUpdatedAt(now);
                    return created;
                });
    }

    private static String preview(String token) {
        String suffix = token.length() <= 4 ? token : token.substring(token.length() - 4);
        return TOKEN_PREFIX + "••••" + suffix;
    }
}
