package br.com.iforce.praxis.team.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
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
 * Porta de entrada (API) da tela de "Equipe".
 *
 * <p>Na visão do processo, é por aqui que o cliente administra os usuários da
 * própria empresa: listar quem tem acesso, convidar novas pessoas, reenviar
 * convites pendentes e bloquear ou desbloquear o acesso de alguém. Este
 * componente apenas recebe o pedido vindo da tela — descobrindo a empresa e o
 * usuário logados a partir do login — e repassa o trabalho para a regra de
 * negócio ({@link TeamService}), que faz o trabalho de fato.</p>
 *
 * <p>A empresa é sempre determinada pelo login do usuário, de modo que ninguém
 * consegue acessar usuários de outra empresa. Toda pessoa convidada por aqui
 * entra com o perfil de acesso padrão de empresa ({@code EMPRESA}).</p>
 */
@RestController
@RequestMapping("/api/v1/team")
@Tag(name = "Equipe", description = "Gerenciamento de usuários da equipe pelo próprio cliente.")
public class TeamController {

    private final TeamService teamService;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;

    public TeamController(
            TeamService teamService,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService
    ) {
        this.teamService = teamService;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
    }

    /**
     * Lista os usuários da equipe da empresa logada para exibir na tela.
     *
     * @return os usuários cadastrados na empresa, com nome, e-mail, perfis e situação
     */
    @GetMapping
    @Operation(summary = "Lista usuários da equipe")
    public ResponseEntity<List<TeamUserResponse>> list() {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(teamService.listUsers(tenantId));
    }

    /**
     * Recebe o pedido para convidar uma nova pessoa para a equipe.
     *
     * @param request nome e e-mail da pessoa a convidar
     * @return os dados do usuário criado junto com o link de convite a ser enviado
     */
    @PostMapping("/invite")
    @Operation(summary = "Convida novo usuário EMPRESA")
    public ResponseEntity<InviteTeamUserResponse> invite(@Valid @RequestBody InviteTeamUserRequest request) {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.inviteUser(actorUserId, tenantId, request));
    }

    /**
     * Recebe o pedido para reenviar o convite de uma pessoa que ainda não entrou.
     *
     * @param userId identificador do usuário que receberá o novo convite
     * @return os dados do usuário junto com o novo link de convite
     */
    @PostMapping("/{userId}/resend-invite")
    @Operation(summary = "Reenvia convite pendente")
    public ResponseEntity<InviteTeamUserResponse> resendInvite(@PathVariable Long userId) {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.resendInvite(actorUserId, tenantId, userId));
    }

    /**
     * Recebe o pedido para bloquear o acesso de um usuário da equipe.
     *
     * @param userId identificador do usuário a ser bloqueado
     * @return os dados do usuário já com a situação "bloqueado"
     */
    @PostMapping("/{userId}/block")
    @Operation(summary = "Bloqueia usuário")
    public ResponseEntity<TeamUserResponse> block(@PathVariable Long userId) {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.blockUser(actorUserId, tenantId, userId));
    }

    /**
     * Recebe o pedido para reativar o acesso de um usuário bloqueado.
     *
     * @param userId identificador do usuário a ser desbloqueado
     * @return os dados do usuário já com a situação "ativo"
     */
    @PostMapping("/{userId}/unblock")
    @Operation(summary = "Desbloqueia usuário")
    public ResponseEntity<TeamUserResponse> unblock(@PathVariable Long userId) {
        String tenantId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        return ResponseEntity.ok(teamService.unblockUser(actorUserId, tenantId, userId));
    }
}
