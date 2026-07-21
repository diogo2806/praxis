package br.com.iforce.praxis.team.model;

import java.util.List;
import java.util.Set;

/** Perfis operacionais disponíveis para usuários vinculados a uma empresa. */
public enum TeamProfile {

    ADMINISTRADOR(
            "Administrador",
            Set.of("EMPRESA", "TEAM_MANAGER", "PARTNER_MANAGER"),
            List.of(
                    "Gerenciar equipe e perfis",
                    "Gerenciar parceiros e especialistas",
                    "Criar, publicar e operar avaliações",
                    "Consultar resultados, integrações e cobrança"
            )
    ),
    AUTOR(
            "Autor de avaliações",
            Set.of("EMPRESA", "ASSESSMENT_EDITOR"),
            List.of(
                    "Criar e editar avaliações",
                    "Consultar o catálogo de competências",
                    "Revisar estrutura, conteúdo e pontuação"
            )
    ),
    ANALISTA(
            "Analista de resultados",
            Set.of("EMPRESA", "RESULTS_ANALYST"),
            List.of(
                    "Consultar resultados e evidências",
                    "Comparar participantes",
                    "Registrar decisão humana"
            )
    ),
    OPERADOR(
            "Operador",
            Set.of("EMPRESA", "OPERATIONS_MANAGER"),
            List.of(
                    "Criar e acompanhar participações",
                    "Acompanhar alertas e falhas operacionais",
                    "Consultar jornadas e integrações"
            )
    ),
    ESPECIALISTA(
            "Especialista parceiro",
            Set.of("PARTNER_SPECIALIST"),
            List.of(
                    "Criar e revisar rascunhos de avaliações",
                    "Consultar competências disponíveis",
                    "Enviar conteúdo para revisão da empresa"
            )
    );

    private final String displayName;
    private final Set<String> roles;
    private final List<String> permissions;

    TeamProfile(String displayName, Set<String> roles, List<String> permissions) {
        this.displayName = displayName;
        this.roles = roles;
        this.permissions = permissions;
    }

    public String displayName() {
        return displayName;
    }

    public Set<String> roles() {
        return roles;
    }

    public List<String> permissions() {
        return permissions;
    }

    public boolean assignableFromTeam() {
        return this != ESPECIALISTA;
    }

    public static TeamProfile fromRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return OPERADOR;
        }
        if (roles.contains("PARTNER_SPECIALIST")) {
            return ESPECIALISTA;
        }
        if (roles.contains("TEAM_MANAGER") || roles.contains("PARTNER_MANAGER")) {
            return ADMINISTRADOR;
        }
        if (roles.contains("ASSESSMENT_EDITOR")) {
            return AUTOR;
        }
        if (roles.contains("RESULTS_ANALYST")) {
            return ANALISTA;
        }
        if (roles.contains("OPERATIONS_MANAGER")) {
            return OPERADOR;
        }
        // Compatibilidade: usuários existentes possuíam somente EMPRESA e tinham acesso administrativo.
        if (roles.equals(Set.of("EMPRESA"))) {
            return ADMINISTRADOR;
        }
        return OPERADOR;
    }
}
