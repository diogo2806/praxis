package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.shared.security.Sha256;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Valida tokens de integração vindos de sistemas externos.
 *
 * <p>Quando a Gupy ou Recrutei enviam dados para a Praxis, eles incluem um token
 * na requisição para provar que são realmente eles e não alguém tentando
 * falsificar a integração. Este serviço valida esse token.</p>
 */
@Service
public class IntegrationAuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final IntegrationTokenRepository integrationTokenRepository;

    public IntegrationAuthService(IntegrationTokenRepository integrationTokenRepository) {
        this.integrationTokenRepository = integrationTokenRepository;
    }

    /**
     * Valida um token Bearer e identifica a empresa à qual ele pertence.
     *
     * @param authorizationHeader valor do cabeçalho Authorization
     * @param provider nome da plataforma externa
     * @return contexto da empresa quando o token é válido
     */
    public IntegrationEmpresaContext validateBearerToken(String authorizationHeader, String provider) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer obrigatório.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        String tokenHash = Sha256.base64Url(token);

        return integrationTokenRepository.findFirstByProviderAndTokenHash(provider, tokenHash)
                .map(entity -> new IntegrationEmpresaContext(
                        entity.getEmpresa().getId(),
                        resolveCompanyId(entity),
                        entity.getProvider(),
                        entity.getPartnerClientId()
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer inválido."));
    }

    private String resolveCompanyId(IntegrationTokenEntity entity) {
        if (entity.getPartnerClientId() != null && entity.getClientCompanyId() != null) {
            return entity.getClientCompanyId();
        }
        return entity.getEmpresa().getCompanyId();
    }
}
