package br.com.iforce.praxis.shared.integration;

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

@RestController
@RequestMapping("/api/v1/integrations/tokens")
public class IntegrationTokenAdminController {

    private final IntegrationTokenAdminService integrationTokenAdminService;

    public IntegrationTokenAdminController(IntegrationTokenAdminService integrationTokenAdminService) {
        this.integrationTokenAdminService = integrationTokenAdminService;
    }

    @GetMapping
    public ResponseEntity<List<IntegrationTokenResponse>> listTokens() {
        return ResponseEntity.ok(integrationTokenAdminService.listTokens());
    }

    @PostMapping("/{provider}/rotate")
    public ResponseEntity<RotateIntegrationTokenResponse> rotateToken(@PathVariable String provider) {
        return ResponseEntity.ok(integrationTokenAdminService.rotateToken(provider));
    }

    @DeleteMapping("/{provider}")
    public ResponseEntity<Void> revokeToken(@PathVariable String provider) {
        integrationTokenAdminService.revokeToken(provider);
        return ResponseEntity.noContent().build();
    }
}
