package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.config.PraxisProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
public class CandidatePageRedirectController {

    private final PraxisProperties praxisProperties;

    public CandidatePageRedirectController(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

    @GetMapping("/candidato/{token}")
    public ResponseEntity<Void> redirectCandidatePage(
            @PathVariable String token,
            HttpServletRequest request
    ) {
        String targetBaseUrl = normalizeBaseUrl(praxisProperties.candidatePageBaseUrl());
        String currentBaseUrl = normalizeBaseUrl(ServletUriComponentsBuilder
                .fromRequestUri(request)
                .replacePath(null)
                .replaceQuery(null)
                .build()
                .toUriString());

        if (targetBaseUrl.isBlank() || targetBaseUrl.equalsIgnoreCase(currentBaseUrl)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Pagina do candidato indisponivel neste dominio. Configure PRAXIS_CANDIDATE_PAGE_BASE_URL."
            );
        }

        URI location = UriComponentsBuilder
                .fromHttpUrl(targetBaseUrl)
                .pathSegment("candidato", token)
                .build()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(location)
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
