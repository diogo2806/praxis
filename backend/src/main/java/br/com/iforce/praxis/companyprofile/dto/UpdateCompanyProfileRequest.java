package br.com.iforce.praxis.companyprofile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados cadastrais editáveis pela própria empresa.")
public record UpdateCompanyProfileRequest(
        @NotBlank
        @Size(max = 180)
        @Schema(example = "iForce Tecnologia")
        String tradeName,

        @Size(max = 180)
        @Schema(example = "iForce Tecnologia LTDA")
        String legalName,

        @Size(max = 40)
        @Schema(example = "12.345.678/0001-90")
        String taxId,

        @Email
        @Size(max = 180)
        @Schema(example = "contato@iforce.com.br")
        String corporateEmail,

        @Size(max = 40)
        @Schema(example = "+55 21 99999-9999")
        String phone,

        @Size(max = 240)
        @Pattern(
                regexp = "^$|https?://.+$",
                message = "O site deve começar com http:// ou https://."
        )
        @Schema(example = "https://iforce.com.br")
        String website
) {
}
