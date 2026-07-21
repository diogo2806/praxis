package br.com.iforce.praxis.shared.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.shared.dto.PrivacyComplianceResponse;
import br.com.iforce.praxis.shared.privacy.service.PrivacyComplianceWorkflowService;
import br.com.iforce.praxis.shared.privacy.service.PrivacyComplianceWorkflowService.DataSubjectRequestResponse;
import br.com.iforce.praxis.shared.privacy.service.PrivacyComplianceWorkflowService.HumanReviewRequestResponse;
import br.com.iforce.praxis.shared.privacy.service.PrivacyComplianceWorkflowService.PrivacyConfigurationRequest;
import br.com.iforce.praxis.shared.privacy.service.PrivacyComplianceWorkflowService.PrivacyConfigurationResponse;
import br.com.iforce.praxis.shared.privacy.service.PrivacyComplianceWorkflowService.PrivacyIncidentRequest;
import br.com.iforce.praxis.shared.privacy.service.PrivacyComplianceWorkflowService.PrivacyIncidentResponse;
import br.com.iforce.praxis.shared.privacy.service.PrivacyComplianceWorkflowService.PrivacyIncidentUpdateRequest;
import br.com.iforce.praxis.shared.privacy.service.PrivacyComplianceWorkflowService.ResolveDataSubjectRequestRequest;
import br.com.iforce.praxis.shared.privacy.service.PrivacyComplianceWorkflowService.ResolveHumanReviewRequest;
import br.com.iforce.praxis.shared.privacy.service.PrivacyRetentionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Porta operacional de privacidade, revisão humana e resposta a incidentes. */
@RestController
@RequestMapping("/api/v1/privacy")
@Tag(name = "Privacy", description = "Privacidade, direitos do titular, revisão e incidentes.")
public class PrivacyController {

    private final PrivacyRetentionService privacyRetentionService;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final PrivacyComplianceWorkflowService workflowService;

    public PrivacyController(
            PrivacyRetentionService privacyRetentionService,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            PrivacyComplianceWorkflowService workflowService
    ) {
        this.privacyRetentionService = privacyRetentionService;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.workflowService = workflowService;
    }

    @GetMapping("/compliance")
    @Operation(summary = "Retorna informações operacionais de privacidade")
    public ResponseEntity<PrivacyComplianceResponse> getCompliance() {
        PrivacyConfigurationResponse configuration = workflowService.getConfiguration();
        String reviewChannel = configuration.serviceUrl() != null
                ? configuration.serviceUrl()
                : configuration.serviceEmail();
        if (reviewChannel == null || reviewChannel.isBlank()) {
            reviewChannel = "Canal de privacidade não configurado para esta empresa. "
                    + "Configure PRAXIS_PRIVACY_SERVICE_EMAIL ou PRAXIS_PRIVACY_SERVICE_URL antes de operar processos reais.";
        }
        String controllerName = configuration.controllerName() == null || configuration.controllerName().isBlank()
                ? "Controlador a ser informado pela empresa"
                : configuration.controllerName();
        int retentionDays = configuration.retentionDays() == null ? 180 : configuration.retentionDays();
        return ResponseEntity.ok(new PrivacyComplianceResponse(
                List.of(
                        new PrivacyComplianceResponse.LegalBasisDto(
                                "Base legal documentada pelo controlador",
                                configuration.legalBasis() == null
                                        ? "Configuração pendente."
                                        : configuration.legalBasis()
                        ),
                        new PrivacyComplianceResponse.LegalBasisDto(
                                "Finalidade e necessidade",
                                "O resultado apoia análise humana e deve permanecer limitado ao processo informado ao titular."
                        ),
                        new PrivacyComplianceResponse.LegalBasisDto(
                                "Registro e salvaguardas",
                                "Pedidos, revisões, ciência do aviso e anonimizações possuem trilha auditável."
                        )
                ),
                retentionDays,
                "Os dados são tratados pelo período configurado e depois passam pela rotina de descarte seguro.",
                new PrivacyComplianceResponse.ControllerContactDto(
                        controllerName,
                        configuration.serviceEmail(),
                        configuration.serviceUrl(),
                        configuration.dpoContact(),
                        "A revisão humana pode ser solicitada no próprio link da participação."
                ),
                reviewChannel,
                "Solicitações possuem prazo, situação, responsável e registro de resposta.",
                false
        ));
    }

    @GetMapping("/configuration")
    public ResponseEntity<PrivacyConfigurationResponse> getConfiguration() {
        return ResponseEntity.ok(workflowService.getConfiguration());
    }

    @PutMapping("/configuration")
    public ResponseEntity<PrivacyConfigurationResponse> updateConfiguration(
            @RequestBody PrivacyConfigurationRequest request
    ) {
        return ResponseEntity.ok(workflowService.updateConfiguration(request));
    }

    @PostMapping("/health-compliance/approve")
    public ResponseEntity<PrivacyConfigurationResponse> approveHealthCompliance() {
        return ResponseEntity.ok(workflowService.approveHealthCompliance());
    }

    @GetMapping("/requests")
    public ResponseEntity<List<DataSubjectRequestResponse>> listDataSubjectRequests() {
        return ResponseEntity.ok(workflowService.listDataSubjectRequests());
    }

    @PatchMapping("/requests/{requestId}")
    public ResponseEntity<DataSubjectRequestResponse> updateDataSubjectRequest(
            @PathVariable String requestId,
            @RequestBody ResolveDataSubjectRequestRequest request
    ) {
        return ResponseEntity.ok(workflowService.updateDataSubjectRequest(requestId, request));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<HumanReviewRequestResponse>> listHumanReviews() {
        return ResponseEntity.ok(workflowService.listHumanReviewRequests());
    }

    @PatchMapping("/reviews/{requestId}")
    public ResponseEntity<HumanReviewRequestResponse> resolveHumanReview(
            @PathVariable String requestId,
            @RequestBody ResolveHumanReviewRequest request
    ) {
        return ResponseEntity.ok(workflowService.resolveHumanReview(requestId, request));
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<PrivacyIncidentResponse>> listIncidents() {
        return ResponseEntity.ok(workflowService.listIncidents());
    }

    @PostMapping("/incidents")
    public ResponseEntity<PrivacyIncidentResponse> createIncident(@RequestBody PrivacyIncidentRequest request) {
        return ResponseEntity.ok(workflowService.createIncident(request));
    }

    @PatchMapping("/incidents/{incidentId}")
    public ResponseEntity<PrivacyIncidentResponse> updateIncident(
            @PathVariable String incidentId,
            @RequestBody PrivacyIncidentUpdateRequest request
    ) {
        return ResponseEntity.ok(workflowService.updateIncident(incidentId, request));
    }

    @PostMapping("/attempts/{attemptId}/action")
    @Operation(summary = "Executa anonimização manual de uma tentativa")
    public ResponseEntity<Void> applyManualPrivacyAction(
            @PathVariable String attemptId,
            @RequestParam(required = false) String reason
    ) {
        privacyRetentionService.anonymizeAttemptNow(
                currentEmpresaService.requiredEmpresaId(),
                attemptId,
                currentUserService.requiredUserId(),
                reason
        );
        return ResponseEntity.noContent().build();
    }
}
