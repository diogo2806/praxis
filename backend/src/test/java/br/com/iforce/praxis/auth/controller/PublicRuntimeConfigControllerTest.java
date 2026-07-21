package br.com.iforce.praxis.auth.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicRuntimeConfigControllerTest {

    @Test
    void exposesDevelopmentEmpresaWhenSecurityIsDisabled() {
        PublicRuntimeConfigController controller = new PublicRuntimeConfigController(
                false,
                true,
                "empresa-dev"
        );

        PublicRuntimeConfigController.PublicRuntimeConfigResponse response = controller.getRuntimeConfig();

        assertThat(response.securityEnabled()).isFalse();
        assertThat(response.partnerModuleEnabled()).isTrue();
        assertThat(response.defaultEmpresaId()).isEqualTo("empresa-dev");
    }

    @Test
    void doesNotExposeDefaultEmpresaWhenSecurityIsEnabled() {
        PublicRuntimeConfigController controller = new PublicRuntimeConfigController(
                true,
                false,
                "empresa-producao"
        );

        PublicRuntimeConfigController.PublicRuntimeConfigResponse response = controller.getRuntimeConfig();

        assertThat(response.securityEnabled()).isTrue();
        assertThat(response.partnerModuleEnabled()).isFalse();
        assertThat(response.defaultEmpresaId()).isNull();
    }
}
