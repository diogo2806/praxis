package br.com.iforce.praxis.tenantconfig.controller;

import br.com.iforce.praxis.tenantconfig.dto.ConfigOptionDto;

import br.com.iforce.praxis.tenantconfig.dto.EmpresaConfigResponse;

import br.com.iforce.praxis.tenantconfig.dto.UpdateEmpresaConfigRequest;

import br.com.iforce.praxis.tenantconfig.model.EmpresaConfigType;

import br.com.iforce.praxis.tenantconfig.service.EmpresaConfigService;

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


/**
 * Porta de entrada (API) dos catálogos configuráveis por empresa.
 *
 * <p>Na visão do processo, é por aqui que a empresa personaliza as listas
 * usadas nas telas de autoria de provas — por exemplo, o catálogo de
 * competências e os limites de tempo. Quando a empresa não personaliza, o
 * sistema usa valores padrão. Permite consultar e substituir esses
 * catálogos.</p>
 */
@RestController
@RequestMapping("/api/v1/empresa-config")
@Tag(name = "Configuração da empresa", description = "Catalogos configuraveis por empresa usados nas telas de autoria.")
public class EmpresaConfigController {

    private final EmpresaConfigService empresaConfigService;

    public EmpresaConfigController(EmpresaConfigService empresaConfigService) {
        this.empresaConfigService = empresaConfigService;
    }

    /**
     * Carrega os catálogos configuráveis da empresa logada.
     *
     * <p>Traz as competências e os limites de tempo; tipos não personalizados
     * vêm com os valores padrão.</p>
     *
     * @return os catálogos da empresa para as telas de autoria
     */
    @GetMapping
    @Operation(
            summary = "Carrega catálogos da empresa",
            description = "Retorna competências e limites de tempo. Tipos não customizados usam os padrões embutidos."
    )
    public ResponseEntity<EmpresaConfigResponse> getConfig() {
        return ResponseEntity.ok(empresaConfigService.getConfig());
    }

    /**
     * Personaliza um catálogo da empresa, substituindo a lista por inteiro.
     *
     * @param configType o tipo de catálogo (ex.: competências, limites de tempo)
     * @param request a nova lista completa de opções
     * @return a lista de opções resultante
     */
    @PutMapping("/{configType}")
    @Operation(
            summary = "Customiza um catálogo da empresa",
            description = "Substitui integralmente a lista de opcoes de um tipo de configuracao para a empresa."
    )
    public ResponseEntity<List<ConfigOptionDto>> updateConfig(
            @PathVariable EmpresaConfigType configType,
            @Valid @RequestBody UpdateEmpresaConfigRequest request
    ) {
        return ResponseEntity.ok(empresaConfigService.updateConfig(configType, request));
    }

}
