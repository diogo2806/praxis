package br.com.iforce.praxis.auth.config;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.env.Environment;

import org.springframework.stereotype.Component;


import java.util.Arrays;


/**
 * Trava de inicialização que impede subir a aplicação de forma insegura.
 *
 * <p>Na visão do processo: rodar em <strong>produção</strong> (perfil {@code prod}
 * ativo) com a segurança interna desligada ({@code praxis.security.enabled=false})
 * liberaria todas as rotas internas e usaria o empresa padrão, quebrando o
 * isolamento entre clientes (multi-tenant). Para evitar esse erro operacional, a
 * aplicação <strong>recusa iniciar</strong> nessa combinação. Fora de produção, a
 * combinação é permitida (é o modo de desenvolvimento/teste), mas um alerta é
 * registrado para não passar despercebido.</p>
 */
@Component
public class SecurityStartupGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityStartupGuard.class);
    private static final String PRODUCTION_PROFILE = "prod";

    private final boolean securityEnabled;
    private final Environment environment;

    public SecurityStartupGuard(
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            Environment environment
    ) {
        this.securityEnabled = securityEnabled;
        this.environment = environment;
    }

    /**
     * Verifica a combinação de segurança e perfil ativo ao subir o contexto.
     *
     * <p>Se a segurança estiver ligada, nada a fazer. Se estiver desligada e o
     * perfil de produção estiver ativo, interrompe a inicialização; caso
     * contrário, apenas alerta.</p>
     */
    @PostConstruct
    void verify() {
        if (securityEnabled) {
            return;
        }
        if (isProductionProfileActive()) {
            throw new IllegalStateException(
                    "Seguranca interna desligada (praxis.security.enabled=false) com o perfil 'prod' ativo. "
                            + "Isso liberaria todas as rotas internas e quebraria o isolamento multi-tenant. "
                            + "Defina PRAXIS_SECURITY_ENABLED=true para operar em producao.");
        }
        LOGGER.warn("ATENCAO: seguranca interna DESLIGADA (praxis.security.enabled=false). "
                + "Todas as rotas internas ficam liberadas e o empresa padrao e usado. "
                + "Use apenas em desenvolvimento/teste, NUNCA em producao.");
    }

    /** Indica se o perfil de produção está entre os perfis ativos. Uso interno. */
    private boolean isProductionProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(PRODUCTION_PROFILE::equalsIgnoreCase);
    }
}
