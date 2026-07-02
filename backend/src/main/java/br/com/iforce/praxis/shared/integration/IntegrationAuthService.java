package br.com.iforce.praxis.shared.integration;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;


import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.security.NoSuchAlgorithmException;

import java.util.Base64;


/**
 * Valida tokens de integração vindo de sistemas externos.
 *
 * Quando a Gupy ou Recrutei enviam dados para a Praxis, eles incluem um token
 * na requisição para provar que são realmente eles e não alguém tentando
 * falsificar a integração. Este serviço valida esse token.
 */
@Service
public class IntegrationAuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final IntegrationTokenRepository integrationTokenRepository;

    public IntegrationAuthService(IntegrationTokenRepository integrationTokenRepository) {
        this.integrationTokenRepository = integrationTokenRepository;
    }

    /**
     * Valida um token Bearer e identifica qual empresa ele pertence.
     *
     * O cabeçalho Authorization da requisição contém "Bearer <token>".
     * Este método extrai o token, calcula seu hash, e busca qual empresa
     * possui aquele token no banco de dados.
     *
     * Isso garante que apenas a Gupy/Recrutei que temos registrado conseguem
     * enviar dados para a Praxis.
     *
     * @param authorizationHeader Valor do cabeçalho Authorization da requisição
     * @param provider Nome da plataforma (Gupy, Recrutei, etc)
     * @return ID da empresa e ID corporativo se o token for válido
     * @throws ResponseStatusException se o token é inválido ou não existe
     */
    public IntegrationEmpresaContext validateBearerToken(String authorizationHeader, String provider) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer obrigatório.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        String tokenHash = sha256(token);

        return integrationTokenRepository.findFirstByProviderAndTokenHash(provider, tokenHash)
                .map(entity -> new IntegrationEmpresaContext(
                        entity.getEmpresa().getId(),
                        entity.getEmpresa().getCompanyId(),
                        entity.getProvider()
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
