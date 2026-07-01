package br.com.iforce.praxis.auth.service;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;


/**
 * Identifica qual usuário está logado no momento.
 *
 * Quando uma ação importante acontece (como a avaliação de um candidato),
 * o sistema precisa saber quem a realizou para rastreabilidade. Este serviço
 * extrai o ID do usuário autenticado do token JWT (em produção) ou usa um
 * padrão (em desenvolvimento).
 *
 * Isso permite auditar quem fez o quê, essencial para conformidade regulatória.
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

    /**
     * Obtém o ID do usuário autenticado.
     *
     * Busca na sessão de segurança o ID do usuário que está usando o sistema.
     * Se não estiver autenticado, lança exceção.
     *
     * Em desenvolvimento/testes (quando segurança está desativada), retorna
     * um ID padrão para permitir testes sem autenticação.
     *
     * @return ID único do usuário autenticado
     * @throws ResponseStatusException se o usuário não está autenticado ou a sessão expirou
     */
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
