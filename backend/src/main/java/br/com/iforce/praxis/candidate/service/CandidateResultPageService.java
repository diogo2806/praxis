package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.candidate.dto.CandidateResultItemResponse;
import br.com.iforce.praxis.candidate.dto.CandidateResultPageResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.CandidateAttemptMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class CandidateResultPageService {

    private final JwtService jwtService;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final CandidateAttemptMapper candidateAttemptMapper;
    private final SimulationCatalogService simulationCatalogService;

    public CandidateResultPageService(
            JwtService jwtService,
            CandidateAttemptRepository candidateAttemptRepository,
            CandidateAttemptMapper candidateAttemptMapper,
            SimulationCatalogService simulationCatalogService
    ) {
        this.jwtService = jwtService;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.candidateAttemptMapper = candidateAttemptMapper;
        this.simulationCatalogService = simulationCatalogService;
    }

    @Transactional(readOnly = true)
    public CandidateResultPageResponse findByToken(String token) {
        JwtService.CandidateResultToken candidateToken = parseToken(token);
        CandidateAttemptEntity entity = candidateAttemptRepository
                .findByEmpresaIdAndId(candidateToken.empresaId(), candidateToken.attemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        CandidateAttempt attempt = candidateAttemptMapper.toDomain(entity);
        PublishedSimulation simulation = findSimulation(entity);
        boolean finished = isTerminal(attempt.status());
        boolean completed = attempt.status() == AttemptStatus.COMPLETED;

        return new CandidateResultPageResponse(
                simulation.name(),
                publicStatus(attempt.status()),
                finished,
                completed ? entity.getCallbackUrl() : null,
                attempt.finishedAt(),
                candidateResults(attempt),
                completed ? attempt.rawScore() : null,
                completed ? attempt.pathMaximumScore() : null,
                completed ? normalizedScore(attempt) : null,
                completed ? attempt.scoringAlgorithmVersion() : null
        );
    }

    private Integer normalizedScore(CandidateAttempt attempt) {
        return attempt.normalizedScore() == null ? attempt.score() : attempt.normalizedScore();
    }

    private List<CandidateResultItemResponse> candidateResults(CandidateAttempt attempt) {
        if (attempt.status() != AttemptStatus.COMPLETED) {
            return List.of();
        }
        return attempt.results().stream()
                .filter(result -> result.tier() == ResultTier.MAJOR)
                .sorted(Comparator.comparing(ResultItem::name))
                .map(result -> new CandidateResultItemResponse(
                        result.name(),
                        result.score(),
                        result.score() + "%"
                ))
                .toList();
    }

    private JwtService.CandidateResultToken parseToken(String token) {
        try {
            return jwtService.parseCandidateResultToken(token);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token público de candidato inválido.");
        }
    }

    private PublishedSimulation findSimulation(CandidateAttemptEntity entity) {
        if (entity.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(entity.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Não encontramos esta versão do teste."
                    ));
        }
        return simulationCatalogService.findPublishedById(entity.getEmpresaId(), entity.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Não encontramos o teste publicado."
                ));
    }

    private boolean isTerminal(AttemptStatus status) {
        return status == AttemptStatus.COMPLETED
                || status == AttemptStatus.ABANDONED
                || status == AttemptStatus.EXPIRED;
    }

    private String publicStatus(AttemptStatus status) {
        return switch (status) {
            case NOT_STARTED -> "nao_iniciada";
            case IN_PROGRESS -> "em_andamento";
            case COMPLETED -> "concluida";
            case ABANDONED -> "abandonada";
            case EXPIRED -> "expirada";
        };
    }
}
