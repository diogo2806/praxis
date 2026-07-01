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


/**
 * Encaminha o candidato para a página correta da avaliação.
 *
 * <p>Na visão do processo, quando o candidato abre o link curto que recebeu
 * (por e-mail ou WhatsApp), este componente o redireciona automaticamente
 * para o endereço da página do candidato configurado para o ambiente. Serve
 * para que o link enviado seja sempre simples e estável, independentemente
 * de onde a tela do candidato está hospedada.</p>
 */
@RestController
public class CandidatePageRedirectController {

    private final PraxisProperties praxisProperties;

    public CandidatePageRedirectController(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

    /**
     * Redireciona o acesso ao link do candidato para a página correta.
     *
     * <p>Recebe o token do candidato e devolve um redirecionamento para a
     * página oficial da avaliação. Se o endereço de destino não estiver
     * configurado, informa que a página está indisponível.</p>
     *
     * @param token identificador único do convite do candidato
     * @param request a requisição recebida (usada para descobrir o domínio atual)
     * @return um redirecionamento para a página do candidato
     */
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

    /**
     * Padroniza o endereço base removendo a barra final, para comparar e
     * montar URLs de forma consistente. Uso interno.
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
