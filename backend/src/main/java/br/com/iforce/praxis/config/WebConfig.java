package br.com.iforce.praxis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] DEFAULT_ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173"
    };

    private final String[] allowedOrigins;

    public WebConfig(@Value("${praxis.cors.allowed-origins:}") String configuredAllowedOrigins) {
        this.allowedOrigins = parseAllowedOrigins(configuredAllowedOrigins);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    private static String[] parseAllowedOrigins(String configuredAllowedOrigins) {
        if (configuredAllowedOrigins == null || configuredAllowedOrigins.isBlank()) {
            return DEFAULT_ALLOWED_ORIGINS;
        }

        String[] origins = Arrays.stream(configuredAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);

        return origins.length == 0 ? DEFAULT_ALLOWED_ORIGINS : origins;
    }
}
