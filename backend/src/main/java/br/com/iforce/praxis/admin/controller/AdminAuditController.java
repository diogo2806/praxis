package br.com.iforce.praxis.admin.controller;

import br.com.iforce.praxis.admin.dto.AdminAuditEventResponse;

import br.com.iforce.praxis.admin.service.AdminAuditService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;


import java.util.List;


/**
 * Auditoria administrativa append-only (somente leitura). Não há rota para editar, excluir
 * ou ocultar eventos.
 */
@RestController
@RequestMapping("/api/admin/audit")
@Tag(name = "Admin · Auditoria", description = "Trilha append-only de ações administrativas (somente leitura).")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    @Operation(summary = "Lista eventos de auditoria")
    public ResponseEntity<List<AdminAuditEventResponse>> list(
            @RequestParam(required = false, defaultValue = "200") int limit
    ) {
        return ResponseEntity.ok(adminAuditService.listAll(limit));
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Detalha evento de auditoria")
    public ResponseEntity<AdminAuditEventResponse> get(@PathVariable Long eventId) {
        return ResponseEntity.ok(adminAuditService.getById(eventId));
    }
}
