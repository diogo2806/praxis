package br.com.iforce.praxis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] DEFAULT_LOCAL_ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173"
    };
    private static final Set<String> LOCAL_PROFILES = Set.of("dev", "local", "test");

    private final String[] allowedOrigins;

    public WebConfig(
            @Value("${praxis.cors.allowed-origins:}") String configuredAllowedOrigins,
            Environment environment
    ) {
        this.allowedOrigins = parseAllowedOrigins(configuredAllowedOrigins, environment);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "Idempotency-Key", "X-Request-Id")
                .exposedHeaders("Location", "X-Request-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }

    private static String[] parseAllowedOrigins(String configuredAllowedOrigins, Environment environment) {
        if (configuredAllowedOrigins == null || configuredAllowedOrigins.isBlank()) {
            return hasOnlyLocalProfiles(environment)
                    ? DEFAULT_LOCAL_ALLOWED_ORIGINS.clone()
                    : new String[0];
        }

        return Arrays.stream(configuredAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .map(WebConfig::validateOrigin)
                .distinct()
                .toArray(String[]::new);
    }

    private static String validateOrigin(String origin) {
        if (origin.contains("*")) {
            throw new IllegalArgumentException("CORS não aceita origem curinga.");
        }

        URI uri = URI.create(origin);
        if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Origem CORS inválida: " + origin);
        }

        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        boolean localHost = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1");
        if (!scheme.equals("https") && !(scheme.equals("http") && localHost)) {
            throw new IllegalArgumentException("Origem CORS deve usar HTTPS, exceto localhost.");
        }

        return origin;
    }

    private static boolean hasOnlyLocalProfiles(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0
                && Arrays.stream(activeProfiles)
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .allMatch(LOCAL_PROFILES::contains);
    }
}
