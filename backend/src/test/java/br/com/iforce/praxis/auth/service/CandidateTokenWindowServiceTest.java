package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.auth.service.CandidateTokenWindowService.CandidateTokenWindow;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateTokenWindowServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-17T14:00:00Z");

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    @Test
    void readingExpiredWindowDoesNotRenewIt() {
        CandidateAttemptEntity entity = attempt(
                NOW.minusSeconds(8 * 24 * 60 * 60L),
                NOW.minusSeconds(24 * 60 * 60L)
        );
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "att-1"))
                .thenReturn(Optional.of(entity));

        CandidateTokenWindow window = service().currentWindow("empresa-1", "att-1", 168);

        assertThat(window.expiresAt()).isBefore(NOW);
        assertThat(service().isExpired(window)).isTrue();
        verify(candidateAttemptRepository, never()).saveAndFlush(entity);
    }

    @Test
    void extendingExpiredWindowReactivatesFromNow() {
        CandidateAttemptEntity entity = attempt(
                NOW.minusSeconds(8 * 24 * 60 * 60L),
                NOW.minusSeconds(24 * 60 * 60L)
        );
        when(candidateAttemptRepository.findOneByEmpresaIdAndId("empresa-1", "att-1"))
                .thenReturn(Optional.of(entity));
        when(candidateAttemptRepository.saveAndFlush(entity)).thenReturn(entity);

        CandidateTokenWindow window = service().extendValidity("empresa-1", "att-1", 168, 7);

        assertThat(window.issuedAt()).isEqualTo(NOW);
        assertThat(window.expiresAt()).isEqualTo(NOW.plusSeconds(7 * 24 * 60 * 60L));
        verify(candidateAttemptRepository).saveAndFlush(entity);
    }

    @Test
    void extendingActiveWindowAddsDaysToCurrentExpiration() {
        Instant currentExpiration = NOW.plusSeconds(2 * 24 * 60 * 60L);
        CandidateAttemptEntity entity = attempt(NOW.minusSeconds(60), currentExpiration);
        when(candidateAttemptRepository.findOneByEmpresaIdAndId("empresa-1", "att-1"))
                .thenReturn(Optional.of(entity));
        when(candidateAttemptRepository.saveAndFlush(entity)).thenReturn(entity);

        CandidateTokenWindow window = service().extendValidity("empresa-1", "att-1", 168, 3);

        assertThat(window.expiresAt()).isEqualTo(currentExpiration.plusSeconds(3 * 24 * 60 * 60L));
    }

    private CandidateTokenWindowService service() {
        return new CandidateTokenWindowService(
                candidateAttemptRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private CandidateAttemptEntity attempt(Instant issuedAt, Instant expiresAt) {
        CandidateAttemptEntity entity = new CandidateAttemptEntity();
        entity.setId("att-1");
        entity.setEmpresaId("empresa-1");
        entity.setCandidateTokenIssuedAt(issuedAt);
        entity.setCandidateTokenExpiresAt(expiresAt);
        return entity;
    }
}
