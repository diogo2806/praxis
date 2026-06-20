package br.com.iforce.praxis.tenantconfig.controller;

import br.com.iforce.praxis.tenantconfig.dto.ConfigOptionDto;
import br.com.iforce.praxis.tenantconfig.dto.TenantConfigResponse;
import br.com.iforce.praxis.tenantconfig.dto.UpdateTenantConfigRequest;
import br.com.iforce.praxis.tenantconfig.model.TenantConfigType;
import br.com.iforce.praxis.tenantconfig.service.TenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant-config")
@Tag(name = "Tenant Config", description = "Catalogos configuraveis por empresa (competencias, senioridade, checklist, usos e tempos).")
public class TenantConfigController {

    private final TenantConfigService tenantConfigService;

    public TenantConfigController(TenantConfigService tenantConfigService) {
        this.tenantConfigService = tenantConfigService;
    }

    @GetMapping
    @Operation(
            summary = "Carrega catalogos do tenant",
            description = "Retorna competencias, niveis de senioridade, checklist de linguagem, usos do resultado e limites de tempo. Tipos nao customizados caem nos padroes embutidos."
    )
    public ResponseEntity<TenantConfigResponse> getConfig() {
        return ResponseEntity.ok(tenantConfigService.getConfig());
    }

    @PutMapping("/{configType}")
    @Operation(
            summary = "Customiza um catalogo do tenant",
            description = "Substitui integralmente a lista de opcoes de um tipo de configuracao para a empresa."
    )
    public ResponseEntity<List<ConfigOptionDto>> updateConfig(
            @PathVariable TenantConfigType configType,
            @Valid @RequestBody UpdateTenantConfigRequest request
    ) {
        return ResponseEntity.ok(tenantConfigService.updateConfig(configType, request));
    }

}
