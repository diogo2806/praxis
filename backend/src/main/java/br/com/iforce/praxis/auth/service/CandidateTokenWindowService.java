package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Mantém o instante canônico da janela de validade do token público de uma tentativa.
 *
 * <p>A tentativa é bloqueada durante a renovação para que chamadas concorrentes reutilizem
 * exatamente o mesmo instante persistido e, consequentemente, produzam o mesmo JWT.</p>
 */
@Service
public class CandidateTokenWindowService {

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final Clock clock;

    public CandidateTokenWindowService(CandidateAttemptRepository candidateAttemptRepository) {
        this(candidateAttemptRepository, Clock.systemUTC());
    }

    CandidateTokenWindowService(CandidateAttemptRepository candidateAttemptRepository, Clock clock) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.clock = clock;
    }

    /**
     * Resolve a janela dentro da transação atual ou abre uma transação quando não houver uma ativa.
     */
    @Transactional
    public Instant currentIssuedAt(String empresaId, String attemptId, int ttlHours) {
        return resolveCurrentIssuedAt(empresaId, attemptId, ttlHours);
    }

    /**
     * Resolve a janela em uma transação própria, inclusive quando o chamador estiver em leitura.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Instant currentIssuedAtInNewTransaction(String empresaId, String attemptId, int ttlHours) {
        return resolveCurrentIssuedAt(empresaId, attemptId, ttlHours);
    }

    private Instant resolveCurrentIssuedAt(String empresaId, String attemptId, int ttlHours) {
        validate(empresaId, attemptId, ttlHours);

        CandidateAttemptEntity entity = candidateAttemptRepository
                .findOneByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));

        Instant issuedAt = entity.getCandidateTokenIssuedAt();
        if (issuedAt == null) {
            throw new IllegalStateException("A tentativa não possui uma janela canônica de token persistida.");
        }

        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Instant expiration = issuedAt.plus(Duration.ofHours(ttlHours));
        if (expiration.isAfter(now)) {
            return issuedAt;
        }

        entity.setCandidateTokenIssuedAt(now);
        CandidateAttemptEntity saved = candidateAttemptRepository.saveAndFlush(entity);
        if (saved.getCandidateTokenIssuedAt() == null) {
            throw new IllegalStateException("Não foi possível persistir a janela canônica do token da tentativa.");
        }
        return saved.getCandidateTokenIssuedAt();
    }

    private void validate(String empresaId, String attemptId, int ttlHours) {
        if (empresaId == null || empresaId.isBlank()) {
            throw new IllegalArgumentException("A empresa da tentativa é obrigatória.");
        }
        if (attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("O identificador da tentativa é obrigatório.");
        }
        if (ttlHours <= 0) {
            throw new IllegalArgumentException("A validade do token deve ser maior que zero.");
        }
    }
}
