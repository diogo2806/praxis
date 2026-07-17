package br.com.iforce.praxis.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityStartupGuardTest {

    @Test
    void refusesToStartWhenSecurityDisabledInProductionEvenWithOverride() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        SecurityStartupGuard guard = new SecurityStartupGuard(false, true, environment);

        assertThatThrownBy(guard::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRAXIS_SECURITY_ENABLED=true");
    }

    @Test
    void refusesSecurityDisabledWithoutExplicitOverride() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        SecurityStartupGuard guard = new SecurityStartupGuard(false, false, environment);

        assertThatThrownBy(guard::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRAXIS_SECURITY_ALLOW_DISABLED=true");
    }

    @Test
    void refusesSecurityDisabledWithoutActiveLocalProfile() {
        MockEnvironment environment = new MockEnvironment();

        SecurityStartupGuard guard = new SecurityStartupGuard(false, true, environment);

        assertThatThrownBy(guard::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("perfil");
    }

    @Test
    void allowsSecurityDisabledOnlyWithExplicitTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        SecurityStartupGuard guard = new SecurityStartupGuard(false, true, environment);

        assertThatCode(guard::verify).doesNotThrowAnyException();
    }

    @Test
    void allowsSecurityEnabledInProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        SecurityStartupGuard guard = new SecurityStartupGuard(true, false, environment);

        assertThatCode(guard::verify).doesNotThrowAnyException();
    }
}
