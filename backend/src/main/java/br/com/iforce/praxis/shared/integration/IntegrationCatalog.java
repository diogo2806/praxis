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
                    "Recebe candidatos da Gupy automaticamente e devolve o resultado da avaliação ao processo seletivo correspondente.",
                    IntegrationType.ATS,
                    "gupy",
                    false
            ),
            new Definition(
                    IntegrationProvider.RECRUTEI,
                    "Recrutei",
                    "Recebe candidatos do Recrutei e devolve o resultado da avaliação concluída ao processo seletivo.",
                    IntegrationType.ATS,
                    "recrutei",
                    false
            ),
            new Definition(
                    IntegrationProvider.CUSTOM_API,
                    "API própria",
                    "Conecte sistemas internos via token de API e webhooks. Consulte a documentação para os endpoints disponíveis.",
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
