package br.com.iforce.praxis.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolve o usuário autenticado (o {@code subject} do JWT é o id do usuário) para os fluxos
 * administrativos que precisam registrar <em>quem</em> realizou uma ação — em especial a decisão
 * humana sobre um candidato (REQ-L1). Quando {@code praxis.security.enabled=false} (perfis de
 * desenvolvimento e testes), devolve um identificador padrão para manter os endpoints utilizáveis
 * sem JWT.
 */
@Service
public class CurrentUserService {

    private final boolean securityEnabled;
    private final String defaultUserId;

    public CurrentUserService(
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.default-user-id:dev-user}") String defaultUserId
    ) {
        this.securityEnabled = securityEnabled;
        this.defaultUserId = defaultUserId;
    }

    public String requiredUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (principal instanceof String userId && !userId.isBlank()) {
            return userId;
        }

        if (!securityEnabled) {
            return defaultUserId;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada.");
    }
}
