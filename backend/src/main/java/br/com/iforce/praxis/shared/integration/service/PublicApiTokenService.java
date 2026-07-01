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


/**
 * Geração e rotação do token de API pública (formato {@code prx_live_…}) que o
 * cliente usa para CONSUMIR a API da Práxis (consultar resultados etc.), em
 * complemento ao webhook que ele RECEBE. Guarda apenas o hash do token; o valor
 * completo só aparece na geração/rotação.
 */
@Service
public class PublicApiTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOKEN_PREFIX = "prx_live_";

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

    /** Gera (ou rotaciona) o token de API pública do cliente. */
    @Transactional
    public PublicApiTokenResponse generateToken() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        EmpresaIntegrationEntity entity = findOrCreate(empresaId);

        String token = TOKEN_PREFIX + randomToken();
        entity.setCredentialsHash(sha256(token));
        entity.setTokenPreview(preview(token));
        entity.setUpdatedAt(Instant.now());
        empresaIntegrationRepository.save(entity);

        return new PublicApiTokenResponse(token, preview(token));
    }

    /** Revoga o token de API pública, sem desligar o webhook. */
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
                    EmpresaIntegrationEntity created = new EmpresaIntegrationEntity();
                    created.setEmpresa(empresa);
                    created.setProvider(IntegrationProvider.CUSTOM_API);
                    created.setType(IntegrationType.API);
                    created.setStatus(IntegrationStatus.CONECTADA);
                    created.setConfiguredAt(Instant.now());
                    created.setCreatedAt(Instant.now());
                    created.setUpdatedAt(Instant.now());
                    return created;
                });
    }

    private static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String preview(String token) {
        String suffix = token.length() <= 4 ? token : token.substring(token.length() - 4);
        return TOKEN_PREFIX + "••••" + suffix;
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
