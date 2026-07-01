package br.com.iforce.praxis.marketplace.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.marketplace.dto.MessageThreadResponse;
import br.com.iforce.praxis.marketplace.dto.SendMessageRequest;
import br.com.iforce.praxis.marketplace.service.MarketplaceMessageService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/marketplace/messages")
public class MarketplaceMessageController {

    private final MarketplaceMessageService messageService;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;

    public MarketplaceMessageController(
            MarketplaceMessageService messageService,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService
    ) {
        this.messageService = messageService;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/tenant")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<MessageThreadResponse> sendAsTenant(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(201).body(messageService.sendAsTenant(
                currentEmpresaService.requiredEmpresaId(),
                Long.valueOf(currentUserService.requiredUserId()),
                request
        ));
    }

    @PostMapping("/professional")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<MessageThreadResponse> sendAsProfessional(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(201).body(messageService.sendAsProfessional(
                currentUserService.requiredUserId(),
                request
        ));
    }

    @GetMapping
    public ResponseEntity<List<MessageThreadResponse>> list(@RequestParam(defaultValue = "tenant") String scope) {
        if ("professional".equalsIgnoreCase(scope)) {
            return ResponseEntity.ok(messageService.listForProfessional(currentUserService.requiredUserId()));
        }
        return ResponseEntity.ok(messageService.listForTenant(currentEmpresaService.requiredEmpresaId()));
    }
}
