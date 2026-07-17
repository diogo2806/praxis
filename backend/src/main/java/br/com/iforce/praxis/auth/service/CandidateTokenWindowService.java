package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Mantém a janela canônica de validade do token público de uma tentativa.
 * Leituras nunca renovam um link expirado; a extensão depende de comando explícito da empresa.
 */
@Service
public class CandidateTokenWindowService {

    private static final int MAX_EXTENSION_DAYS = 365;

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final Clock clock;

    @Autowired
    public CandidateTokenWindowService(CandidateAttemptRepository candidateAttemptRepository) {
        this(candidateAttemptRepository, Clock.systemUTC());
    }

    CandidateTokenWindowService(CandidateAttemptRepository candidateAttemptRepository, Clock clock) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CandidateTokenWindow currentWindow(String empresaId, String attemptId, int ttlHours) {
        validate(empresaId, attemptId, ttlHours);
        CandidateAttemptEntity entity = candidateAttemptRepository
                .findByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        return window(entity, ttlHours);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public CandidateTokenWindow currentWindowInNewTransaction(String empresaId, String attemptId, int ttlHours) {
        return currentWindow(empresaId, attemptId, ttlHours);
    }

    public Instant currentIssuedAt(String empresaId, String attemptId, int ttlHours) {
        return currentWindow(empresaId, attemptId, ttlHours).issuedAt();
    }

    public Instant currentIssuedAtInNewTransaction(String empresaId, String attemptId, int ttlHours) {
        return currentWindowInNewTransaction(empresaId, attemptId, ttlHours).issuedAt();
    }

    /**
     * Soma dias ao vencimento atual. Quando já expirado, inicia uma nova janela a partir de agora.
     */
    @Transactional
    public CandidateTokenWindow extendValidity(
            String empresaId,
            String attemptId,
            int ttlHours,
            int additionalDays
    ) {
        validate(empresaId, attemptId, ttlHours);
        if (additionalDays < 1 || additionalDays > MAX_EXTENSION_DAYS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe uma quantidade entre 1 e 365 dias."
            );
        }

        CandidateAttemptEntity entity = candidateAttemptRepository
                .findOneByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        CandidateTokenWindow current = window(entity, ttlHours);
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Instant extensionBase = current.expiresAt().isAfter(now) ? current.expiresAt() : now;

        entity.setCandidateTokenIssuedAt(now);
        entity.setCandidateTokenExpiresAt(extensionBase.plus(additionalDays, ChronoUnit.DAYS));
        CandidateAttemptEntity saved = candidateAttemptRepository.saveAndFlush(entity);
        return window(saved, ttlHours);
    }

    public boolean isExpired(CandidateTokenWindow window) {
        return !window.expiresAt().isAfter(Instant.now(clock));
    }

    private CandidateTokenWindow window(CandidateAttemptEntity entity, int ttlHours) {
        Instant issuedAt = entity.getCandidateTokenIssuedAt();
        if (issuedAt == null) {
            throw new IllegalStateException("A tentativa não possui emissão canônica do token persistida.");
        }
        Instant expiresAt = entity.getCandidateTokenExpiresAt();
        if (expiresAt == null) {
            expiresAt = issuedAt.plusSeconds(ttlHours * 60L * 60L);
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalStateException("A expiração do link deve ser posterior à emissão.");
        }
        return new CandidateTokenWindow(issuedAt, expiresAt);
    }

    private void validate(String empresaId, String attemptId, int ttlHours) {
        if (empresaId == null || empresaId.isBlank()) {
            throw new IllegalArgumentException("A empresa da tentativa é obrigatória.");
        }
        if (attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("O identificador da tentativa é obrigatório.");
        }
        if (ttlHours <= 0) {
            throw new IllegalArgumentException("A validade padrão do token deve ser maior que zero.");
        }
    }

    public record CandidateTokenWindow(Instant issuedAt, Instant expiresAt) {
    }
}
