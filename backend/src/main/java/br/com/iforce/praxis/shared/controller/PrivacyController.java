package br.com.iforce.praxis.shared.controller;

import br.com.iforce.praxis.shared.dto.PrivacyComplianceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/privacy")
@Tag(name = "Privacy", description = "Informacoes operacionais de privacidade, explicabilidade e revisao.")
public class PrivacyController {

    private final int retentionDays;

    public PrivacyController(@Value("${praxis.privacy-retention-days:180}") int retentionDays) {
        this.retentionDays = retentionDays;
    }

    @GetMapping("/compliance")
    @Operation(
            summary = "Retorna informacoes operacionais de privacidade",
            description = "Exibe finalidades, retencao e orientacoes de revisao informadas para o frontend."
    )
    public ResponseEntity<PrivacyComplianceResponse> getCompliance() {
        return ResponseEntity.ok(new PrivacyComplianceResponse(
                List.of(
                        new PrivacyComplianceResponse.LegalBasisDto(
                                "Base legal informada pelo controlador",
                                "A empresa responsavel pelo processo seletivo define e documenta a base legal aplicavel a cada finalidade."
                        ),
                        new PrivacyComplianceResponse.LegalBasisDto(
                                "Finalidade e necessidade",
                                "As informacoes exibidas refletem a configuracao informada pelo controlador e nao substituem sua avaliacao juridica."
                        ),
                        new PrivacyComplianceResponse.LegalBasisDto(
                                "Registro e salvaguardas",
                                "Retencao, compartilhamentos e salvaguardas devem ser definidos conforme a politica aplicavel ao processo."
                        )
                ),
                retentionDays,
                "Os dados sao mantidos pelo periodo configurado para o processo, observadas as finalidades informadas, obrigacoes aplicaveis e hipoteses de preservacao necessarias ao exercicio regular de direitos.",
                "Canal informado pela empresa responsavel pelo processo seletivo.",
                "As solicitacoes serao tratadas nos prazos aplicaveis, conforme sua natureza e a legislacao vigente.",
                false
        ));
    }
}
