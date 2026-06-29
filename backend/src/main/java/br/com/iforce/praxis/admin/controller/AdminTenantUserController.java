package br.com.iforce.praxis.admin.controller;

import br.com.iforce.praxis.admin.dto.AdminUserResponse;
import br.com.iforce.praxis.admin.dto.InviteUserAdminRequest;
import br.com.iforce.praxis.admin.dto.InviteUserAdminResponse;
import br.com.iforce.praxis.admin.service.AdminTenantService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API administrativa dos usuários de acesso de um cliente (aba "Acessos").
 *
 * <p>Usuários convidados recebem sempre o papel {@code EMPRESA}; o ADMIN nunca cria outro
 * {@code ADMIN} dentro de um tenant cliente. Bloquear um usuário não apaga histórico.</p>
 */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/users")
@Tag(name = "Admin · Acessos", description = "Usuários de acesso de um cliente gerenciados pelo ADMIN.")
public class AdminTenantUserController {

    private final AdminTenantService adminTenantService;
    private final CurrentUserService currentUserService;

    public AdminTenantUserController(
            AdminTenantService adminTenantService,
            CurrentUserService currentUserService
    ) {
        this.adminTenantService = adminTenantService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "Lista usuários do cliente")
    public ResponseEntity<List<AdminUserResponse>> list(@PathVariable String tenantId) {
        return ResponseEntity.ok(adminTenantService.listUsers(tenantId));
    }

    @PostMapping("/invite")
    @Operation(summary = "Convida usuário EMPRESA")
    public ResponseEntity<InviteUserAdminResponse> invite(
            @PathVariable String tenantId,
            @Valid @RequestBody InviteUserAdminRequest request
    ) {
        return ResponseEntity.ok(adminTenantService.inviteUser(
                currentUserService.requiredUserId(), tenantId, request));
    }

    @PostMapping("/{userId}/resend-invite")
    @Operation(summary = "Reenvia convite")
    public ResponseEntity<InviteUserAdminResponse> resendInvite(
            @PathVariable String tenantId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminTenantService.resendInvite(
                currentUserService.requiredUserId(), tenantId, userId));
    }

    @PostMapping("/{userId}/block")
    @Operation(summary = "Bloqueia usuário")
    public ResponseEntity<AdminUserResponse> block(
            @PathVariable String tenantId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminTenantService.blockUser(
                currentUserService.requiredUserId(), tenantId, userId));
    }

    @PostMapping("/{userId}/unblock")
    @Operation(summary = "Desbloqueia usuário")
    public ResponseEntity<AdminUserResponse> unblock(
            @PathVariable String tenantId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminTenantService.unblockUser(
                currentUserService.requiredUserId(), tenantId, userId));
    }
}
