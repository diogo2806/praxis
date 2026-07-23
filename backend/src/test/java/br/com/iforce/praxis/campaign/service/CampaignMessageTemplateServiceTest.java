package br.com.iforce.praxis.campaign.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignMessageTemplateServiceTest {

    private final CampaignMessageTemplateService service = new CampaignMessageTemplateService();

    @Test
    void shouldRenderOnlyWhitelistedVariables() {
        var rendered = service.renderMessage(
                "Convite {{campaign}}",
                "Olá {{name}}, use {{link}} até {{expiresAt}}.",
                "Ana",
                "ana@example.com",
                "https://praxis.example.com/candidato/token",
                "Campanha Java",
                "Avaliação Java",
                Instant.parse("2026-08-01T12:00:00Z")
        );

        assertThat(rendered.subject()).isEqualTo("Convite Campanha Java");
        assertThat(rendered.body())
                .contains("Olá Ana")
                .contains("https://praxis.example.com/candidato/token")
                .contains("2026-08-01T12:00:00Z");
    }

    @Test
    void shouldRejectUnknownVariable() {
        assertThatThrownBy(() -> service.validate(
                "Convite {{campaign}}",
                "Use {{link}} e revele {{secret}}."
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Variáveis não permitidas");
    }

    @Test
    void shouldRequireCandidateLinkInBody() {
        assertThatThrownBy(() -> service.validate(
                "Convite {{campaign}}",
                "Olá {{name}}, sua avaliação está disponível."
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("{{link}}");
    }
}
