package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.shared.integration.dto.GenerateIntegrationTokenResponse;
import br.com.iforce.praxis.shared.integration.dto.IntegrationTokenResponse;
import br.com.iforce.praxis.shared.integration.dto.RotateIntegrationTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * Endpoint de compatibilidade para clientes administrativos antigos.
 *
 * <p>Listagens continuam consultando a fonte real de autenticação. Rotação e
 * revogação, porém, delegam ao mesmo caso de uso transacional utilizado pela
 * Central de Integrações, evitando fluxos concorrentes entre as tabelas.</p>
 */
@RestController
@RequestMapping("/api/v1/integrations/tokens")
public class IntegrationTokenAdminController {

    private final IntegrationTokenAdminService integrationTokenAdminService;
    private final IntegrationManagementService integrationManagementService;

    public IntegrationTokenAdminController(
            IntegrationTokenAdminService integrationTokenAdminService,
            IntegrationManagementService integrationManagementService
    ) {
        this.integrationTokenAdminService = integrationTokenAdminService;
        this.integrationManagementService = integrationManagementService;
    }

    @GetMapping
    public ResponseEntity<List<IntegrationTokenResponse>> listTokens() {
        return ResponseEntity.ok(integrationTokenAdminService.listTokens());
    }

    @PostMapping("/{provider}/rotate")
    public ResponseEntity<RotateIntegrationTokenResponse> rotateToken(@PathVariable String provider) {
        GenerateIntegrationTokenResponse generated = integrationManagementService.generateToken(provider);
        return ResponseEntity.ok(new RotateIntegrationTokenResponse(
                generated.provider().toLowerCase(Locale.ROOT),
                true,
                generated.createdAt(),
                generated.token()
        ));
    }

    @DeleteMapping("/{provider}")
    public ResponseEntity<Void> revokeToken(@PathVariable String provider) {
        integrationManagementService.revokeProviderToken(provider);
        return ResponseEntity.noContent().build();
    }
}
