package br.com.iforce.praxis.team.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.auth.service.PasswordResetEmailSender;
import br.com.iforce.praxis.team.dto.InviteTeamUserRequest;
import br.com.iforce.praxis.team.dto.InviteTeamUserResponse;
import br.com.iforce.praxis.team.dto.TeamUserResponse;
import br.com.iforce.praxis.team.dto.UpdateTeamUserAccessRequest;
import br.com.iforce.praxis.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/team")
@Tag(name = "Equipe", description = "Gerenciamento de usuários, perfis e acessos pelo próprio cliente.")
public class TeamController {

    private final TeamService teamService;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final PasswordResetEmailSender notifier;
    private final int inviteTtlHours;

    public TeamController(
            TeamService teamService,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            PasswordResetEmailSender notifier,
            @Value("${praxis.admin.invite-ttl-hours:168}") int inviteTtlHours
    ) {
        this.teamService = teamService;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.notifier = notifier;
        this.inviteTtlHours = inviteTtlHours;
    }

    @GetMapping
    @Operation(summary = "Lista usuários, perfis, permissões e situação de acesso da equipe")
    public ResponseEntity<List<TeamUserResponse>> list() {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(teamService.listUsers(tenantId));
    }

    @PostMapping("/invite")
    @Operation(summary = "Convida novo usuário com perfil operacional")
    public ResponseEntity<InviteTeamUserResponse> invite(@Valid @RequestBody InviteTeamUserRequest request) {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        InviteTeamUserResponse response = teamService.inviteUser(actorUserId, tenantId, request);
        notifyInvite(response);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/access")
    @Operation(summary = "Altera o perfil e as permissões de um usuário")
    public ResponseEntity<TeamUserResponse> updateAccess(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateTeamUserAccessRequest request
    ) {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.updateUserAccess(actorUserId, tenantId, userId, request));
    }

    @PostMapping("/{userId}/resend-invite")
    @Operation(summary = "Reenvia convite pendente")
    public ResponseEntity<InviteTeamUserResponse> resendInvite(@PathVariable Long userId) {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        InviteTeamUserResponse response = teamService.resendInvite(actorUserId, tenantId, userId);
        notifyInvite(response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/block")
    @Operation(summary = "Bloqueia o acesso de um usuário")
    public ResponseEntity<TeamUserResponse> block(@PathVariable Long userId) {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.blockUser(actorUserId, tenantId, userId));
    }

    @PostMapping("/{userId}/unblock")
    @Operation(summary = "Desbloqueia o acesso de um usuário")
    public ResponseEntity<TeamUserResponse> unblock(@PathVariable Long userId) {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.unblockUser(actorUserId, tenantId, userId));
    }

    private void notifyInvite(InviteTeamUserResponse response) {
        notifier.sendTeamInviteEmail(
                response.user().email(),
                response.user().name(),
                response.inviteUrl(),
                inviteTtlHours
        );
    }
}
