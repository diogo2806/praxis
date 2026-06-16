package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.config.PraxisProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GupyAuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final PraxisProperties properties;

    public GupyAuthService(PraxisProperties properties, @Value("${praxis.security.enabled:true}") boolean securityEnabled) {
        if (securityEnabled && (properties.integrationToken() == null
                || properties.integrationToken().isBlank()
                || "dev-company-token".equals(properties.integrationToken()))) {
            throw new IllegalStateException("praxis.integration-token deve ser configurado fora do valor de desenvolvimento.");
        }
        this.properties = properties;
    }

    public void validateBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer obrigatorio.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        if (properties.integrationToken() == null || !properties.integrationToken().equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer invalido.");
        }
    }
}
