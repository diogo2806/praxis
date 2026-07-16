package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.candidate.dto.CandidateLinkPageResponse;
import br.com.iforce.praxis.candidate.dto.CandidateLinkResponse;
import br.com.iforce.praxis.candidate.service.BlindMasking;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.security.EmpresaSecurity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CandidateLinkQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final JwtService jwtService;
    private final PraxisProperties praxisProperties;

    public CandidateLinkQueryService(
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationCatalogService simulationCatalogService,
            JwtService jwtService,
            PraxisProperties praxisProperties
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.jwtService = jwtService;
        this.praxisProperties = praxisProperties;
    }

    @Transactional(readOnly = true)
    public CandidateLinkPageResponse search(
            int page,
            int size,
            boolean blind,
            String status,
            String simulationId,
            Integer versionNumber,
            String candidate
    ) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        AttemptStatus parsedStatus = parseStatus(status);
        String normalizedSimulation = normalize(simulationId);
        String normalizedCandidateValue = normalize(candidate);
        String normalizedCandidate = normalizedCandidateValue == null
                ? null
                : normalizedCandidateValue.toLowerCase(Locale.ROOT);

        Specification<CandidateAttemptEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("empresaId"), empresaId));
            if (parsedStatus != null) {
                predicates.add(builder.equal(root.get("status"), parsedStatus));
            }
            if (normalizedSimulation != null) {
                predicates.add(builder.equal(root.get("simulationId"), normalizedSimulation));
            }
            if (versionNumber != null) {
                predicates.add(builder.equal(root.get("simulationVersionNumber"), versionNumber));
            }
            if (normalizedCandidate != null) {
                String pattern = "%" + normalizedCandidate + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.<String>get("candidateName")), pattern),
                        builder.like(builder.lower(root.<String>get("candidateEmail")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        Page<CandidateAttemptEntity> result = candidateAttemptRepository.findAll(
                specification,
                PageRequest.of(
                        normalizedPage,
                        normalizedSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );

        List<CandidateLinkResponse> items = result.getContent().stream()
                .map(entity -> toResponse(entity, blind))
                .toList();

        return new CandidateLinkPageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private CandidateLinkResponse toResponse(CandidateAttemptEntity entity, boolean blind) {
        PublishedSimulation simulation = findSimulation(entity);
        String candidateName = blind ? BlindMasking.maskedName(entity.getId()) : entity.getCandidateName();
        String candidateEmail = blind ? null : entity.getCandidateEmail();
        return new CandidateLinkResponse(
                entity.getId(),
                candidatePageUrl(entity),
                candidateName,
                candidateEmail,
                entity.getSimulationId(),
                simulation.name(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    private PublishedSimulation findSimulation(CandidateAttemptEntity entity) {
        if (entity.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(entity.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "A versão associada à tentativa não foi encontrada."
                    ));
        }
        return simulationCatalogService.findPublishedById(entity.getEmpresaId(), entity.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "A avaliação associada à tentativa não foi encontrada."
                ));
    }

    private String candidatePageUrl(CandidateAttemptEntity entity) {
        String token = jwtService.generateCandidateAttemptToken(
                entity.getEmpresaId(),
                entity.getId(),
                praxisProperties.attemptLinkTtlHours()
        );
        return praxisProperties.candidatePageBaseUrl() + "/candidato/" + token;
    }

    private AttemptStatus parseStatus(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return null;
        }
        try {
            return AttemptStatus.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de tentativa inválido.");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
