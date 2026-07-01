package br.com.iforce.praxis.auth.service;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;


/**
 * Obtém qual empresa o usuário autenticado está usando no momento.
 *
 * O sistema é multi-empresa: cada empresa tem dados isolados. Este serviço
 * descobre qual empresa o usuário logado representa, extraindo a informação
 * do token JWT (em produção) ou de uma configuração padrão (em desenvolvimento).
 *
 * Isso garante que um usuário nunca consegue acessar dados de outra empresa,
 * mesmo que tente manipular requisições.
 */
@Service
public class CurrentEmpresaService {

    private final boolean securityEnabled;
    private final String defaultEmpresaId;

    public CurrentEmpresaService(
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.default-empresa-id:empresa-1}") String defaultEmpresaId
    ) {
        this.securityEnabled = securityEnabled;
        this.defaultEmpresaId = defaultEmpresaId;
    }

    /**
     * Obtém o ID da empresa do usuário autenticado.
     *
     * Busca a informação da empresa na sessão de segurança atual. Se o usuário
     * não estiver autenticado ou a informação estiver faltando, lança uma exceção.
     *
     * Em ambiente de desenvolvimento/testes (quando segurança está desativada),
     * retorna a empresa padrão configurada para permitir testes sem autenticação.
     *
     * @return ID da empresa do usuário autenticado
     * @throws ResponseStatusException se o usuário não está autenticado ou a sessão expirou
     */
    public String requiredEmpresaId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object details = authentication == null ? null : authentication.getDetails();

        if (details instanceof AuthenticatedEmpresa authenticatedEmpresa
                && authenticatedEmpresa.empresaId() != null
                && !authenticatedEmpresa.empresaId().isBlank()) {
            return authenticatedEmpresa.empresaId();
        }

        if (details instanceof String empresaId && !empresaId.isBlank()) {
            return empresaId;
        }

        if (!securityEnabled) {
            return defaultEmpresaId;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada.");
    }

    /** Guarda qual empresa (empresa) está vinculada à sessão autenticada. */
    public record AuthenticatedEmpresa(String empresaId) {
    }
}
