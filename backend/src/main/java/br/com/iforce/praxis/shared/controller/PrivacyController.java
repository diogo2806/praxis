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
@Tag(name = "Privacy", description = "Politicas operacionais de privacidade, explicabilidade e revisao humana.")
public class PrivacyController {

    private final int retentionDays;

    public PrivacyController(@Value("${praxis.privacy-retention-days:180}") int retentionDays) {
        this.retentionDays = retentionDays;
    }

    @GetMapping("/compliance")
    @Operation(
            summary = "Retorna politica LGPD operacional",
            description = "Expõe bases legais, retencao e canal de revisao humana exibidos no frontend."
    )
    public ResponseEntity<PrivacyComplianceResponse> getCompliance() {
        return ResponseEntity.ok(new PrivacyComplianceResponse(
                List.of(
                        new PrivacyComplianceResponse.LegalBasisDto(
                                "Execucao de contrato",
                                "Processamento necessario para disponibilizar a simulacao contratada pela empresa."
                        ),
                        new PrivacyComplianceResponse.LegalBasisDto(
                                "Legitimo interesse",
                                "Uso de evidencias comportamentais estruturadas para avaliacao proporcional e auditavel."
                        ),
                        new PrivacyComplianceResponse.LegalBasisDto(
                                "Exercicio regular de direitos",
                                "Manutencao de trilhas de auditoria para contestacao, revisao e defesa tecnica."
                        )
                ),
                retentionDays,
                "Tentativas, respostas e resultados sao retidos por " + retentionDays + " dias apos conclusao ou expiracao, salvo obrigacao contratual distinta.",
                "privacy-review@praxis.local",
                "5 dias uteis",
                false
        ));
    }
}
