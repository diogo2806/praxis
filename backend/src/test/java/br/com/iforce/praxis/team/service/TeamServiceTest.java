package br.com.iforce.praxis.team.service;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.team.dto.InviteTeamUserRequest;
import br.com.iforce.praxis.team.dto.InviteTeamUserResponse;
import br.com.iforce.praxis.team.dto.TeamUserResponse;
import br.com.iforce.praxis.team.model.TeamProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TeamServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuditEventService auditEventService = mock(AuditEventService.class);
    private final AuditMetadata auditMetadata = new AuditMetadata(new ObjectMapper());
    private final TeamService service = new TeamService(
            userRepository,
            passwordEncoder,
            auditEventService,
            auditMetadata,
            "https://praxis.example",
            24
    );

    @Test
    void listsOnlyUsersFromRequestedEmpresaInRepositoryOrder() {
        UserEntity first = user(10L, "Ana", "ana@example.com", UserStatus.ATIVO);
        UserEntity second = user(11L, "Bruno", "bruno@example.com", UserStatus.CONVIDADO);
        when(userRepository.findByEmpresaIdOrderByCreatedAtAsc("empresa-1"))
                .thenReturn(List.of(first, second));

        List<TeamUserResponse> response = service.listUsers("empresa-1");

        assertThat(response).extracting(TeamUserResponse::id).containsExactly(10L, 11L);
        assertThat(response).extracting(TeamUserResponse::email)
                .containsExactly("ana@example.com", "bruno@example.com");
        verify(userRepository).findByEmpresaIdOrderByCreatedAtAsc("empresa-1");
    }

    @Test
    void rejectsDuplicateEmailInsideSameEmpresa() {
        when(userRepository.existsByEmpresaIdAndEmail("empresa-1", "ana@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.inviteUser(
                "actor-1",
                "empresa-1",
                new InviteTeamUserRequest("Ana", "ana@example.com")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Já existe um usuário");

        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(auditEventService);
    }

    @Test
    void createsInvitationWithSelectedProfileExpirationAndAuditing() {
        when(userRepository.existsByEmpresaIdAndEmail("empresa-1", "ana@example.com"))
                .thenReturn(false);
        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "encoded:" + invocation.getArgument(0, String.class));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0, UserEntity.class);
            saved.setId(99L);
            return saved;
        });

        InviteTeamUserResponse response = service.inviteUser(
                "actor-1",
                "empresa-1",
                new InviteTeamUserRequest("  Ana Silva  ", "ana@example.com", TeamProfile.ANALISTA)
        );

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity saved = userCaptor.getValue();
        String token = response.inviteUrl().substring("https://praxis.example/convite/".length());

        assertThat(saved.getEmpresaId()).isEqualTo("empresa-1");
        assertThat(saved.getName()).isEqualTo("Ana Silva");
        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getRoles()).containsExactlyInAnyOrder("EMPRESA", "RESULTS_ANALYST");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.CONVIDADO);
        assertThat(saved.getPasswordHash()).startsWith("encoded:");
        assertThat(saved.getInviteTokenHash()).isEqualTo("encoded:" + token);
        assertThat(saved.getInvitedAt()).isNotNull();
        assertThat(saved.getInviteExpiresAt()).isAfter(saved.getInvitedAt());
        assertThat(Duration.between(saved.getInvitedAt(), saved.getInviteExpiresAt()).toHours())
                .isEqualTo(24);

        assertThat(response.user().id()).isEqualTo(99L);
        assertThat(response.user().profile()).isEqualTo(TeamProfile.ANALISTA);
        assertThat(response.user().permissions()).contains("Consultar resultados e evidências");
        assertThat(response.user().status()).isEqualTo(UserStatus.CONVIDADO);
        assertThat(response.inviteUrl()).startsWith("https://praxis.example/convite/");

        verify(auditEventService).auditAdminAction(
                eq("actor-1"),
                eq("empresa-1"),
                eq(AuditEventType.TEAM_USER_INVITED),
                contains("ana@example.com"),
                anyString()
        );
    }

    @Test
    void rejectsResendForUserThatIsNotInvited() {
        UserEntity activeUser = user(10L, "Ana", "ana@example.com", UserStatus.ATIVO);
        when(userRepository.findByIdAndEmpresaId(10L, "empresa-1"))
                .thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> service.resendInvite("actor-1", "empresa-1", 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CONVIDADO");

        verifyNoInteractions(auditEventService);
    }

    @Test
    void rejectsSelfBlockingBeforeLoadingUser() {
        assertThatThrownBy(() -> service.blockUser("10", "empresa-1", 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("próprio usuário");

        verify(userRepository, never()).findByIdAndEmpresaId(any(Long.class), anyString());
        verifyNoInteractions(auditEventService);
    }

    @Test
    void blocksAndUnblocksUserWithinEmpresaWithAuditTrail() {
        UserEntity user = user(10L, "Ana", "ana@example.com", UserStatus.ATIVO);
        when(userRepository.findByIdAndEmpresaId(10L, "empresa-1"))
                .thenReturn(Optional.of(user));

        TeamUserResponse blocked = service.blockUser("actor-1", "empresa-1", 10L);
        TeamUserResponse unblocked = service.unblockUser("actor-1", "empresa-1", 10L);

        assertThat(blocked.status()).isEqualTo(UserStatus.BLOQUEADO);
        assertThat(unblocked.status()).isEqualTo(UserStatus.ATIVO);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ATIVO);

        verify(auditEventService).auditAdminAction(
                eq("actor-1"),
                eq("empresa-1"),
                eq(AuditEventType.TEAM_USER_BLOCKED),
                contains("ana@example.com"),
                anyString()
        );
        verify(auditEventService).auditAdminAction(
                eq("actor-1"),
                eq("empresa-1"),
                eq(AuditEventType.TEAM_USER_UNBLOCKED),
                contains("ana@example.com"),
                anyString()
        );
    }

    @Test
    void returnsNotFoundWhenUserDoesNotBelongToEmpresa() {
        when(userRepository.findByIdAndEmpresaId(10L, "empresa-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.blockUser("actor-1", "empresa-1", 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Usuário não encontrado");

        verifyNoInteractions(auditEventService);
    }

    private UserEntity user(Long id, String name, String email, UserStatus status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmpresaId("empresa-1");
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRoles(Set.of("EMPRESA", "OPERATIONS_MANAGER"));
        user.setStatus(status);
        user.setCreatedAt(Instant.parse("2026-07-13T10:00:00Z").plusSeconds(id));
        return user;
    }
}
