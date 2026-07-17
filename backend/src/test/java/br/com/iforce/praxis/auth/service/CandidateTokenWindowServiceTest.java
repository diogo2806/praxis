package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateTokenWindowServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-17T14:00:00Z");

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    @Test
    void reusesPersistedIssuedAtWhileWindowIsValid() {
        Instant issuedAt = NOW.minusSeconds(60);
        CandidateAttemptEntity entity = attempt(issuedAt);
        when(candidateAttemptRepository.findOneByEmpresaIdAndId("empresa-1", "att-1"))
                .thenReturn(Optional.of(entity));

        Instant resolved = service().currentIssuedAt("empresa-1", "att-1", 1);

        assertThat(resolved).isEqualTo(issuedAt);
        verify(candidateAttemptRepository, never()).saveAndFlush(entity);
    }

    @Test
    void persistsSingleCanonicalIssuedAtWhenWindowExpired() {
        CandidateAttemptEntity entity = attempt(NOW.minusSeconds(3600));
        when(candidateAttemptRepository.findOneByEmpresaIdAndId("empresa-1", "att-1"))
                .thenReturn(Optional.of(entity));
        when(candidateAttemptRepository.saveAndFlush(entity)).thenReturn(entity);

        Instant resolved = service().currentIssuedAt("empresa-1", "att-1", 1);

        assertThat(resolved).isEqualTo(NOW);
        assertThat(entity.getCandidateTokenIssuedAt()).isEqualTo(NOW);
        verify(candidateAttemptRepository).saveAndFlush(entity);
    }

    @Test
    void failsWhenAttemptDoesNotExist() {
        when(candidateAttemptRepository.findOneByEmpresaIdAndId("empresa-1", "att-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().currentIssuedAt("empresa-1", "att-1", 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }

    @Test
    void failsWhenCanonicalIssuedAtIsMissing() {
        CandidateAttemptEntity entity = attempt(null);
        when(candidateAttemptRepository.findOneByEmpresaIdAndId("empresa-1", "att-1"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service().currentIssuedAt("empresa-1", "att-1", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("janela canônica");
    }

    private CandidateTokenWindowService service() {
        return new CandidateTokenWindowService(
                candidateAttemptRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private CandidateAttemptEntity attempt(Instant issuedAt) {
        CandidateAttemptEntity entity = new CandidateAttemptEntity();
        entity.setId("att-1");
        entity.setEmpresaId("empresa-1");
        entity.setCandidateTokenIssuedAt(issuedAt);
        return entity;
    }
}
