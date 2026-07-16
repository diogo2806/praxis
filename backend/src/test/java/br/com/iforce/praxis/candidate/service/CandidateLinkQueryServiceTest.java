package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;
import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.candidate.dto.CandidateLinkPageResponse;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateLinkQueryServiceTest {

    private static final Instant ATTEMPT_CREATED_AT = Instant.parse("2026-07-15T20:00:00Z");

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private SimulationCatalogService simulationCatalogService;
    @Mock
    private JwtService jwtService;

    private CandidateLinkQueryService service;

    @BeforeEach
    void setUp() {
        EmpresaContextHolder.set("empresa-1");
        PraxisProperties properties = new PraxisProperties(
                "https://api.praxis.test",
                "https://app.praxis.test",
                168,
                12,
                720,
                70,
                15,
                0.001
        );
        service = new CandidateLinkQueryService(
                candidateAttemptRepository,
                simulationCatalogService,
                jwtService,
                properties
        );
    }

    @AfterEach
    void tearDown() {
        EmpresaContextHolder.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchReturnsPageWithoutExposingIdentityInBlindMode() {
        CandidateAttemptEntity entity = attempt();
        when(candidateAttemptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 100), 201));
        when(simulationCatalogService.findByVersionId(10L)).thenReturn(Optional.of(simulation()));
        when(jwtService.generateCandidateAttemptToken(
                "empresa-1",
                "att_1234567890123456",
                168,
                ATTEMPT_CREATED_AT
        )).thenReturn("candidate-token");

        CandidateLinkPageResponse response = service.search(
                -1,
                500,
                true,
                "completed",
                "sim-1",
                3,
                "maria"
        );

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(100);
        assertThat(response.totalElements()).isEqualTo(201);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().candidateName()).startsWith("Candidato ");
        assertThat(response.items().getFirst().candidateEmail()).isNull();
        assertThat(response.items().getFirst().candidateUrl())
                .isEqualTo("https://app.praxis.test/candidato/candidate-token");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(candidateAttemptRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        verify(jwtService).generateCandidateAttemptToken(
                eq("empresa-1"),
                eq("att_1234567890123456"),
                eq(168),
                eq(ATTEMPT_CREATED_AT)
        );
    }

    @Test
    void searchRejectsUnknownStatus() {
        assertThatThrownBy(() -> service.search(0, 25, false, "unknown", null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    private CandidateAttemptEntity attempt() {
        CandidateAttemptEntity entity = new CandidateAttemptEntity();
        entity.setId("att_1234567890123456");
        entity.setEmpresaId("empresa-1");
        entity.setSimulationId("sim-1");
        entity.setSimulationVersionId(10L);
        entity.setSimulationVersionNumber(3);
        entity.setCandidateName("Maria Silva");
        entity.setCandidateEmail("maria@example.com");
        entity.setStatus(AttemptStatus.COMPLETED);
        entity.setCreatedAt(ATTEMPT_CREATED_AT);
        return entity;
    }

    private PublishedSimulation simulation() {
        return new PublishedSimulation(
                10L,
                3,
                "sim-1",
                "Atendimento N2",
                "Descrição",
                List.of("Comunicação"),
                Map.of("Comunicação", 1.0),
                Map.of(),
                "node-1",
                List.of()
        );
    }
}
