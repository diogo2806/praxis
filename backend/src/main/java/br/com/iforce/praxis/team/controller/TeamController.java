package br.com.iforce.praxis.team.controller;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.team.dto.InviteTeamUserRequest;
import br.com.iforce.praxis.team.dto.InviteTeamUserResponse;
import br.com.iforce.praxis.team.dto.TeamUserResponse;
import br.com.iforce.praxis.team.service.TeamService;
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
 * Gerenciamento de usuários da equipe pelo próprio cliente.
 *
 * <p>O tenant é obtido do JWT do usuário logado; o cliente nunca acessa usuários
 * de outro tenant. Usuários convidados recebem sempre o papel {@code EMPRESA}.</p>
 */
@RestController
@RequestMapping("/api/v1/team")
@Tag(name = "Equipe", description = "Gerenciamento de usuários da equipe pelo próprio cliente.")
public class TeamController {

    private final TeamService teamService;
    private final CurrentTenantService currentTenantService;
    private final CurrentUserService currentUserService;

    public TeamController(
            TeamService teamService,
            CurrentTenantService currentTenantService,
            CurrentUserService currentUserService
    ) {
        this.teamService = teamService;
        this.currentTenantService = currentTenantService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "Lista usuários da equipe")
    public ResponseEntity<List<TeamUserResponse>> list() {
        String tenantId = currentTenantService.requiredTenantId();
        return ResponseEntity.ok(teamService.listUsers(tenantId));
    }

    @PostMapping("/invite")
    @Operation(summary = "Convida novo usuário EMPRESA")
    public ResponseEntity<InviteTeamUserResponse> invite(@Valid @RequestBody InviteTeamUserRequest request) {
        String tenantId = currentTenantService.requiredTenantId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.inviteUser(actorUserId, tenantId, request));
    }

    @PostMapping("/{userId}/resend-invite")
    @Operation(summary = "Reenvia convite pendente")
    public ResponseEntity<InviteTeamUserResponse> resendInvite(@PathVariable Long userId) {
        String tenantId = currentTenantService.requiredTenantId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.resendInvite(actorUserId, tenantId, userId));
    }

    @PostMapping("/{userId}/block")
    @Operation(summary = "Bloqueia usuário")
    public ResponseEntity<TeamUserResponse> block(@PathVariable Long userId) {
        String tenantId = currentTenantService.requiredTenantId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.blockUser(actorUserId, tenantId, userId));
    }

    @PostMapping("/{userId}/unblock")
    @Operation(summary = "Desbloqueia usuário")
    public ResponseEntity<TeamUserResponse> unblock(@PathVariable Long userId) {
        String tenantId = currentTenantService.requiredTenantId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.unblockUser(actorUserId, tenantId, userId));
    }
}
