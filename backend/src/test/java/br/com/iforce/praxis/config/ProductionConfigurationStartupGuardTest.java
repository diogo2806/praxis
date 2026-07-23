package br.com.iforce.praxis.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationStartupGuardTest {

    @Test
    void ignoresIncompleteConfigurationOutsideProduction() {
        ProductionConfigurationStartupGuard guard = new ProductionConfigurationStartupGuard(new MockEnvironment());

        assertThatCode(guard::verify).doesNotThrowAnyException();
    }

    @Test
    void acceptsCompleteProductionConfigurationWithOptionalResourcesDisabled() {
        MockEnvironment environment = validProductionEnvironment();

        ProductionConfigurationStartupGuard guard = new ProductionConfigurationStartupGuard(environment);

        assertThatCode(guard::verify).doesNotThrowAnyException();
    }

    @Test
    void reportsAllMissingCoreProductionVariables() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/postgres");
        environment.setProperty("spring.datasource.username", "postgres");
        environment.setProperty("spring.datasource.password", "postgres");
        environment.setProperty("praxis.public-base-url", "http://localhost:8080");
        environment.setProperty("praxis.candidate-page-base-url", "http://localhost:8080");
        environment.setProperty("praxis.jwt-secret", "curto");
        environment.setProperty("praxis.cors.allowed-origins", "");
        environment.setProperty("praxis.privacy.controller-name", "Controlador a ser informado pelo empresa");
        environment.setProperty("praxis.privacy.service-email", "");
        environment.setProperty("praxis.privacy.service-url", "");
        environment.setProperty("praxis.privacy.dpo-contact", "");

        ProductionConfigurationStartupGuard guard = new ProductionConfigurationStartupGuard(environment);

        assertThatThrownBy(guard::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost")
                .hasMessageContaining("PRAXIS_JWT_SECRET")
                .hasMessageContaining("PRAXIS_CORS_ALLOWED_ORIGINS")
                .hasMessageContaining("PRAXIS_PRIVACY_CONTROLLER_NAME")
                .hasMessageContaining("PRAXIS_PRIVACY_DPO_CONTACT");
    }

    @Test
    void requiresSmtpOnlyWhenEmailIsEnabled() {
        MockEnvironment environment = validProductionEnvironment();
        environment.setProperty("praxis.email.enabled", "true");

        ProductionConfigurationStartupGuard guard = new ProductionConfigurationStartupGuard(environment);

        assertThatThrownBy(guard::verify)
                .hasMessageContaining("SPRING_MAIL_HOST")
                .hasMessageContaining("SPRING_MAIL_PORT")
                .hasMessageContaining("SPRING_MAIL_USERNAME")
                .hasMessageContaining("SPRING_MAIL_PASSWORD");
    }

    @Test
    void requiresMercadoPagoConfigurationOnlyWhenEnabled() {
        MockEnvironment environment = validProductionEnvironment();
        environment.setProperty("mp.enabled", "true");
        environment.setProperty("mp.base-url", "https://api.mercadopago.com");

        ProductionConfigurationStartupGuard guard = new ProductionConfigurationStartupGuard(environment);

        assertThatThrownBy(guard::verify)
                .hasMessageContaining("MP_ACCESS_TOKEN")
                .hasMessageContaining("MP_PUBLIC_KEY")
                .hasMessageContaining("MP_WEBHOOK_SECRET")
                .hasMessageContaining("MP_NOTIFICATION_URL");
    }

    @Test
    void requiresCompleteObjectStorageWhenEndpointEnablesUploads() {
        MockEnvironment environment = validProductionEnvironment();
        environment.setProperty("praxis.object-storage.endpoint", "https://storage.example.com");
        environment.setProperty("praxis.object-storage.region", "us-east-1");
        environment.setProperty("praxis.object-storage.bucket", "praxis-media");

        ProductionConfigurationStartupGuard guard = new ProductionConfigurationStartupGuard(environment);

        assertThatThrownBy(guard::verify)
                .hasMessageContaining("OBJECT_STORAGE_PUBLIC_URL")
                .hasMessageContaining("OBJECT_STORAGE_ACCESS_KEY")
                .hasMessageContaining("OBJECT_STORAGE_SECRET_KEY");
    }

    private MockEnvironment validProductionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("spring.datasource.url", "jdbc:postgresql://database.internal:5432/praxis");
        environment.setProperty("spring.datasource.username", "praxis_app");
        environment.setProperty("spring.datasource.password", "senha-segura-producao");
        environment.setProperty("praxis.public-base-url", "https://praxis.example.com");
        environment.setProperty("praxis.candidate-page-base-url", "https://praxis.example.com/candidate");
        environment.setProperty("praxis.jwt-secret", "segredo-jwt-de-producao-com-mais-de-32-caracteres");
        environment.setProperty("praxis.cors.allowed-origins", "https://praxis.example.com");
        environment.setProperty("praxis.privacy.controller-name", "Empresa Exemplo LTDA");
        environment.setProperty("praxis.privacy.service-email", "privacidade@example.com");
        environment.setProperty("praxis.privacy.dpo-contact", "dpo@example.com");
        environment.setProperty("praxis.email.enabled", "false");
        environment.setProperty("mp.enabled", "false");
        return environment;
    }
}
