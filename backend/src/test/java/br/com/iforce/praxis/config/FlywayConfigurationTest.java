package br.com.iforce.praxis.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class FlywayConfigurationTest {

    @Test
    void deveDesabilitarMigrationsForaDeOrdemPorPadrao() throws IOException {
        Properties properties = loadProperties("application.properties");

        assertEquals(
                "${SPRING_FLYWAY_OUT_OF_ORDER:false}",
                properties.getProperty("spring.flyway.out-of-order"));
    }

    @Test
    void deveForcarMigrationsForaDeOrdemDesabilitadasEmProducao() throws IOException {
        Properties properties = loadProperties("application-prod.properties");

        assertEquals("false", properties.getProperty("spring.flyway.out-of-order"));
    }

    private Properties loadProperties(String resourceName) throws IOException {
        Properties properties = new Properties();

        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Recurso não encontrado: " + resourceName);
            }
            properties.load(inputStream);
        }

        return properties;
    }
}
