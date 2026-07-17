package br.com.iforce.praxis.auth.config;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * Trava de inicialização que impede subir a aplicação de forma insegura.
 *
 * <p>Desligar a segurança libera rotas internas e ativa comportamentos de
 * conveniência que só são aceitáveis em desenvolvimento e testes. Por isso,
 * esse modo exige duas autorizações explícitas: a propriedade
 * {@code praxis.security.enabled=false}, a propriedade
 * {@code praxis.security.allow-disabled=true} e um perfil local conhecido.</p>
 */
@Component
public class SecurityStartupGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityStartupGuard.class);
    private static final Set<String> ALLOWED_DISABLED_PROFILES = Set.of("dev", "local", "test");

    private final boolean securityEnabled;
    private final boolean allowDisabled;
    private final Environment environment;

    public SecurityStartupGuard(
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.security.allow-disabled:false}") boolean allowDisabled,
            Environment environment
    ) {
        this.securityEnabled = securityEnabled;
        this.allowDisabled = allowDisabled;
        this.environment = environment;
    }

    /**
     * Recusa qualquer inicialização com segurança desligada que não tenha sido
     * explicitamente autorizada em um perfil exclusivamente local.
     */
    @PostConstruct
    void verify() {
        if (securityEnabled) {
            return;
        }

        String[] activeProfiles = environment.getActiveProfiles();
        boolean onlyLocalProfiles = activeProfiles.length > 0
                && Arrays.stream(activeProfiles)
                .map(String::toLowerCase)
                .allMatch(ALLOWED_DISABLED_PROFILES::contains);

        if (!allowDisabled || !onlyLocalProfiles) {
            throw new IllegalStateException(
                    "Seguranca interna desligada sem autorizacao local segura. "
                            + "Para desenvolvimento ou testes, ative exclusivamente um dos perfis "
                            + ALLOWED_DISABLED_PROFILES
                            + " e defina PRAXIS_SECURITY_ALLOW_DISABLED=true. "
                            + "Em qualquer outro ambiente, mantenha PRAXIS_SECURITY_ENABLED=true."
            );
        }

        LOGGER.warn(
                "ATENCAO: seguranca interna DESLIGADA com autorizacao explicita no(s) perfil(is) {}. "
                        + "Use apenas em desenvolvimento/teste, NUNCA em ambiente publicado.",
                Arrays.toString(activeProfiles)
        );
    }
}
