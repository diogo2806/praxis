package br.com.iforce.praxis.auth.config;

import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;


import static org.assertj.core.api.Assertions.assertThatCode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


class SecurityStartupGuardTest {

    @Test
    void refusesToStartWhenSecurityDisabledInProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        SecurityStartupGuard guard = new SecurityStartupGuard(false, environment);

        assertThatThrownBy(guard::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod")
                .hasMessageContaining("PRAXIS_SECURITY_ENABLED=true");
    }

    @Test
    void allowsSecurityDisabledOutsideProduction() {
        MockEnvironment environment = new MockEnvironment();

        SecurityStartupGuard guard = new SecurityStartupGuard(false, environment);

        assertThatCode(guard::verify).doesNotThrowAnyException();
    }

    @Test
    void allowsSecurityEnabledInProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        SecurityStartupGuard guard = new SecurityStartupGuard(true, environment);

        assertThatCode(guard::verify).doesNotThrowAnyException();
    }
}
