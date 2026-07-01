package br.com.iforce.praxis.shared.security;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;

import org.springframework.http.HttpStatus;

import org.springframework.web.server.ResponseStatusException;


public final class EmpresaSecurity {

    private EmpresaSecurity() {
    }

    public static String requiredEmpresa() {
        String empresa = EmpresaContextHolder.get();
        if (empresa == null) {
            throw new IllegalStateException("Empresa obrigatória não foi estabelecida no contexto");
        }
        return empresa;
    }

    public static void validateEmpresaAccess(String resourceEmpresaId) {
        String currentEmpresa = requiredEmpresa();
        if (!currentEmpresa.equals(resourceEmpresaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a este item.");
        }
    }

    public static void validateEmpresaAccess(String resourceEmpresaId, String resourceId) {
        String currentEmpresa = requiredEmpresa();
        if (!currentEmpresa.equals(resourceEmpresaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado.");
        }
    }
}
