package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.config.PraxisProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GupyAuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final PraxisProperties properties;

    public GupyAuthService(PraxisProperties properties) {
        this.properties = properties;
    }

    public void validateBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer obrigatorio.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        if (!properties.integrationToken().equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Bearer invalido.");
        }
    }
}
