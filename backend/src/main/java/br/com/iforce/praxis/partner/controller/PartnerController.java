package br.com.iforce.praxis.partner.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.auth.service.PasswordResetEmailSender;
import br.com.iforce.praxis.partner.dto.CreatePartnerClientRequest;
import br.com.iforce.praxis.partner.dto.CreatePartnerSpecialistRequest;
import br.com.iforce.praxis.partner.dto.PartnerModuleResponse;
import br.com.iforce.praxis.partner.dto.UpdatePartnerCatalogRequest;
import br.com.iforce.praxis.partner.service.PartnerModuleAccessService;
import br.com.iforce.praxis.partner.service.PartnerService;
import br.com.iforce.praxis.team.dto.InviteTeamUserResponse;
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
@RequestMapping("/api/v1/partners")
@Tag(name = "Parceiros", description = "Especialistas, clientes e catálogo distribuído de avaliações.")
public class PartnerController {

    private final PartnerService partnerService;
    private final PartnerModuleAccessService accessService;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final PasswordResetEmailSender notifier;
    private final int inviteTtlHours;

    public PartnerController(
            PartnerService partnerService,
            PartnerModuleAccessService accessService,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            PasswordResetEmailSender notifier,
            @Value("${praxis.admin.invite-ttl-hours:168}") int inviteTtlHours
    ) {
        this.partnerService = partnerService;
        this.accessService = accessService;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.notifier = notifier;
        this.inviteTtlHours = inviteTtlHours;
    }

    @GetMapping("/specialists")
    @Operation(summary = "Lista especialistas do parceiro")
    public ResponseEntity<List<PartnerModuleResponse.Specialist>> listSpecialists() {
        requireModuleAccess();
        return ResponseEntity.ok(partnerService.listSpecialists(currentEmpresaService.requiredEmpresaId()));
    }

    @PostMapping("/specialists/invite")
    @Operation(summary = "Convida um especialista")
    public ResponseEntity<InviteTeamUserResponse> inviteSpecialist(
            @Valid @RequestBody CreatePartnerSpecialistRequest request
    ) {
        requireModuleAccess();
        InviteTeamUserResponse response = partnerService.inviteSpecialist(
                currentUserService.requiredUserId(),
                currentEmpresaService.requiredEmpresaId(),
                request
        );
        notifier.sendTeamInviteEmail(
                response.user().email(),
                response.user().name(),
                response.inviteUrl(),
                inviteTtlHours
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/specialists/{userId}")
    @Operation(summary = "Define um usuário existente como especialista")
    public ResponseEntity<PartnerModuleResponse.Specialist> promoteSpecialist(@PathVariable Long userId) {
        requireModuleAccess();
        return ResponseEntity.ok(partnerService.promoteSpecialist(
                currentUserService.requiredUserId(),
                currentEmpresaService.requiredEmpresaId(),
                userId
        ));
    }

    @PostMapping("/specialists/{userId}/remove")
    @Operation(summary = "Remove a função de especialista")
    public ResponseEntity<Void> removeSpecialist(@PathVariable Long userId) {
        requireModuleAccess();
        partnerService.removeSpecialist(
                currentUserService.requiredUserId(),
                currentEmpresaService.requiredEmpresaId(),
                userId
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clients")
    @Operation(summary = "Lista clientes atendidos pelo parceiro")
    public ResponseEntity<List<PartnerModuleResponse.Client>> listClients() {
        requireModuleAccess();
        return ResponseEntity.ok(partnerService.listClients(currentEmpresaService.requiredEmpresaId()));
    }

    @PostMapping("/clients")
    @Operation(summary = "Cadastra cliente do parceiro")
    public ResponseEntity<PartnerModuleResponse.Client> createClient(
            @Valid @RequestBody CreatePartnerClientRequest request
    ) {
        requireModuleAccess();
        return ResponseEntity.ok(partnerService.createClient(
                currentUserService.requiredUserId(),
                currentEmpresaService.requiredEmpresaId(),
                request
        ));
    }

    @PostMapping("/clients/{clientId}/activate")
    @Operation(summary = "Ativa cliente do parceiro")
    public ResponseEntity<PartnerModuleResponse.Client> activateClient(@PathVariable String clientId) {
        requireModuleAccess();
        return ResponseEntity.ok(partnerService.setClientActive(
                currentUserService.requiredUserId(),
                currentEmpresaService.requiredEmpresaId(),
                clientId,
                true
        ));
    }

    @PostMapping("/clients/{clientId}/deactivate")
    @Operation(summary = "Desativa cliente e revoga seu token")
    public ResponseEntity<PartnerModuleResponse.Client> deactivateClient(@PathVariable String clientId) {
        requireModuleAccess();
        return ResponseEntity.ok(partnerService.setClientActive(
                currentUserService.requiredUserId(),
                currentEmpresaService.requiredEmpresaId(),
                clientId,
                false
        ));
    }

    @PostMapping("/clients/{clientId}/token")
    @Operation(summary = "Gera ou rotaciona o token do cliente")
    public ResponseEntity<PartnerModuleResponse.Token> rotateClientToken(@PathVariable String clientId) {
        requireModuleAccess();
        return ResponseEntity.ok(partnerService.rotateClientToken(
                currentUserService.requiredUserId(),
                currentEmpresaService.requiredEmpresaId(),
                clientId
        ));
    }

    @GetMapping("/clients/{clientId}/catalog")
    @Operation(summary = "Lista o catálogo e as liberações do cliente")
    public ResponseEntity<List<PartnerModuleResponse.CatalogItem>> listCatalog(@PathVariable String clientId) {
        requireModuleAccess();
        return ResponseEntity.ok(partnerService.listCatalog(
                currentEmpresaService.requiredEmpresaId(),
                clientId
        ));
    }

    @PutMapping("/clients/{clientId}/catalog")
    @Operation(summary = "Substitui os testes liberados para o cliente")
    public ResponseEntity<List<PartnerModuleResponse.CatalogItem>> updateCatalog(
            @PathVariable String clientId,
            @Valid @RequestBody UpdatePartnerCatalogRequest request
    ) {
        requireModuleAccess();
        return ResponseEntity.ok(partnerService.updateCatalog(
                currentUserService.requiredUserId(),
                currentEmpresaService.requiredEmpresaId(),
                clientId,
                request
        ));
    }

    private void requireModuleAccess() {
        accessService.requireAccess(
                currentUserService.requiredUserId(),
                currentEmpresaService.requiredEmpresaId()
        );
    }
}
