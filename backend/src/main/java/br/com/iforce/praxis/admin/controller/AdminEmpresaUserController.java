package br.com.iforce.praxis.admin.controller;

import br.com.iforce.praxis.admin.dto.AdminUserResponse;

import br.com.iforce.praxis.admin.dto.InviteUserAdminRequest;

import br.com.iforce.praxis.admin.dto.InviteUserAdminResponse;

import br.com.iforce.praxis.admin.service.AdminEmpresaService;

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
 * {@code ADMIN} dentro de um empresa cliente. Bloquear um usuário não apaga histórico.</p>
 */
@RestController
@RequestMapping("/api/admin/empresas/{empresaId}/users")
@Tag(name = "Admin · Acessos", description = "Usuários de acesso de um cliente gerenciados pelo ADMIN.")
public class AdminEmpresaUserController {

    private final AdminEmpresaService adminEmpresaService;
    private final CurrentUserService currentUserService;

    public AdminEmpresaUserController(
            AdminEmpresaService adminEmpresaService,
            CurrentUserService currentUserService
    ) {
        this.adminEmpresaService = adminEmpresaService;
        this.currentUserService = currentUserService;
    }

    /**
     * Lista as pessoas com acesso ao sistema em nome de um cliente.
     *
     * <p>Na visão do processo: é a aba "Acessos" — mostra quem pode entrar por aquela
     * empresa e a situação de cada um (convidado, ativo, bloqueado).</p>
     *
     * @param empresaId identificador do cliente
     * @return os usuários de acesso do cliente
     */
    @GetMapping
    @Operation(summary = "Lista usuários do cliente")
    public ResponseEntity<List<AdminUserResponse>> list(@PathVariable String empresaId) {
        return ResponseEntity.ok(adminEmpresaService.listUsers(empresaId));
    }

    /**
     * Convida uma nova pessoa para acessar em nome do cliente.
     *
     * <p>Na visão do processo: adiciona mais um acesso à empresa. A pessoa recebe um link
     * para criar a senha e entra sempre como usuário da empresa (nunca como operador).</p>
     *
     * @param empresaId identificador do cliente
     * @param request nome e e-mail da pessoa a convidar
     * @return os dados do usuário criado e o link de convite
     */
    @PostMapping("/invite")
    @Operation(summary = "Convida usuário EMPRESA")
    public ResponseEntity<InviteUserAdminResponse> invite(
            @PathVariable String empresaId,
            @Valid @RequestBody InviteUserAdminRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.inviteUser(
                currentUserService.requiredUserId(), empresaId, request));
    }

    /**
     * Reenvia o convite para uma pessoa que ainda não entrou.
     *
     * <p>Na visão do processo: reemite o link para quem foi convidado mas ainda não criou
     * a senha. Só vale para quem ainda está como "convidado".</p>
     *
     * @param empresaId identificador do cliente
     * @param userId identificador do usuário convidado
     * @return os dados do usuário e o novo link de convite
     */
    @PostMapping("/{userId}/resend-invite")
    @Operation(summary = "Reenvia convite")
    public ResponseEntity<InviteUserAdminResponse> resendInvite(
            @PathVariable String empresaId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminEmpresaService.resendInvite(
                currentUserService.requiredUserId(), empresaId, userId));
    }

    /**
     * Bloqueia o acesso de uma pessoa do cliente.
     *
     * <p>Na visão do processo: "tira a chave" do usuário — ele deixa de conseguir entrar,
     * mas o cadastro e o histórico são preservados. É reversível pelo desbloqueio.</p>
     *
     * @param empresaId identificador do cliente
     * @param userId identificador do usuário a bloquear
     * @return os dados atualizados do usuário
     */
    @PostMapping("/{userId}/block")
    @Operation(summary = "Bloqueia usuário")
    public ResponseEntity<AdminUserResponse> block(
            @PathVariable String empresaId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminEmpresaService.blockUser(
                currentUserService.requiredUserId(), empresaId, userId));
    }

    /**
     * Desbloqueia uma pessoa que estava impedida de acessar.
     *
     * <p>Na visão do processo: "devolve a chave" do usuário, deixando-o ativo de novo. É o
     * oposto do bloqueio.</p>
     *
     * @param empresaId identificador do cliente
     * @param userId identificador do usuário a desbloquear
     * @return os dados atualizados do usuário
     */
    @PostMapping("/{userId}/unblock")
    @Operation(summary = "Desbloqueia usuário")
    public ResponseEntity<AdminUserResponse> unblock(
            @PathVariable String empresaId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminEmpresaService.unblockUser(
                currentUserService.requiredUserId(), empresaId, userId));
    }
}
