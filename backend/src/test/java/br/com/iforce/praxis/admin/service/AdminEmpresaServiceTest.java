package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.AdminUserResponse;

import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminRequest;

import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminResponse;

import br.com.iforce.praxis.admin.dto.InviteUserAdminRequest;

import br.com.iforce.praxis.admin.dto.ReactivateEmpresaAdminRequest;

import br.com.iforce.praxis.admin.dto.UpdateEmpresaAdminRequest;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.admin.model.UserStatus;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.audit.service.AuditMetadata;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.auth.persistence.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.server.ResponseStatusException;


import java.util.List;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.lenient;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AdminEmpresaServiceTest {

    private static final String ACTOR = "admin-1";

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private AdminUsageService adminUsageService;

    private AdminEmpresaService service;

    @BeforeEach
    void setUp() {
        service = new AdminEmpresaService(
                empresaRepository,
                userRepository,
                passwordEncoder,
                auditEventService,
                new AuditMetadata(new ObjectMapper()),
                adminUsageService,
                "http://localhost:8080",
                168,
                30
        );
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        lenient().when(adminUsageService.countCompletedInPeriod(anyString(), any(), any())).thenReturn(0L);
        lenient().when(empresaRepository.existsById(anyString())).thenReturn(false);
        lenient().when(empresaRepository.findFirstByCompanyId(anyString())).thenReturn(Optional.empty());
        lenient().when(empresaRepository.save(any(EmpresaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CreateEmpresaAdminRequest createRequest(boolean sendInvite, EmpresaStatus initial) {
        return new CreateEmpresaAdminRequest(
                "Acme Saúde", "Acme", "Acme S.A.", "12345678000199",
                "contato@acme.com", "+55 11 99999-0000", "https://acme.com",
                true, null, CommercialPlanType.PROFISSIONAL, "Condição padrão",
                initial, "Maria", "maria@acme.com", sendInvite);
    }

    @Test
    void createProvisionsEmpresaAndEmpresaUserWithInvite() {
        when(userRepository.findByEmpresaIdOrderByCreatedAtAsc(anyString())).thenReturn(List.of());

        CreateEmpresaAdminResponse response = service.create(ACTOR, createRequest(true, null));

        ArgumentCaptor<EmpresaEntity> empresaCaptor = ArgumentCaptor.forClass(EmpresaEntity.class);
        verify(empresaRepository).save(empresaCaptor.capture());
        EmpresaEntity empresa = empresaCaptor.getValue();
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.EM_TESTE); // default quando ausente
        assertThat(empresa.isHealthVertical()).isTrue();
        assertThat(empresa.getCompanyId()).startsWith("emp_");
        assertThat(empresa.getCreatedAt()).isNotNull();

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity user = userCaptor.getValue();
        assertThat(user.getRoles()).containsExactly(AdminEmpresaService.EMPRESA_ROLE);
        assertThat(user.getRoles()).doesNotContain("ADMIN");
        assertThat(user.getStatus()).isEqualTo(UserStatus.CONVIDADO);
        assertThat(user.getInviteTokenHash()).isNotNull();

        assertThat(response.inviteUrl()).contains("/convite/");
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), anyString(), eq(AuditEventType.ADMIN_EMPRESA_CREATED), anyString(), anyString());
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), anyString(), eq(AuditEventType.ADMIN_USER_INVITED), anyString(), anyString());
    }

    @Test
    void createWithoutInviteCreatesActiveUserAndNoInviteUrl() {
        when(userRepository.findByEmpresaIdOrderByCreatedAtAsc(anyString())).thenReturn(List.of());

        CreateEmpresaAdminResponse response = service.create(ACTOR, createRequest(false, EmpresaStatus.ATIVO));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ATIVO);
        assertThat(userCaptor.getValue().getInviteTokenHash()).isNull();
        assertThat(response.inviteUrl()).isNull();
    }

    @Test
    void suspendSetsStatusAndAuditsReason() {
        EmpresaEntity empresa = existingEmpresa("tnt_1", EmpresaStatus.ATIVO);
        when(empresaRepository.findById("tnt_1")).thenReturn(Optional.of(empresa));
        when(userRepository.findByEmpresaIdOrderByCreatedAtAsc("tnt_1")).thenReturn(List.of());

        service.suspend(ACTOR, "tnt_1", "Inadimplência");

        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.SUSPENSO);
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), eq("tnt_1"), eq(AuditEventType.ADMIN_EMPRESA_SUSPENDED), anyString(), anyString());
    }

    @Test
    void cancelSetsStatusCanceled() {
        EmpresaEntity empresa = existingEmpresa("tnt_2", EmpresaStatus.ATIVO);
        when(empresaRepository.findById("tnt_2")).thenReturn(Optional.of(empresa));
        when(userRepository.findByEmpresaIdOrderByCreatedAtAsc("tnt_2")).thenReturn(List.of());

        service.cancel(ACTOR, "tnt_2", "Encerramento");

        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.CANCELADO);
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), eq("tnt_2"), eq(AuditEventType.ADMIN_EMPRESA_CANCELED), anyString(), anyString());
    }

    @Test
    void reactivateRejectsInvalidTargetStatus() {
        assertThatThrownBy(() -> service.reactivate(
                ACTOR, "tnt_3", new ReactivateEmpresaAdminRequest("ok", EmpresaStatus.SUSPENSO)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void reactivateDefaultsToAtivo() {
        EmpresaEntity empresa = existingEmpresa("tnt_4", EmpresaStatus.SUSPENSO);
        when(empresaRepository.findById("tnt_4")).thenReturn(Optional.of(empresa));
        when(userRepository.findByEmpresaIdOrderByCreatedAtAsc("tnt_4")).thenReturn(List.of());

        service.reactivate(ACTOR, "tnt_4", new ReactivateEmpresaAdminRequest("Regularizado", null));

        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.ATIVO);
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), eq("tnt_4"), eq(AuditEventType.ADMIN_EMPRESA_REACTIVATED), anyString(), anyString());
    }

    @Test
    void planChangeIsAudited() {
        EmpresaEntity empresa = existingEmpresa("tnt_5", EmpresaStatus.ATIVO);
        empresa.setCommercialPlanType(CommercialPlanType.PROFISSIONAL);
        when(empresaRepository.findById("tnt_5")).thenReturn(Optional.of(empresa));
        when(userRepository.findByEmpresaIdOrderByCreatedAtAsc("tnt_5")).thenReturn(List.of());

        service.update(ACTOR, "tnt_5", new UpdateEmpresaAdminRequest(
                null, null, null, null, null, null, null, null, CommercialPlanType.AVULSO, null));

        assertThat(empresa.getCommercialPlanType()).isEqualTo(CommercialPlanType.AVULSO);
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), eq("tnt_5"), eq(AuditEventType.ADMIN_COMMERCIAL_PLAN_CHANGED), anyString(), anyString());
    }

    @Test
    void inviteUserCreatesEmpresaRole() {
        EmpresaEntity empresa = existingEmpresa("tnt_6", EmpresaStatus.ATIVO);
        when(empresaRepository.findById("tnt_6")).thenReturn(Optional.of(empresa));
        when(userRepository.existsByEmpresaIdAndEmail("tnt_6", "novo@acme.com")).thenReturn(false);

        service.inviteUser(ACTOR, "tnt_6", new InviteUserAdminRequest("Novo", "novo@acme.com"));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(AdminEmpresaService.EMPRESA_ROLE);
    }

    @Test
    void resendInviteRefreshesTokenForInvitedUser() {
        EmpresaEntity empresa = existingEmpresa("tnt_8", EmpresaStatus.ATIVO);
        when(empresaRepository.findById("tnt_8")).thenReturn(Optional.of(empresa));
        UserEntity user = new UserEntity();
        user.setId(11L);
        user.setEmpresaId("tnt_8");
        user.setEmail("convidado@acme.com");
        user.setStatus(UserStatus.CONVIDADO);
        when(userRepository.findByIdAndEmpresaId(11L, "tnt_8")).thenReturn(Optional.of(user));

        var response = service.resendInvite(ACTOR, "tnt_8", 11L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.CONVIDADO);
        assertThat(user.getInviteTokenHash()).isNotNull();
        assertThat(response.inviteUrl()).contains("/convite/");
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), eq("tnt_8"), eq(AuditEventType.ADMIN_USER_INVITE_RESENT), anyString(), anyString());
    }

    @Test
    void resendInviteRejectsUserThatIsNotInvited() {
        EmpresaEntity empresa = existingEmpresa("tnt_9", EmpresaStatus.ATIVO);
        when(empresaRepository.findById("tnt_9")).thenReturn(Optional.of(empresa));
        UserEntity user = new UserEntity();
        user.setId(12L);
        user.setEmpresaId("tnt_9");
        user.setEmail("ativo@acme.com");
        user.setStatus(UserStatus.ATIVO);
        when(userRepository.findByIdAndEmpresaId(12L, "tnt_9")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resendInvite(ACTOR, "tnt_9", 12L))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(user.getInviteTokenHash()).isNull();
    }

    @Test
    void blockAndUnblockChangeUserStatus() {
        EmpresaEntity empresa = existingEmpresa("tnt_7", EmpresaStatus.ATIVO);
        when(empresaRepository.findById("tnt_7")).thenReturn(Optional.of(empresa));
        UserEntity user = new UserEntity();
        user.setId(9L);
        user.setEmpresaId("tnt_7");
        user.setEmail("u@acme.com");
        user.setStatus(UserStatus.ATIVO);
        when(userRepository.findByIdAndEmpresaId(9L, "tnt_7")).thenReturn(Optional.of(user));

        AdminUserResponse blocked = service.blockUser(ACTOR, "tnt_7", 9L);
        assertThat(blocked.status()).isEqualTo(UserStatus.BLOQUEADO);

        AdminUserResponse unblocked = service.unblockUser(ACTOR, "tnt_7", 9L);
        assertThat(unblocked.status()).isEqualTo(UserStatus.ATIVO);
    }

    @Test
    void platformEmpresaIsNotTreatedAsClient() {
        assertThatThrownBy(() -> service.detail("PLATFORM"))
                .isInstanceOf(ResponseStatusException.class);
    }

    private EmpresaEntity existingEmpresa(String id, EmpresaStatus status) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(id);
        empresa.setName("Cliente " + id);
        empresa.setCompanyId("emp_" + id);
        empresa.setStatus(status);
        empresa.setCommercialPlanType(CommercialPlanType.PROFISSIONAL);
        return empresa;
    }
}
