package br.com.iforce.praxis.simulation.controller;

import org.junit.jupiter.api.Test;

import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confirma, sem subir contexto Spring, que a rota GET
 * {@code /{simulationId}/versions/{versionNumber}/calibration} continua
 * mapeada em {@link SimulationAdminController}. Não depende de banco (a
 * suíte @SpringBootTest do controller ainda não roda no CI, ver ci.yml),
 * então serve como rede de segurança rápida contra a rota sumir
 * silenciosamente numa refatoração.
 */
class SimulationAdminControllerRouteRegressionTest {

    @Test
    void calibrationRouteIsStillMapped() throws NoSuchMethodException {
        Method method = SimulationAdminController.class.getMethod(
                "getCalibrationReport", String.class, int.class);

        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/{simulationId}/versions/{versionNumber}/calibration");
    }
}
