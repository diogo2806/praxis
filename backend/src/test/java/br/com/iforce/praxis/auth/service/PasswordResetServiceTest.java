package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.admin.model.UserStatus;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.audit.service.AuditMetadata;

import br.com.iforce.praxis.auth.dto.ForgotPasswordRequest;

import br.com.iforce.praxis.auth.dto.ResetPasswordRequest;

import br.com.iforce.praxis.auth.dto.ResetPasswordTokenResponse;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.auth.persistence.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.List;

import java.util.Optional;

import java.util.Set;


import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyInt;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.times;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.verifyNoInteractions;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private PasswordResetEmailSender emailSender;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuditMetadata auditMetadata;

    @BeforeEach
    void setUp() {
        auditMetadata = new AuditMetadata(new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private PasswordResetService service() {
        return new PasswordResetService(
                userRepository,
                empresaRepository,
                passwordEncoder,
                auditEventService,
                auditMetadata,
                emailSender,
                "https://app.praxis.com.br",
                2
        );
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setEmpresaId("empresa123");
        user.setEmail("usuario@empresa.com");
        user.setName("João");
        user.setRoles(Set.of("EMPRESA"));
        user.setPasswordHash(passwordEncoder.encode("senhaAntiga1"));
        user.setStatus(UserStatus.ATIVO);
        return user;
    }

    private EmpresaEntity activeEmpresa(String id) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(id);
        empresa.setStatus(EmpresaStatus.ATIVO);
        return empresa;
    }

    // ------------------------------------------------------------------
    // Solicitação (forgot)
    // ------------------------------------------------------------------

    @Test
    void requestResetGeneratesTokenAndSendsEmailForActiveUser() {
        UserEntity user = activeUser();
        when(userRepository.findFirstByEmailAndEmpresaId("usuario@empresa.com", "empresa123"))
                .thenReturn(Optional.of(user));
        when(empresaRepository.findById("empresa123")).thenReturn(Optional.of(activeEmpresa("empresa123")));

        service().requestReset(new ForgotPasswordRequest("empresa123", "usuario@empresa.com"), "1.2.3.4");

        // Apenas o hash é gravado; o expirador é definido.
        assertThat(user.getPasswordResetTokenHash()).isNotBlank();
        assertThat(user.getPasswordResetExpiresAt()).isAfter(Instant.now());
        verify(userRepository).save(user);
        verify(emailSender).sendPasswordResetEmail(eq("usuario@empresa.com"), eq("João"), anyString(), anyInt());
        verify(auditEventService).appendUserEvent(
                eq("empresa123"), eq("7"), eq(AuditEventType.PASSWORD_RESET_REQUESTED), anyString(), anyString());
    }

    @Test
    void requestResetForAdminDefaultsToPlatformEmpresa() {
        UserEntity admin = activeUser();
        admin.setEmpresaId("PLATFORM");
        admin.setRoles(Set.of("ADMIN"));
        when(userRepository.findFirstByEmailAndEmpresaId("admin@praxis.com", "PLATFORM"))
                .thenReturn(Optional.of(admin));
        when(empresaRepository.findById("PLATFORM")).thenReturn(Optional.of(activeEmpresa("PLATFORM")));

        service().requestReset(new ForgotPasswordRequest(null, "admin@praxis.com"), null);

        verify(emailSender).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void requestResetIsSilentWhenUserMissing() {
        when(userRepository.findFirstByEmailAndEmpresaId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        service().requestReset(new ForgotPasswordRequest("empresa123", "ghost@empresa.com"), "1.2.3.4");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailSender, auditEventService);
    }

    @Test
    void requestResetIsSilentForNonActiveUser() {
        UserEntity invited = activeUser();
        invited.setStatus(UserStatus.CONVIDADO);
        when(userRepository.findFirstByEmailAndEmpresaId(anyString(), anyString()))
                .thenReturn(Optional.of(invited));

        service().requestReset(new ForgotPasswordRequest("empresa123", "usuario@empresa.com"), null);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailSender);
    }

    // ------------------------------------------------------------------
    // Validação do token
    // ------------------------------------------------------------------

    @Test
    void validateTokenReturnsDetailsForValidToken() {
        String token = "tok_valido";
        UserEntity user = activeUser();
        user.setPasswordResetTokenHash(passwordEncoder.encode(token));
        user.setPasswordResetExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        when(userRepository.findByPasswordResetTokenHashIsNotNull()).thenReturn(List.of(user));

        ResetPasswordTokenResponse response = service().validateToken(token);

        assertThat(response.valid()).isTrue();
        assertThat(response.userName()).isEqualTo("João");
    }

    @Test
    void validateTokenReturns404WhenUnknown() {
        when(userRepository.findByPasswordResetTokenHashIsNotNull()).thenReturn(List.of());

        assertThatThrownBy(() -> service().validateToken("desconhecido"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }

    @Test
    void validateTokenReturns410WhenExpired() {
        String token = "tok_expirado";
        UserEntity user = activeUser();
        user.setPasswordResetTokenHash(passwordEncoder.encode(token));
        user.setPasswordResetExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(userRepository.findByPasswordResetTokenHashIsNotNull()).thenReturn(List.of(user));

        assertThatThrownBy(() -> service().validateToken(token))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.GONE);
    }

    // ------------------------------------------------------------------
    // Confirmação (reset)
    // ------------------------------------------------------------------

    @Test
    void confirmResetChangesPasswordAndInvalidatesToken() {
        String token = "tok_ok";
        UserEntity user = activeUser();
        user.setPasswordResetTokenHash(passwordEncoder.encode(token));
        user.setPasswordResetExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        when(userRepository.findByPasswordResetTokenHashIsNotNull()).thenReturn(List.of(user));
        when(empresaRepository.findById("empresa123")).thenReturn(Optional.of(activeEmpresa("empresa123")));

        service().confirmReset(new ResetPasswordRequest(token, "novaSenha123", "novaSenha123"), "1.2.3.4");

        assertThat(passwordEncoder.matches("novaSenha123", user.getPasswordHash())).isTrue();
        assertThat(user.getPasswordResetTokenHash()).isNull();
        assertThat(user.getPasswordResetExpiresAt()).isNull();
        assertThat(user.getLastPasswordResetAt()).isNotNull();
        verify(auditEventService).appendUserEvent(
                eq("empresa123"), eq("7"), eq(AuditEventType.PASSWORD_RESET_COMPLETED), anyString(), anyString());
    }

    @Test
    void confirmResetRejectsMismatchedConfirmation() {
        assertThatThrownBy(() -> service().confirmReset(
                new ResetPasswordRequest("tok", "novaSenha123", "outraSenha123"), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository);
    }

    @Test
    void confirmResetRejectsSameAsCurrentPassword() {
        String token = "tok_ok";
        UserEntity user = activeUser();
        user.setPasswordResetTokenHash(passwordEncoder.encode(token));
        user.setPasswordResetExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        when(userRepository.findByPasswordResetTokenHashIsNotNull()).thenReturn(List.of(user));
        when(empresaRepository.findById("empresa123")).thenReturn(Optional.of(activeEmpresa("empresa123")));

        assertThatThrownBy(() -> service().confirmReset(
                new ResetPasswordRequest(token, "senhaAntiga1", "senhaAntiga1"), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmResetRejectsExpiredTokenAndClearsState() {
        String token = "tok_expirado";
        UserEntity user = activeUser();
        user.setPasswordResetTokenHash(passwordEncoder.encode(token));
        user.setPasswordResetExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(userRepository.findByPasswordResetTokenHashIsNotNull()).thenReturn(List.of(user));

        assertThatThrownBy(() -> service().confirmReset(
                new ResetPasswordRequest(token, "novaSenha123", "novaSenha123"), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.GONE);
        assertThat(user.getPasswordResetTokenHash()).isNull();
    }

    @Test
    void confirmResetRejectsReusedToken() {
        // Após o uso o token é removido, então uma nova busca não encontra usuário.
        when(userRepository.findByPasswordResetTokenHashIsNotNull()).thenReturn(List.of());

        assertThatThrownBy(() -> service().confirmReset(
                new ResetPasswordRequest("tok_ja_usado", "novaSenha123", "novaSenha123"), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    @Test
    void requestResetNeverIncludesRawTokenInAuditMetadata() {
        UserEntity user = activeUser();
        when(userRepository.findFirstByEmailAndEmpresaId(anyString(), anyString())).thenReturn(Optional.of(user));
        when(empresaRepository.findById("empresa123")).thenReturn(Optional.of(activeEmpresa("empresa123")));

        service().requestReset(new ForgotPasswordRequest("empresa123", "usuario@empresa.com"), "1.2.3.4");

        ArgumentCaptor<String> metadata = ArgumentCaptor.forClass(String.class);
        verify(auditEventService, times(1)).appendUserEvent(
                anyString(), anyString(), any(), anyString(), metadata.capture());
        assertThat(metadata.getValue()).doesNotContain(user.getPasswordResetTokenHash());
        assertThat(metadata.getValue().toLowerCase()).doesNotContain("password");
    }
}
