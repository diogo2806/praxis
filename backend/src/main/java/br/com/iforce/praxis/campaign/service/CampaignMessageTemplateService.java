package br.com.iforce.praxis.campaign.service;

import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.MessagePreviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CampaignMessageTemplateService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}", Pattern.MULTILINE);
    private static final Set<String> ALLOWED_VARIABLES = Set.of(
            "name",
            "email",
            "link",
            "campaign",
            "simulation",
            "expiresAt"
    );

    public void validate(String subject, String body) {
        if (subject == null || subject.isBlank() || body == null || body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assunto e corpo da mensagem são obrigatórios.");
        }
        Set<String> unknown = new LinkedHashSet<>();
        findVariables(subject).stream().filter(variable -> !ALLOWED_VARIABLES.contains(variable)).forEach(unknown::add);
        findVariables(body).stream().filter(variable -> !ALLOWED_VARIABLES.contains(variable)).forEach(unknown::add);
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Variáveis não permitidas no modelo: " + String.join(", ", unknown) + "."
            );
        }
        if (!findVariables(body).contains("link")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O corpo da mensagem deve incluir a variável {{link}}.");
        }
    }

    public MessagePreviewResponse preview(String subject, String body, String sampleName, String campaignName) {
        validate(subject, body);
        Map<String, String> values = Map.of(
                "name", sampleName,
                "email", "candidato@example.com",
                "link", "https://praxis.example.com/candidato/exemplo",
                "campaign", campaignName,
                "simulation", "Avaliação de exemplo",
                "expiresAt", Instant.now().plusSeconds(7L * 24L * 3600L).toString()
        );
        return new MessagePreviewResponse(
                render(subject, values),
                render(body, values),
                findVariables(subject + "\n" + body).stream().sorted().toList()
        );
    }

    public RenderedMessage renderMessage(
            String subject,
            String body,
            String name,
            String email,
            String link,
            String campaign,
            String simulation,
            Instant expiresAt
    ) {
        validate(subject, body);
        Map<String, String> values = Map.of(
                "name", safe(name),
                "email", safe(email),
                "link", safe(link),
                "campaign", safe(campaign),
                "simulation", safe(simulation),
                "expiresAt", expiresAt == null ? "" : expiresAt.toString()
        );
        return new RenderedMessage(render(subject, values), render(body, values));
    }

    public Set<String> findVariables(String template) {
        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template == null ? "" : template);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    private String render(String template, Map<String, String> values) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String replacement = values.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record RenderedMessage(String subject, String body) {
    }
}
