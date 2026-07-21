package br.com.iforce.praxis.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runtime-config")
public class PublicRuntimeConfigController {

    private final boolean securityEnabled;
    private final boolean partnerModuleEnabled;
    private final String defaultEmpresaId;

    public PublicRuntimeConfigController(
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.partner.enabled:false}") boolean partnerModuleEnabled,
            @Value("${praxis.default-empresa-id:empresa-1}") String defaultEmpresaId
    ) {
        this.securityEnabled = securityEnabled;
        this.partnerModuleEnabled = partnerModuleEnabled;
        this.defaultEmpresaId = defaultEmpresaId;
    }

    @GetMapping
    public PublicRuntimeConfigResponse getRuntimeConfig() {
        return new PublicRuntimeConfigResponse(
                securityEnabled,
                partnerModuleEnabled,
                securityEnabled ? null : defaultEmpresaId
        );
    }

    public record PublicRuntimeConfigResponse(
            boolean securityEnabled,
            boolean partnerModuleEnabled,
            String defaultEmpresaId
    ) {
    }
}
