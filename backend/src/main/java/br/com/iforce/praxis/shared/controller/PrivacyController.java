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


/**
 * Porta de entrada (API) das informações de privacidade exibidas ao usuário.
 *
 * <p>Na visão do processo, é por aqui que a tela mostra ao candidato as
 * informações de privacidade do processo seletivo: finalidades do uso dos
 * dados, por quanto tempo são guardados, contato do responsável e como pedir
 * revisão. O conteúdo vem de configurações definidas pela empresa
 * responsável, sem expor dados técnicos internos.</p>
 */
@RestController
@RequestMapping("/api/v1/privacy")
@Tag(name = "Privacy", description = "Informacoes operacionais de privacidade, explicabilidade e revisao.")
public class PrivacyController {

    private final int retentionDays;
    private final String controllerName;
    private final String serviceEmail;
    private final String serviceUrl;
    private final String dataProtectionOfficerContact;
    private final String reviewInstructions;

    public PrivacyController(
            @Value("${praxis.privacy-retention-days:180}") int retentionDays,
            @Value("${praxis.privacy.controller-name:Controlador a ser informado pela empresa}") String controllerName,
            @Value("${praxis.privacy.service-email:}") String serviceEmail,
            @Value("${praxis.privacy.service-url:}") String serviceUrl,
            @Value("${praxis.privacy.dpo-contact:}") String dataProtectionOfficerContact,
            @Value("${praxis.privacy.review-instructions:Solicite revisao pelo canal de atendimento configurado pela empresa responsavel pelo processo seletivo.}") String reviewInstructions
    ) {
        this.retentionDays = retentionDays;
        this.controllerName = controllerName;
        this.serviceEmail = serviceEmail;
        this.serviceUrl = serviceUrl;
        this.dataProtectionOfficerContact = dataProtectionOfficerContact;
        this.reviewInstructions = reviewInstructions;
    }

    /**
     * Devolve as informações de privacidade para exibir na tela.
     *
     * <p>Reúne as bases legais, o prazo de retenção, o contato do responsável
     * e o canal/orientações para solicitar revisão.</p>
     *
     * @return o pacote de informações de privacidade do processo
     */
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
                                "As informações exibidas refletem a configuração informada pelo controlador e não substituem sua análise jurídica."
                        ),
                        new PrivacyComplianceResponse.LegalBasisDto(
                                "Registro e salvaguardas",
                                "Retencao, compartilhamentos e salvaguardas devem ser definidos conforme a politica aplicavel ao processo."
                        )
                ),
                retentionDays,
                "Os dados sao mantidos pelo periodo configurado para o processo, observadas as finalidades informadas, obrigacoes aplicaveis e hipoteses de preservacao necessarias ao exercicio regular de direitos.",
                new PrivacyComplianceResponse.ControllerContactDto(
                        controllerName,
                        blankToNull(serviceEmail),
                        blankToNull(serviceUrl),
                        blankToNull(dataProtectionOfficerContact),
                        reviewInstructions
                ),
                reviewChannel(),
                "As solicitacoes serao tratadas nos prazos aplicaveis, conforme sua natureza e a legislacao vigente.",
                false
        ));
    }

    /**
     * Define qual canal de privacidade mostrar ao candidato: prioriza o site
     * configurado, depois o e-mail; se nada estiver configurado, devolve um
     * aviso orientando a configuração. Uso interno.
     */
    private String reviewChannel() {
        if (serviceUrl != null && !serviceUrl.isBlank()) {
            return serviceUrl;
        }
        if (serviceEmail != null && !serviceEmail.isBlank()) {
            return serviceEmail;
        }
        return "Canal de privacidade não configurado para esta empresa. Configure PRAXIS_PRIVACY_SERVICE_EMAIL ou PRAXIS_PRIVACY_SERVICE_URL antes de operar processos reais.";
    }

    /** Trata texto em branco como ausência de valor (nulo). Uso interno. */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
