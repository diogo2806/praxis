package br.com.iforce.praxis.companyprofile.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Perfil cadastral da empresa visivel para o cliente.")
public record CompanyProfileResponse(
        @Schema(description = "Nome fantasia exibido para a empresa.")
        String tradeName,

        @Schema(description = "Razao social cadastrada.")
        String legalName,

        @Schema(description = "CNPJ ou identificador fiscal cadastrado.")
        String taxId,

        @Schema(description = "E-mail corporativo da empresa.")
        String corporateEmail,

        @Schema(description = "Telefone corporativo da empresa.")
        String phone,

        @Schema(description = "Site institucional da empresa.")
        String website
) {
}
