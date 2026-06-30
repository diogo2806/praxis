package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.admin.model.UserStatus;

import br.com.iforce.praxis.auth.dto.LoginRequest;

import br.com.iforce.praxis.auth.dto.LoginResponse;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.auth.persistence.repository.UserRepository;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.server.ResponseStatusException;


import java.util.Optional;

import java.util.Set;


import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.lenient;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService service() {
        return new AuthService(userRepository, empresaRepository, passwordEncoder, jwtService);
    }

    private UserEntity user(UserStatus status) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmpresaId("tnt_1");
        user.setEmail("u@acme.com");
        user.setName("Usuário");
        user.setRoles(Set.of("EMPRESA"));
        user.setPasswordHash("hash");
        user.setStatus(status);
        return user;
    }

    private EmpresaEntity activeEmpresa() {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("tnt_1");
        empresa.setStatus(EmpresaStatus.ATIVO);
        return empresa;
    }

    private LoginRequest request() {
        return new LoginRequest("tnt_1", "u@acme.com", "senha");
    }

    @Test
    void loginSucceedsForActiveUser() {
        when(userRepository.findFirstByEmailAndEmpresaId("u@acme.com", "tnt_1"))
                .thenReturn(Optional.of(user(UserStatus.ATIVO)));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(empresaRepository.findById("tnt_1")).thenReturn(Optional.of(activeEmpresa()));
        when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("jwt");

        LoginResponse response = service().login(request());

        assertThat(response.token()).isEqualTo("jwt");
    }

    @Test
    void loginRejectsInvitedUserEvenWithValidPassword() {
        when(userRepository.findFirstByEmailAndEmpresaId("u@acme.com", "tnt_1"))
                .thenReturn(Optional.of(user(UserStatus.CONVIDADO)));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(empresaRepository.findById("tnt_1")).thenReturn(Optional.of(activeEmpresa()));
        lenient().when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("jwt");

        assertThatThrownBy(() -> service().login(request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    @Test
    void loginRejectsBlockedUser() {
        when(userRepository.findFirstByEmailAndEmpresaId("u@acme.com", "tnt_1"))
                .thenReturn(Optional.of(user(UserStatus.BLOQUEADO)));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(empresaRepository.findById("tnt_1")).thenReturn(Optional.of(activeEmpresa()));

        assertThatThrownBy(() -> service().login(request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }
}
