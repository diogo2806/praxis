package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.AdminUserResponse;
import br.com.iforce.praxis.admin.dto.CreateTenantAdminRequest;
import br.com.iforce.praxis.admin.dto.CreateTenantAdminResponse;
import br.com.iforce.praxis.admin.dto.InviteUserAdminRequest;
import br.com.iforce.praxis.admin.dto.ReactivateTenantAdminRequest;
import br.com.iforce.praxis.admin.dto.UpdateTenantAdminRequest;
import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
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
class AdminTenantServiceTest {

    private static final String ACTOR = "admin-1";

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private AdminUsageService adminUsageService;

    private AdminTenantService service;

    @BeforeEach
    void setUp() {
        service = new AdminTenantService(
                tenantRepository,
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
        lenient().when(tenantRepository.existsById(anyString())).thenReturn(false);
        lenient().when(tenantRepository.findFirstByCompanyId(anyString())).thenReturn(Optional.empty());
        lenient().when(tenantRepository.save(any(TenantEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CreateTenantAdminRequest createRequest(boolean sendInvite, TenantStatus initial) {
        return new CreateTenantAdminRequest(
                "Acme Saúde", "Acme", "Acme S.A.", "12345678000199",
                "contato@acme.com", "+55 11 99999-0000", "https://acme.com",
                true, null, CommercialPlanType.PROFISSIONAL, "Condição padrão",
                initial, "Maria", "maria@acme.com", sendInvite);
    }

    @Test
    void createProvisionsTenantAndEmpresaUserWithInvite() {
        when(userRepository.findByTenantIdOrderByCreatedAtAsc(anyString())).thenReturn(List.of());

        CreateTenantAdminResponse response = service.create(ACTOR, createRequest(true, null));

        ArgumentCaptor<TenantEntity> tenantCaptor = ArgumentCaptor.forClass(TenantEntity.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        TenantEntity tenant = tenantCaptor.getValue();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.EM_TESTE); // default quando ausente
        assertThat(tenant.isHealthVertical()).isTrue();
        assertThat(tenant.getCompanyId()).startsWith("emp_");
        assertThat(tenant.getCreatedAt()).isNotNull();

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity user = userCaptor.getValue();
        assertThat(user.getRoles()).containsExactly(AdminTenantService.EMPRESA_ROLE);
        assertThat(user.getRoles()).doesNotContain("ADMIN");
        assertThat(user.getStatus()).isEqualTo(UserStatus.CONVIDADO);
        assertThat(user.getInviteTokenHash()).isNotNull();

        assertThat(response.inviteUrl()).contains("/convite/");
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), anyString(), eq(AuditEventType.ADMIN_TENANT_CREATED), anyString(), anyString());
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), anyString(), eq(AuditEventType.ADMIN_USER_INVITED), anyString(), anyString());
    }

    @Test
    void createWithoutInviteCreatesActiveUserAndNoInviteUrl() {
        when(userRepository.findByTenantIdOrderByCreatedAtAsc(anyString())).thenReturn(List.of());

        CreateTenantAdminResponse response = service.create(ACTOR, createRequest(false, TenantStatus.ATIVO));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ATIVO);
        assertThat(userCaptor.getValue().getInviteTokenHash()).isNull();
        assertThat(response.inviteUrl()).isNull();
    }

    @Test
    void suspendSetsStatusAndAuditsReason() {
        TenantEntity tenant = existingTenant("tnt_1", TenantStatus.ATIVO);
        when(tenantRepository.findById("tnt_1")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantIdOrderByCreatedAtAsc("tnt_1")).thenReturn(List.of());

        service.suspend(ACTOR, "tnt_1", "Inadimplência");

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENSO);
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), eq("tnt_1"), eq(AuditEventType.ADMIN_TENANT_SUSPENDED), anyString(), anyString());
    }

    @Test
    void cancelSetsStatusCanceled() {
        TenantEntity tenant = existingTenant("tnt_2", TenantStatus.ATIVO);
        when(tenantRepository.findById("tnt_2")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantIdOrderByCreatedAtAsc("tnt_2")).thenReturn(List.of());

        service.cancel(ACTOR, "tnt_2", "Encerramento");

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.CANCELADO);
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), eq("tnt_2"), eq(AuditEventType.ADMIN_TENANT_CANCELED), anyString(), anyString());
    }

    @Test
    void reactivateRejectsInvalidTargetStatus() {
        assertThatThrownBy(() -> service.reactivate(
                ACTOR, "tnt_3", new ReactivateTenantAdminRequest("ok", TenantStatus.SUSPENSO)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void reactivateDefaultsToAtivo() {
        TenantEntity tenant = existingTenant("tnt_4", TenantStatus.SUSPENSO);
        when(tenantRepository.findById("tnt_4")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantIdOrderByCreatedAtAsc("tnt_4")).thenReturn(List.of());

        service.reactivate(ACTOR, "tnt_4", new ReactivateTenantAdminRequest("Regularizado", null));

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ATIVO);
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), eq("tnt_4"), eq(AuditEventType.ADMIN_TENANT_REACTIVATED), anyString(), anyString());
    }

    @Test
    void planChangeIsAudited() {
        TenantEntity tenant = existingTenant("tnt_5", TenantStatus.ATIVO);
        tenant.setCommercialPlanType(CommercialPlanType.PROFISSIONAL);
        when(tenantRepository.findById("tnt_5")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantIdOrderByCreatedAtAsc("tnt_5")).thenReturn(List.of());

        service.update(ACTOR, "tnt_5", new UpdateTenantAdminRequest(
                null, null, null, null, null, null, null, null, CommercialPlanType.AVULSO, null));

        assertThat(tenant.getCommercialPlanType()).isEqualTo(CommercialPlanType.AVULSO);
        verify(auditEventService).auditAdminAction(
                eq(ACTOR), eq("tnt_5"), eq(AuditEventType.ADMIN_COMMERCIAL_PLAN_CHANGED), anyString(), anyString());
    }

    @Test
    void inviteUserCreatesEmpresaRole() {
        TenantEntity tenant = existingTenant("tnt_6", TenantStatus.ATIVO);
        when(tenantRepository.findById("tnt_6")).thenReturn(Optional.of(tenant));
        when(userRepository.existsByTenantIdAndEmail("tnt_6", "novo@acme.com")).thenReturn(false);

        service.inviteUser(ACTOR, "tnt_6", new InviteUserAdminRequest("Novo", "novo@acme.com"));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(AdminTenantService.EMPRESA_ROLE);
    }

    @Test
    void blockAndUnblockChangeUserStatus() {
        TenantEntity tenant = existingTenant("tnt_7", TenantStatus.ATIVO);
        when(tenantRepository.findById("tnt_7")).thenReturn(Optional.of(tenant));
        UserEntity user = new UserEntity();
        user.setId(9L);
        user.setTenantId("tnt_7");
        user.setEmail("u@acme.com");
        user.setStatus(UserStatus.ATIVO);
        when(userRepository.findByIdAndTenantId(9L, "tnt_7")).thenReturn(Optional.of(user));

        AdminUserResponse blocked = service.blockUser(ACTOR, "tnt_7", 9L);
        assertThat(blocked.status()).isEqualTo(UserStatus.BLOQUEADO);

        AdminUserResponse unblocked = service.unblockUser(ACTOR, "tnt_7", 9L);
        assertThat(unblocked.status()).isEqualTo(UserStatus.ATIVO);
    }

    @Test
    void platformTenantIsNotTreatedAsClient() {
        assertThatThrownBy(() -> service.detail("PLATFORM"))
                .isInstanceOf(ResponseStatusException.class);
    }

    private TenantEntity existingTenant(String id, TenantStatus status) {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(id);
        tenant.setName("Cliente " + id);
        tenant.setCompanyId("emp_" + id);
        tenant.setStatus(status);
        tenant.setCommercialPlanType(CommercialPlanType.PROFISSIONAL);
        return tenant;
    }
}
