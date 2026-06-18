package br.com.iforce.praxis.tenantconfig.controller;

import br.com.iforce.praxis.tenantconfig.dto.ConfigOptionDto;
import br.com.iforce.praxis.tenantconfig.dto.TenantConfigResponse;
import br.com.iforce.praxis.tenantconfig.dto.UpdateConfigOptionRequest;
import br.com.iforce.praxis.tenantconfig.dto.UpdateTenantConfigRequest;
import br.com.iforce.praxis.tenantconfig.model.TenantConfigType;
import br.com.iforce.praxis.tenantconfig.service.TenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
            description = "Substitui integralmente a lista de opcoes de um tipo de configuracao para o tenant autenticado."
    )
    public ResponseEntity<List<ConfigOptionDto>> updateConfig(
            @PathVariable TenantConfigType configType,
            @Valid @RequestBody UpdateTenantConfigRequest request
    ) {
        return ResponseEntity.ok(tenantConfigService.updateConfig(configType, request));
    }

    @GetMapping("/{configType}/all")
    @Operation(
            summary = "Carrega todas as opcoes de um catalogo",
            description = "Retorna todas as opcoes (ativas e inativas) de um tipo de configuracao para o tenant autenticado. Usado pela tela de gerenciamento."
    )
    public ResponseEntity<List<ConfigOptionDto>> getAllConfigOptions(
            @PathVariable TenantConfigType configType
    ) {
        return ResponseEntity.ok(tenantConfigService.getAllConfigOptions(configType));
    }

    @PatchMapping("/{configType}/{optionValue}")
    @Operation(
            summary = "Atualiza uma opcao de configuracao",
            description = "Permite editar ou desativar uma opcao especifica."
    )
    public ResponseEntity<ConfigOptionDto> updateConfigOption(
            @PathVariable TenantConfigType configType,
            @PathVariable String optionValue,
            @Valid @RequestBody UpdateConfigOptionRequest request
    ) {
        return ResponseEntity.ok(tenantConfigService.updateConfigOption(configType, optionValue, request));
    }
}
