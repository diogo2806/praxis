package br.com.iforce.praxis.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Página de clientes do painel administrativo.")
public record EmpresaAdminPageResponse(
        List<EmpresaAdminSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
