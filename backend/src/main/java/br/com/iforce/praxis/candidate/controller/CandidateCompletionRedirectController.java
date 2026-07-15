package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@RestController
@RequestMapping("/candidate/attempts")
public class CandidateCompletionRedirectController {

    private final JwtService jwtService;
    private final CandidateAttemptRepository candidateAttemptRepository;

    public CandidateCompletionRedirectController(
            JwtService jwtService,
            CandidateAttemptRepository candidateAttemptRepository
    ) {
        this.jwtService = jwtService;
        this.candidateAttemptRepository = candidateAttemptRepository;
    }

    @GetMapping("/{attemptToken}/redirect")
    public ResponseEntity<Void> redirectAfterCompletion(@PathVariable String attemptToken) {
        JwtService.CandidateAttemptToken token = parseToken(attemptToken);
        CandidateAttemptEntity attempt = candidateAttemptRepository
                .findByEmpresaIdAndId(token.empresaId(), token.attemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));

        if (attempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A avaliação ainda não foi concluída.");
        }
        if (attempt.getCallbackUrl() == null || attempt.getCallbackUrl().isBlank()) {
            return ResponseEntity.noContent().build();
        }

        URI callbackUrl = URI.create(attempt.getCallbackUrl());
        String scheme = callbackUrl.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || callbackUrl.getHost() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "callback_url armazenada é inválida.");
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(callbackUrl).build();
    }

    private JwtService.CandidateAttemptToken parseToken(String token) {
        try {
            return jwtService.parseCandidateAttemptToken(token);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token público de candidato inválido.");
        }
    }
}
