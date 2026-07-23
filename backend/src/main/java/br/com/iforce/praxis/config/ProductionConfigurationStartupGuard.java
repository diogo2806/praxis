package br.com.iforce.praxis.config;

import jakarta.annotation.PostConstruct;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Impede que o perfil de produção inicie com fallbacks ou recursos parcialmente configurados. */
@Component
public class ProductionConfigurationStartupGuard {

    private static final String PRODUCTION_PROFILE = "prod";
    private static final int MINIMUM_JWT_SECRET_LENGTH = 32;

    private final Environment environment;

    public ProductionConfigurationStartupGuard(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void verify() {
        if (!isProductionProfileActive()) {
            return;
        }

        List<String> errors = new ArrayList<>();
        validateDatabase(errors);
        validateApplicationUrls(errors);
        validateJwt(errors);
        validateCors(errors);
        validatePrivacy(errors);
        validateEmail(errors);
        validateMercadoPago(errors);
        validateObjectStorage(errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Configuracao de producao invalida: " + String.join("; ", errors));
        }
    }

    private void validateDatabase(List<String> errors) {
        String url = value("spring.datasource.url");
        String username = value("spring.datasource.username");
        String password = value("spring.datasource.password");
        require(errors, "DB_HOST/DB_NAME", url);
        require(errors, "DB_USER/PRAXIS_DATASOURCE_USERNAME", username);
        require(errors, "DB_PASS/PRAXIS_DATASOURCE_PASSWORD", password);
        if (containsIgnoreCase(url, "localhost") || containsIgnoreCase(url, "127.0.0.1")) {
            errors.add("spring.datasource.url nao pode apontar para localhost em producao");
        }
        if ("postgres".equalsIgnoreCase(username) || "postgres".equals(password)) {
            errors.add("credenciais padrao postgres nao podem ser usadas em producao");
        }
    }

    private void validateApplicationUrls(List<String> errors) {
        validateHttpsUrl(errors, "PRAXIS_PUBLIC_BASE_URL", value("praxis.public-base-url"));
        validateHttpsUrl(errors, "PRAXIS_CANDIDATE_PAGE_BASE_URL", value("praxis.candidate-page-base-url"));
    }

    private void validateJwt(List<String> errors) {
        String secret = value("praxis.jwt-secret");
        require(errors, "PRAXIS_JWT_SECRET", secret);
        if (secret != null && secret.length() < MINIMUM_JWT_SECRET_LENGTH) {
            errors.add("PRAXIS_JWT_SECRET deve possuir ao menos 32 caracteres");
        }
    }

    private void validateCors(List<String> errors) {
        String origins = value("praxis.cors.allowed-origins");
        require(errors, "PRAXIS_CORS_ALLOWED_ORIGINS", origins);
        if (hasText(origins)) {
            Arrays.stream(origins.split(","))
                    .map(String::trim)
                    .forEach(origin -> validateHttpsUrl(errors, "PRAXIS_CORS_ALLOWED_ORIGINS", origin));
        }
    }

    private void validatePrivacy(List<String> errors) {
        String controller = value("praxis.privacy.controller-name");
        require(errors, "PRAXIS_PRIVACY_CONTROLLER_NAME", controller);
        if (containsIgnoreCase(controller, "a ser informado")) {
            errors.add("PRAXIS_PRIVACY_CONTROLLER_NAME nao pode manter o placeholder padrao");
        }
        if (!hasText(value("praxis.privacy.service-email")) && !hasText(value("praxis.privacy.service-url"))) {
            errors.add("informe PRAXIS_PRIVACY_SERVICE_EMAIL ou PRAXIS_PRIVACY_SERVICE_URL");
        }
        require(errors, "PRAXIS_PRIVACY_DPO_CONTACT", value("praxis.privacy.dpo-contact"));
    }

    private void validateEmail(List<String> errors) {
        if (!Boolean.parseBoolean(value("praxis.email.enabled"))) {
            return;
        }
        require(errors, "PRAXIS_EMAIL_FROM", value("praxis.email.from"));
        require(errors, "SPRING_MAIL_HOST", value("spring.mail.host"));
        require(errors, "SPRING_MAIL_PORT", value("spring.mail.port"));
        require(errors, "SPRING_MAIL_USERNAME", value("spring.mail.username"));
        require(errors, "SPRING_MAIL_PASSWORD", value("spring.mail.password"));
    }

    private void validateMercadoPago(List<String> errors) {
        if (!Boolean.parseBoolean(value("mp.enabled"))) {
            return;
        }
        validateHttpsUrl(errors, "MP_BASE_URL", value("mp.base-url"));
        require(errors, "MP_ACCESS_TOKEN", value("mp.access-token"));
        require(errors, "MP_PUBLIC_KEY", value("mp.public-key"));
        require(errors, "MP_WEBHOOK_SECRET", value("mp.webhook-secret"));
        validateHttpsUrl(errors, "MP_BACK_URL", value("mp.back-url"));
        validateHttpsUrl(errors, "MP_NOTIFICATION_URL", value("mp.notification-url"));
    }

    private void validateObjectStorage(List<String> errors) {
        String endpoint = value("praxis.object-storage.endpoint");
        if (!hasText(endpoint)) {
            return;
        }
        validateHttpsUrl(errors, "OBJECT_STORAGE_ENDPOINT", endpoint);
        validateHttpsUrl(errors, "OBJECT_STORAGE_PUBLIC_URL", value("praxis.object-storage.public-url"));
        require(errors, "OBJECT_STORAGE_REGION", value("praxis.object-storage.region"));
        require(errors, "OBJECT_STORAGE_ACCESS_KEY", value("praxis.object-storage.access-key"));
        require(errors, "OBJECT_STORAGE_SECRET_KEY", value("praxis.object-storage.secret-key"));
        require(errors, "OBJECT_STORAGE_BUCKET", value("praxis.object-storage.bucket"));
    }

    private void validateHttpsUrl(List<String> errors, String variable, String rawValue) {
        require(errors, variable, rawValue);
        if (!hasText(rawValue)) {
            return;
        }
        try {
            URI uri = URI.create(rawValue.trim());
            if (!uri.isAbsolute() || uri.getHost() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
                errors.add(variable + " deve ser uma URL absoluta HTTPS");
            }
        } catch (IllegalArgumentException exception) {
            errors.add(variable + " deve ser uma URL valida");
        }
    }

    private void require(List<String> errors, String variable, String rawValue) {
        if (!hasText(rawValue)) {
            errors.add(variable + " deve ser informado");
        }
    }

    private String value(String property) {
        return environment.getProperty(property);
    }

    private boolean isProductionProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(PRODUCTION_PROFILE::equals);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean containsIgnoreCase(String value, String expected) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }
}
