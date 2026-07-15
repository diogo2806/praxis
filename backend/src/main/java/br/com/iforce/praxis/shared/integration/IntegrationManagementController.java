package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.shared.integration.dto.ConfigureIntegrationRequest;
import br.com.iforce.praxis.shared.integration.dto.GenerateIntegrationTokenResponse;
import br.com.iforce.praxis.shared.integration.dto.IntegrationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationManagementController {

    private final IntegrationManagementService integrationManagementService;
    private final IntegrationStatusRefreshService integrationStatusRefreshService;

    public IntegrationManagementController(
            IntegrationManagementService integrationManagementService,
            IntegrationStatusRefreshService integrationStatusRefreshService
    ) {
        this.integrationManagementService = integrationManagementService;
        this.integrationStatusRefreshService = integrationStatusRefreshService;
    }

    @GetMapping
    public ResponseEntity<List<IntegrationResponse>> listIntegrations() {
        return ResponseEntity.ok(integrationStatusRefreshService.listIntegrations());
    }

    @GetMapping("/{provider}")
    public ResponseEntity<IntegrationResponse> getIntegration(@PathVariable String provider) {
        return ResponseEntity.ok(integrationStatusRefreshService.getIntegration(provider));
    }

    @PostMapping("/{provider}/configure")
    public ResponseEntity<IntegrationResponse> configure(
            @PathVariable String provider,
            @RequestBody(required = false) ConfigureIntegrationRequest request
    ) {
        return ResponseEntity.ok(integrationStatusRefreshService.normalize(
                integrationManagementService.configure(provider, request)
        ));
    }

    @PostMapping("/{provider}/disconnect")
    public ResponseEntity<IntegrationResponse> disconnect(@PathVariable String provider) {
        return ResponseEntity.ok(integrationStatusRefreshService.normalize(
                integrationManagementService.disconnect(provider)
        ));
    }

    @PostMapping("/{provider}/sync")
    public ResponseEntity<IntegrationResponse> sync(@PathVariable String provider) {
        return ResponseEntity.ok(integrationStatusRefreshService.normalize(
                integrationManagementService.sync(provider)
        ));
    }

    /**
     * Compatibilidade com clientes antigos. O endpoint não testa conectividade externa:
     * apenas consulta novamente o estado persistido e comprovável da integração.
     */
    @PostMapping("/{provider}/test-connection")
    public ResponseEntity<IntegrationResponse> testConnection(@PathVariable String provider) {
        return ResponseEntity.ok(integrationStatusRefreshService.refreshStatus(provider));
    }

    @PostMapping("/{provider}/refresh-status")
    public ResponseEntity<IntegrationResponse> refreshStatus(@PathVariable String provider) {
        return ResponseEntity.ok(integrationStatusRefreshService.refreshStatus(provider));
    }

    @PostMapping("/{provider}/reactivate")
    public ResponseEntity<IntegrationResponse> reactivate(@PathVariable String provider) {
        return ResponseEntity.ok(integrationStatusRefreshService.normalize(
                integrationManagementService.reactivate(provider)
        ));
    }

    @PostMapping("/{provider}/tokens")
    public ResponseEntity<GenerateIntegrationTokenResponse> generateToken(@PathVariable String provider) {
        return ResponseEntity.ok(integrationManagementService.generateToken(provider));
    }

    @PostMapping("/{provider}/tokens/rotate")
    public ResponseEntity<GenerateIntegrationTokenResponse> rotateToken(@PathVariable String provider) {
        return ResponseEntity.ok(integrationManagementService.generateToken(provider));
    }

    @DeleteMapping("/{provider}/tokens")
    public ResponseEntity<Void> revokeToken(@PathVariable String provider) {
        integrationManagementService.revokeProviderToken(provider);
        return ResponseEntity.noContent().build();
    }
}
