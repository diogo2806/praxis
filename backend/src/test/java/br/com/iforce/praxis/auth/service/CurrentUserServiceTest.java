package br.com.iforce.praxis.auth.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedStringPrincipalWhenSecurityIsEnabled() {
        CurrentUserService service = new CurrentUserService(true, "dev-user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-42", null, List.of())
        );

        assertThat(service.requiredUserId()).isEqualTo("user-42");
    }

    @Test
    void returnsConfiguredDefaultUserWhenSecurityIsDisabled() {
        CurrentUserService service = new CurrentUserService(false, "local-user");

        assertThat(service.requiredUserId()).isEqualTo("local-user");
    }

    @Test
    void rejectsMissingAuthenticationWhenSecurityIsEnabled() {
        CurrentUserService service = new CurrentUserService(true, "dev-user");

        assertThatThrownBy(service::requiredUserId)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Sessão inválida ou expirada");
    }

    @Test
    void rejectsBlankPrincipalWhenSecurityIsEnabled() {
        CurrentUserService service = new CurrentUserService(true, "dev-user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("   ", null, List.of())
        );

        assertThatThrownBy(service::requiredUserId)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Sessão inválida ou expirada");
    }
}
