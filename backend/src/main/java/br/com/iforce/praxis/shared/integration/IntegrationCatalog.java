package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class IntegrationCatalog {

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition(
                    IntegrationProvider.GUPY,
                    "Gupy",
                    "Receba candidatos e envie resultados automaticamente.",
                    IntegrationType.ATS,
                    "gupy",
                    true
            ),
            new Definition(
                    IntegrationProvider.RECRUTEI,
                    "Recrutei",
                    "Conecte o Práxis ao Recrutei para automatizar avaliações.",
                    IntegrationType.ATS,
                    "recrutei",
                    true
            ),
            new Definition(
                    IntegrationProvider.CUSTOM_API,
                    "API própria",
                    "Use esta opção para conectar sistemas internos ao Práxis.",
                    IntegrationType.API,
                    "custom_api",
                    false
            )
    );

    private IntegrationCatalog() {
    }

    public static List<Definition> definitions() {
        return DEFINITIONS;
    }

    public static Definition requireDefinition(String provider) {
        IntegrationProvider normalized = normalize(provider);
        return DEFINITIONS.stream()
                .filter(definition -> definition.provider() == normalized)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Provedor de integração não suportado."
                ));
    }

    public static IntegrationProvider normalize(String provider) {
        String normalized = provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(IntegrationProvider.values())
                .filter(value -> value.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Provedor de integração não suportado."
                ));
    }

    public record Definition(
            IntegrationProvider provider,
            String name,
            String description,
            IntegrationType type,
            String tokenProvider,
            boolean supportsManualSync
    ) {
    }
}
