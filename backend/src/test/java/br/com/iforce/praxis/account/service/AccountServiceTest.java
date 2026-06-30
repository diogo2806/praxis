package br.com.iforce.praxis.account.service;

import br.com.iforce.praxis.account.dto.AccountResponse;

import br.com.iforce.praxis.account.dto.ChangePasswordRequest;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;

import br.com.iforce.praxis.auth.persistence.repository.UserRepository;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.auth.service.CurrentUserService;

import org.junit.jupiter.api.Test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.server.ResponseStatusException;


import java.util.LinkedHashSet;

import java.util.Optional;

import java.util.Set;


import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.when;


class AccountServiceTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final CurrentEmpresaService currentEmpresaService = mock(CurrentEmpresaService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AccountService service = new AccountService(
            currentUserService,
            currentEmpresaService,
            userRepository,
            passwordEncoder
    );

    @Test
    void currentAccountReturnsAuthenticatedUserInsideEmpresa() {
        UserEntity user = user("old-password");
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(currentUserService.requiredUserId()).thenReturn("42");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        AccountResponse response = service.currentAccount();

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.empresaId()).isEqualTo("empresa-1");
        assertThat(response.email()).isEqualTo("ana@example.com");
        assertThat(response.roles()).containsExactly("EMPRESA");
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        UserEntity user = user("old-password");
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(currentUserService.requiredUserId()).thenReturn("42");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(
                new ChangePasswordRequest("wrong-password", "new-password")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Senha atual incorreta");
    }

    @Test
    void changePasswordStoresNewHash() {
        UserEntity user = user("old-password");
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(currentUserService.requiredUserId()).thenReturn("42");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.changePassword(new ChangePasswordRequest("old-password", "new-password"));

        assertThat(passwordEncoder.matches("old-password", user.getPasswordHash())).isFalse();
        assertThat(passwordEncoder.matches("new-password", user.getPasswordHash())).isTrue();
    }

    private UserEntity user(String password) {
        UserEntity user = new UserEntity();
        user.setId(42L);
        user.setEmpresaId("empresa-1");
        user.setEmail("ana@example.com");
        user.setName("Ana");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(new LinkedHashSet<>(Set.of("EMPRESA")));
        return user;
    }
}
